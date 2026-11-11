package consulo.internal.injecting.binding;

import com.squareup.javapoet.CodeBlock;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import consulo.internal.source.SourceTexts;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Follows the constants an action is created with down its constructors, to the parameters which are its text,
 * description and icon.
 * <p/>
 * A constructor with source is followed through its {@code super(...)} or {@code this(...)}, a parameter replaced by what
 * the caller passed to it. The constructor called is chosen by its signature - the count of the arguments and the
 * types of those known - and nothing is guessed: several matching constructors end the walk. A constructor without
 * source, or without an explicit call, is where the walk ends: its parameters named {@code text}, {@code description}
 * and {@code icon} take what reached them - the names are in the class files.
 * <p/>
 * The call may come after other statements (flexible constructor bodies): a local variable declared, or a parameter
 * assigned, before it is known by what it was given - the last assignment wins.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
final class ActionPresentationTracker {
    /**
     * What is known of an argument: a constant text, a constant icon, or only its type - null when nothing is known.
     */
    private record Value(Kind kind, CodeBlock code, TypeMirror type) {
    }

    private enum Kind {
        TEXT,
        ICON,
        UNKNOWN
    }

    record Presentation(CodeBlock text, CodeBlock description, CodeBlock icon) {
    }

    private static final int MAX_DEPTH = 16;

    private final ProcessingEnvironment myProcessingEnv;
    private final Trees myTrees;
    private final SourceTexts mySourceTexts;
    private final TypeMirror myLocalizeValueType;
    private final TypeMirror myImageType;

    ActionPresentationTracker(ProcessingEnvironment processingEnv, Trees trees, SourceTexts sourceTexts) {
        myProcessingEnv = processingEnv;
        myTrees = trees;
        mySourceTexts = sourceTexts;
        TypeElement localizeValue = processingEnv.getElementUtils().getTypeElement(SourceTexts.LOCALIZE_VALUE);
        TypeElement image = processingEnv.getElementUtils().getTypeElement(SourceTexts.IMAGE);
        myLocalizeValueType = localizeValue == null ? null : localizeValue.asType();
        myImageType = image == null ? null : image.asType();
    }

    /**
     * The presentation the action gets when created by the constructor - its parameters are injected, so unknown.
     * Null when no constant text reaches the text.
     */
    Presentation track(ExecutableElement constructor) {
        if (myLocalizeValueType == null || myImageType == null) {
            return null;
        }
        List<Value> arguments = new ArrayList<>();
        for (VariableElement parameter : constructor.getParameters()) {
            arguments.add(new Value(Kind.UNKNOWN, null, parameter.asType()));
        }
        Presentation presentation = walk(constructor, arguments, 0);
        return presentation == null || presentation.text() == null ? null : presentation;
    }

    private Presentation walk(ExecutableElement constructor, List<Value> arguments, int depth) {
        if (depth > MAX_DEPTH) {
            return null;
        }

        MethodTree tree = myTrees.getTree(constructor);
        if (tree == null || tree.getBody() == null) {
            // compiled - the walk ends at the parameters themselves
            return byParameterNames(constructor, arguments);
        }

        TreePath constructorPath = myTrees.getPath(constructor);
        TypeElement owner = (TypeElement) constructor.getEnclosingElement();

        // the names known before the call - the parameters, then what the statements before the call declare or assign
        Map<String, Value> names = new HashMap<>();
        List<? extends VariableElement> parameters = constructor.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            names.put(parameters.get(i).getSimpleName().toString(), arguments.get(i));
        }

        MethodInvocationTree call = null;
        for (StatementTree statement : tree.getBody().getStatements()) {
            TreePath statementPath = new TreePath(constructorPath, statement);
            if (statement instanceof ExpressionStatementTree expressionStatement) {
                ExpressionTree expression = expressionStatement.getExpression();
                if (expression instanceof MethodInvocationTree invocation
                    && invocation.getMethodSelect() instanceof IdentifierTree name
                    && (name.getName().contentEquals("super") || name.getName().contentEquals("this"))) {
                    call = invocation;
                    break;
                }
                if (expression instanceof AssignmentTree assignment && assignment.getVariable() instanceof IdentifierTree variable) {
                    TreePath valuePath = new TreePath(new TreePath(statementPath, assignment), assignment.getExpression());
                    names.put(variable.getName().toString(), valueOf(valuePath, assignment.getExpression(), names, owner));
                }
            }
            else if (statement instanceof VariableTree variable && variable.getInitializer() != null) {
                TreePath valuePath = new TreePath(statementPath, variable.getInitializer());
                names.put(variable.getName().toString(), valueOf(valuePath, variable.getInitializer(), names, owner));
            }
        }
        if (call == null) {
            // nothing called - the walk ends at the parameters themselves
            return byParameterNames(constructor, arguments);
        }

