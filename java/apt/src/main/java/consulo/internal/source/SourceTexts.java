package consulo.internal.source;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the localize texts and the icons of the source - from the syntax alone, as asking javac for the element of a
 * call attributes the method early, which breaks the attribution of its lambdas later.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
public final class SourceTexts {
    public static final String LOCALIZE_VALUE = "consulo.localize.LocalizeValue";
    public static final String IMAGE = "consulo.ui.image.Image";

    private final ProcessingEnvironment processingEnv;

    public SourceTexts(ProcessingEnvironment processingEnvironment) {
        processingEnv = processingEnvironment;
    }

    /**
     * The code of an icon - {@code SomeIconGroup.name()} of a generated icon group - null for anything else.
     */
    public CodeBlock toIcon(TreePath path, ExpressionTree expression) {
        if (!(expression instanceof MethodInvocationTree node) || !node.getArguments().isEmpty()
            || !(node.getMethodSelect() instanceof MemberSelectTree select)) {
            return null;
        }
        TypeElement owner = resolveType(path, select.getExpression());
        if (owner == null || !owner.getSimpleName().toString().endsWith("IconGroup")) {
            return null;
        }
        String name = select.getIdentifier().toString();
        TypeElement image = processingEnv.getElementUtils().getTypeElement(IMAGE);
        for (ExecutableElement method : ElementFilter.methodsIn(owner.getEnclosedElements())) {
            // an icon group returns ImageKey, which is an Image
            if (method.getSimpleName().contentEquals(name) && method.getParameters().isEmpty()
                && method.getModifiers().contains(Modifier.STATIC)
                && image != null
                && processingEnv.getTypeUtils().isAssignable(method.getReturnType(), image.asType())) {
                return CodeBlock.of("$T.$L()", ClassName.get(owner), name);
            }
        }
        return null;
    }

    /**
     * The code of a localize text - {@code SomeLocalize.key(constants...)}, or
     * {@code LocalizeValue.localizeTODO("literal")} / {@code LocalizeValue.of("literal")} - null for anything else.
     * <p/>
     * Read from the syntax alone: asking javac for the element of a call attributes the method early, which
     * breaks the attribution of its lambdas later.
     */
    public CodeBlock toText(TreePath path, MethodInvocationTree node, TypeElement scope) {
        if (!(node.getMethodSelect() instanceof MemberSelectTree select)) {
            return null;
        }
        TypeElement owner = resolveType(path, select.getExpression());
        if (owner == null) {
            return null;
        }
        String methodName = select.getIdentifier().toString();
        ExecutableElement method = findStaticLocalizeMethod(owner, methodName, node.getArguments().size());
        if (method == null) {
            return null;
        }

        String ownerName = owner.getQualifiedName().toString();
        if (ownerName.equals(LOCALIZE_VALUE)) {
            if (!(methodName.equals("localizeTODO") || methodName.equals("of")) || node.getArguments().size() != 1
                || !(node.getArguments().getFirst() instanceof LiteralTree literal) || !(literal.getValue() instanceof String value)
                || value.isBlank()) {
                return null;
            }
            return CodeBlock.of("$T.$L($S)", ClassName.bestGuess(LOCALIZE_VALUE), methodName, value);
        }

        // a generated localize class - <package>.localize.<Name>Localize
        if (!owner.getSimpleName().toString().endsWith("Localize")
            || !(owner.getEnclosingElement() instanceof PackageElement packageElement)
            || !packageElement.getQualifiedName().toString().endsWith(".localize")) {
            return null;
        }

        List<CodeBlock> arguments = new ArrayList<>();
        for (ExpressionTree argument : node.getArguments()) {
            Object constant = constantValue(path, argument, scope);
            // a text built from values known only at runtime can't be listed
            if (constant == null) {
                return null;
            }
            arguments.add(constant instanceof String ? CodeBlock.of("$S", constant) : CodeBlock.of("$L", literal(constant)));
        }
        return CodeBlock.of("$T.$L($L)", ClassName.get(owner), methodName, CodeBlock.join(arguments, ", "));
    }

    private static ExecutableElement findStaticLocalizeMethod(TypeElement owner, String name, int parameterCount) {
        for (ExecutableElement method : ElementFilter.methodsIn(owner.getEnclosedElements())) {
            if (method.getSimpleName().contentEquals(name)
                && method.getModifiers().contains(Modifier.STATIC)
                && method.getParameters().size() == parameterCount
                && method.getReturnType().getKind() == TypeKind.DECLARED
                && ((TypeElement) ((DeclaredType) method.getReturnType()).asElement()).getQualifiedName().contentEquals(LOCALIZE_VALUE)) {
                return method;
            }
        }
        return null;
    }

    /**
     * A literal, a constant field of {@code scope} by its name, or a constant field of a class -
     * {@code Owner.NAME}.
     */
    public Object constantValue(TreePath path, ExpressionTree expression, TypeElement scope) {
        if (expression instanceof LiteralTree literal) {
            return literal.getValue();
        }
        if (expression instanceof IdentifierTree identifier) {
            return constantField(scope, identifier.getName().toString());
        }
        if (expression instanceof MemberSelectTree select) {
            TypeElement owner = resolveType(path, select.getExpression());
            return owner == null ? null : constantField(owner, select.getIdentifier().toString());
        }
        return null;
    }

    private static Object constantField(TypeElement type, String name) {
        TypeElement current = type;
        while (current != null) {
            for (VariableElement field : ElementFilter.fieldsIn(current.getEnclosedElements())) {
                if (field.getSimpleName().contentEquals(name)) {
                    return field.getConstantValue();
                }
            }
            current = current.getEnclosingElement() instanceof TypeElement outer ? outer : null;
        }
        return null;
    }

    /**
     * The class a name in the source stands for - qualified, imported, or in the same package.
     */
    public TypeElement resolveType(TreePath path, ExpressionTree expression) {
        String name = expression.toString();
        if (!(expression instanceof IdentifierTree) && !(expression instanceof MemberSelectTree)) {
            return null;
        }
        if (name.contains(".")) {
            return processingEnv.getElementUtils().getTypeElement(name);
        }

        CompilationUnitTree unit = path.getCompilationUnit();
        for (ImportTree importTree : unit.getImports()) {
            if (importTree.isStatic()) {
                continue;
            }
            String imported = importTree.getQualifiedIdentifier().toString();
            if (imported.endsWith("." + name)) {
                return processingEnv.getElementUtils().getTypeElement(imported);
            }
        }
        String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
        TypeElement samePackage = processingEnv.getElementUtils().getTypeElement(packageName.isEmpty() ? name : packageName + "." + name);
        if (samePackage != null) {
            return samePackage;
        }
        for (ImportTree importTree : unit.getImports()) {
            String imported = importTree.getQualifiedIdentifier().toString();
            if (!importTree.isStatic() && imported.endsWith(".*")) {
                TypeElement onDemand = processingEnv.getElementUtils().getTypeElement(imported.substring(0, imported.length() - 1) + name);
                if (onDemand != null) {
                    return onDemand;
                }
            }
        }
        return null;
    }

    private static String literal(Object constant) {
        if (constant instanceof Long) {
            return constant + "L";
        }
        if (constant instanceof Float) {
            return constant + "F";
        }
        if (constant instanceof Character c) {
            return "'" + (c == '\'' || c == '\\' ? "\\" + c : String.valueOf(c)) + "'";
        }
        return String.valueOf(constant);
    }
}