        List<Value> callArguments = new ArrayList<>();
        for (ExpressionTree argument : call.getArguments()) {
            callArguments.add(valueOf(new TreePath(new TreePath(constructorPath, call), argument), argument, names, owner));
        }

        boolean superCall = ((IdentifierTree) call.getMethodSelect()).getName().contentEquals("super");
        TypeElement target = superCall ? superclassOf(owner) : owner;
        if (target == null) {
            return null;
        }
        // a constructor can't call itself - for this(...) the caller is no candidate
        ExecutableElement targetConstructor = chooseConstructor(target, callArguments, superCall ? null : constructor);
        return targetConstructor == null ? null : walk(targetConstructor, callArguments, depth + 1);
    }

    /**
     * An expression of a constructor: a constant text or icon, a name known before the call - a parameter replaced by
     * what reached it, or a local variable - or else only its type, when the syntax tells it.
     */
    private Value valueOf(TreePath path, ExpressionTree argument, Map<String, Value> names, TypeElement scope) {
        if (argument instanceof IdentifierTree identifier) {
            Value known = names.get(identifier.getName().toString());
            if (known != null) {
                return known;
            }
        }
        if (argument instanceof MethodInvocationTree invocation) {
            CodeBlock text = mySourceTexts.toText(path, invocation, scope);
            if (text != null) {
                return new Value(Kind.TEXT, text, myLocalizeValueType);
            }
            CodeBlock icon = mySourceTexts.toIcon(path, argument);
            if (icon != null) {
                return new Value(Kind.ICON, icon, myImageType);
            }
        }
        return new Value(Kind.UNKNOWN, null, typeOf(path, argument));
    }

    private TypeMirror typeOf(TreePath path, ExpressionTree argument) {
        Types types = myProcessingEnv.getTypeUtils();
        if (argument instanceof LiteralTree literal) {
            Object value = literal.getValue();
            if (value instanceof Boolean) {
                return types.getPrimitiveType(TypeKind.BOOLEAN);
            }
            if (value instanceof Integer) {
                return types.getPrimitiveType(TypeKind.INT);
            }
            if (value instanceof String) {
                return myProcessingEnv.getElementUtils().getTypeElement("java.lang.String").asType();
            }
            // null, or a literal of no interest - any reference type
            return null;
        }
        if (argument instanceof NewClassTree newClass && newClass.getClassBody() == null) {
            TypeElement type = mySourceTexts.resolveType(path, newClass.getIdentifier());
            return type == null ? null : type.asType();
        }
        return null;
    }

    /**
     * The one constructor of the type the arguments fit - null when none, or several.
     */
    private ExecutableElement chooseConstructor(TypeElement type, List<Value> arguments, ExecutableElement excluded) {
        Types types = myProcessingEnv.getTypeUtils();
        ExecutableElement chosen = null;
        for (ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
            List<? extends VariableElement> parameters = constructor.getParameters();
            if (constructor.equals(excluded) || parameters.size() != arguments.size() || constructor.isVarArgs()) {
                continue;
            }
            boolean fits = true;
            for (int i = 0; i < parameters.size() && fits; i++) {
                TypeMirror known = arguments.get(i).type();
                if (known != null) {
                    fits = types.isAssignable(types.erasure(known), types.erasure(parameters.get(i).asType()));
                }
            }
            if (fits) {
                if (chosen != null) {
                    // ambiguous - nothing is guessed
                    return null;
                }
                chosen = constructor;
            }
        }
        return chosen;
    }

    private Presentation byParameterNames(ExecutableElement constructor, List<Value> arguments) {
        Types types = myProcessingEnv.getTypeUtils();
        CodeBlock text = null;
        CodeBlock description = null;
        CodeBlock icon = null;
        List<? extends VariableElement> parameters = constructor.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            Value value = arguments.get(i);
            String name = parameter.getSimpleName().toString();
            TypeMirror type = types.erasure(parameter.asType());
            if (value.kind() == Kind.TEXT && types.isSameType(type, types.erasure(myLocalizeValueType))) {
                if (name.equals("text")) {
                    text = value.code();
                }
                else if (name.equals("description")) {
                    description = value.code();
                }
            }
            else if (value.kind() == Kind.ICON && name.equals("icon") && types.isAssignable(types.erasure(myImageType), type)) {
                icon = value.code();
            }
        }
        return new Presentation(text, description, icon);
    }

    private static TypeElement superclassOf(TypeElement type) {
        TypeMirror superclass = type.getSuperclass();
        if (superclass.getKind() != TypeKind.DECLARED) {
            return null;
        }
        Element element = ((DeclaredType) superclass).asElement();
        return element instanceof TypeElement typeElement ? typeElement : null;
    }
}
