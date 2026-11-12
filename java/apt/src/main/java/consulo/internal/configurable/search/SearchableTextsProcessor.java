package consulo.internal.configurable.search;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import consulo.compiler.apt.shared.ConsuloClasses;
import consulo.internal.source.SourceTexts;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a {@code SearchableConfigurableTexts} for every configurable extension - the id of the page and the
 * localize texts its code uses, so settings search finds the page without creating it.
 * <p/>
 * The generated class is an extension itself, so the binding processor binds it in the next round. This processor
 * must be registered before the binding processor, which claims {@code @ExtensionImpl}.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@SupportedAnnotationTypes(ConsuloClasses.consulo.annotation.component.ExtensionImpl)
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SearchableTextsProcessor extends AbstractProcessor {
    private static final String CONFIGURABLE = "consulo.configurable.Configurable";
    private static final String SEARCHABLE_TEXTS = "consulo.configurable.search.SearchableConfigurableTexts";
    private static final String SEARCHABLE_TEXTS_ANNOTATION = "consulo.configurable.search.SearchableTexts";
    private static final String NOT_SEARCHABLE = "consulo.configurable.search.NotSearchable";
    private static final String LOCALIZE_VALUE = "consulo.localize.LocalizeValue";
    private static final String SUFFIX = "_SearchableTexts";
    private static final String SEARCHABLE_BOOLEAN_OPTIONS = "consulo.configurable.search.SearchableBooleanOptions";
    private static final String SEARCHABLE_BOOLEAN_OPTION = "consulo.configurable.search.SearchableBooleanOption";
    private static final String PROJECT_SEARCHABLE_BOOLEAN_OPTIONS = "consulo.configurable.search.ProjectSearchableBooleanOptions";
    private static final String AFTER_OPTION_CHANGE = "consulo.configurable.search.AfterOptionChange";
    private static final String SERVICE_API = "consulo.annotation.component.ServiceAPI";
    private static final String CHECK_BOX = "consulo.ui.CheckBox";
    private static final String BOOLEAN_OPTIONS_SUFFIX = "_BooleanOptions";

    /**
     * A check box bound to a boolean of an application or project service - {@code propertyBuilder.add(box, s::getX, s::setX)}.
     */
    private record BooleanOption(CodeBlock text, TypeElement settings, String getter, String setter) {
    }

    private Trees myTrees;
    private SourceTexts mySourceTexts;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mySourceTexts = new SourceTexts(processingEnv);
        try {
            myTrees = Trees.instance(processingEnv);
        }
        catch (IllegalArgumentException e) {
            // not javac - the method bodies can't be read, nothing is generated
            myTrees = null;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty() || myTrees == null) {
            return false;
        }

        TypeElement configurable = processingEnv.getElementUtils().getTypeElement(CONFIGURABLE);
        TypeElement searchableTexts = processingEnv.getElementUtils().getTypeElement(SEARCHABLE_TEXTS);
        // a platform without the api - nothing to generate against
        if (configurable == null || searchableTexts == null) {
            return false;
        }
        TypeMirror configurableType = processingEnv.getTypeUtils().erasure(configurable.asType());

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (!(element instanceof TypeElement type)
                    || type.getKind() != ElementKind.CLASS
                    || type.getModifiers().contains(Modifier.ABSTRACT)
                    || !processingEnv.getTypeUtils().isAssignable(type.asType(), configurableType)
                    || findAnnotation(type, NOT_SEARCHABLE) != null) {
                    continue;
                }

                try {
                    generate(type, configurableType);
                }
                catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can't generate searchable texts: " + e, type);
                }
            }
        }
        // the annotation is the binding processor's too
        return false;
    }

    private void generate(TypeElement type, TypeMirror configurableType) throws IOException {
        AnnotationMirror searchableAnnotation = findAnnotation(type, SEARCHABLE_TEXTS_ANNOTATION);

        String id = annotationString(searchableAnnotation, "id");
        if (id == null || id.isEmpty()) {
            id = findId(type);
        }
        if (id == null || id.isEmpty()) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "No constant id - the page is found by its name only. Return a constant from getId(), or use @SearchableTexts(id = ...)",
                type
            );
            return;
        }

        // the code of a text, keyed by itself - the same text used twice is listed once
        Map<String, CodeBlock> texts = new LinkedHashMap<>();
        // the name of the page first
        ExecutableElement displayName = findMethod(type, "getDisplayName");
        if (displayName != null) {
            collectTexts(myTrees.getPath(displayName), texts, configurableType, type);
        }
        TreePath classPath = myTrees.getPath(type);
        if (classPath != null) {
            collectTexts(classPath, texts, configurableType, type);
        }
        for (TypeElement extra : annotationClasses(searchableAnnotation, "value")) {
            TreePath extraPath = myTrees.getPath(extra);
            if (extraPath != null) {
                collectTexts(extraPath, texts, configurableType, extra);
            }
            else {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "The source of " + extra.getQualifiedName() + " is not compiled here - its texts are not searchable",
                    type
                );
            }
        }
        generateBooleanOptions(type, id);

        if (texts.isEmpty()) {
            return;
        }

        ClassName localizeValue = ClassName.bestGuess(LOCALIZE_VALUE);
        // an array, as a call takes 255 arguments at most
        CodeBlock.Builder list = CodeBlock.builder().add("return $T.of(new $T[] {", List.class, localizeValue);
        boolean first = true;
        for (CodeBlock text : texts.values()) {
            list.add(first ? "\n$>$>$L" : ",\n$L", text);
            first = false;
        }
        list.add("$<$<\n});\n");

        String generatedName = flatName(type) + SUFFIX;
        TypeSpec generated = TypeSpec.classBuilder(generatedName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "ALL").build())
            .addAnnotation(ClassName.bestGuess(ConsuloClasses.consulo.annotation.component.ExtensionImpl))
            .addSuperinterface(ClassName.bestGuess(SEARCHABLE_TEXTS))
            .addMethod(MethodSpec.methodBuilder("getConfigurableId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", id)
                .build())
            .addMethod(MethodSpec.methodBuilder("getTexts")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), localizeValue))
                .addCode(list.build())
                .build())
            .build();

        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(type);
        String packageName = packageElement.getQualifiedName().toString();
        JavaFileObject file = processingEnv.getFiler().createSourceFile(
            packageName.isEmpty() ? generatedName : packageName + "." + generatedName,
            type
        );
        try (Writer writer = file.openWriter()) {
            JavaFile.builder(packageName, generated).build().writeTo(writer);
        }
    }

    /**
     * The constant {@code getId()} returns - a literal, or a constant field.
     */
    private String findId(TypeElement type) {
        ExecutableElement getId = findMethod(type, "getId");
        if (getId == null) {
            return null;
        }
        MethodTree method = myTrees.getTree(getId);
        if (method == null || method.getBody() == null || method.getBody().getStatements().size() != 1
            || !(method.getBody().getStatements().getFirst() instanceof ReturnTree returnTree)
            || returnTree.getExpression() == null) {
            return null;
        }
        Object constant = mySourceTexts.constantValue(myTrees.getPath(getId), returnTree.getExpression(), (TypeElement) getId.getEnclosingElement());
        return constant instanceof String string ? string : null;
    }

    /**
     * {@code getId()} or {@code getDisplayName()} of the class or the nearest super class compiled here.
     */
    private ExecutableElement findMethod(TypeElement type, String name) {
        TypeElement current = type;
        while (current != null) {
            for (ExecutableElement method : ElementFilter.methodsIn(current.getEnclosedElements())) {
                if (method.getSimpleName().contentEquals(name) && method.getParameters().isEmpty()
                    && !method.getModifiers().contains(Modifier.ABSTRACT) && myTrees.getTree(method) != null) {
                    return method;
                }
            }
            TypeMirror superclass = current.getSuperclass();
            current = superclass.getKind() == TypeKind.DECLARED ? (TypeElement) ((DeclaredType) superclass).asElement() : null;
        }
        return null;
    }

    private void collectTexts(TreePath root, Map<String, CodeBlock> texts, TypeMirror configurableType, TypeElement owner) {
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                // a nested configurable has texts of its own - found among the members by name, as asking javac
                // for the element of a class in a method body would attribute it early
                if (node != root.getLeaf() && !node.getSimpleName().isEmpty()) {
                    for (TypeElement nested : ElementFilter.typesIn(owner.getEnclosedElements())) {
                        if (nested.getSimpleName().contentEquals(node.getSimpleName())
                            && processingEnv.getTypeUtils().isAssignable(nested.asType(), configurableType)
                            && findAnnotation(nested, ConsuloClasses.consulo.annotation.component.ExtensionImpl) != null) {
                            return null;
                        }
                    }
                }
                return super.visitClass(node, unused);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                CodeBlock text = mySourceTexts.toText(getCurrentPath(), node, owner);
                if (text != null) {
                    texts.putIfAbsent(text.toString(), text);
                }
                return super.visitMethodInvocation(node, unused);
            }
        }.scan(root, null);
    }







    /**
     * {@code Outer_Inner} for a nested class - the generated class sits next to the top level one.
     */
    private static String flatName(TypeElement type) {
        StringBuilder name = new StringBuilder(type.getSimpleName());
        Element enclosing = type.getEnclosingElement();
        while (enclosing instanceof TypeElement outer) {
            name.insert(0, outer.getSimpleName() + "_");
            enclosing = outer.getEnclosingElement();
        }
        return name.toString();
    }

    private static AnnotationMirror findAnnotation(Element element, String name) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(name)) {
                return mirror;
            }
        }
        return null;
    }

    private String annotationString(AnnotationMirror annotation, String name) {
        AnnotationValue value = annotationValue(annotation, name);
        return value != null && value.getValue() instanceof String string ? string : null;
    }

    private List<TypeElement> annotationClasses(AnnotationMirror annotation, String name) {
        AnnotationValue value = annotationValue(annotation, name);
        List<TypeElement> result = new ArrayList<>();
        if (value != null && value.getValue() instanceof List<?> values) {
            for (Object item : values) {
                if (item instanceof AnnotationValue classValue && classValue.getValue() instanceof DeclaredType declared) {
                    result.add((TypeElement) declared.asElement());
                }
            }
        }
        return result;
    }

    private static AnnotationValue annotationValue(AnnotationMirror annotation, String name) {
        if (annotation == null) {
            return null;
        }
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Generates the {@code SearchableBooleanOptions} of the check boxes the configurable binds to a service with
     * method references - the {@code ProjectSearchableBooleanOptions} when one of the services is a project one.
     */
    private void generateBooleanOptions(TypeElement type, String id) throws IOException {
        if (processingEnv.getElementUtils().getTypeElement(SEARCHABLE_BOOLEAN_OPTIONS) == null) {
            return;
        }
        ClassTree classTree = myTrees.getTree(type);
        TreePath classPath = myTrees.getPath(type);
        if (classTree == null || classPath == null) {
            return;
        }

        List<BooleanOption> options = new ArrayList<>();
        for (Tree member : classTree.getMembers()) {
            if (member instanceof MethodTree method && method.getBody() != null) {
                collectBooleanOptions(new TreePath(classPath, method), type, options);
            }
        }
        if (options.isEmpty()) {
            return;
        }

        ExecutableElement hook = findAfterOptionChange(type);
        if (hook == null && declaresApply(type)) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "apply() or afterApply() does more than storing the values - its options can be switched from Find Action once a static method"
                    + " marked @AfterOptionChange does the rest",
                type
            );
            return;
        }

        // one provider of each settings type the options use
        Map<String, TypeElement> settingsTypes = new LinkedHashMap<>();
        boolean projectLevel = false;
        for (BooleanOption option : options) {
            settingsTypes.putIfAbsent(option.settings().getQualifiedName().toString(), option.settings());
            projectLevel |= isProjectService(findServiceApi(option.settings()));
        }
        if (projectLevel && processingEnv.getElementUtils().getTypeElement(PROJECT_SEARCHABLE_BOOLEAN_OPTIONS) == null) {
            // a platform without project options
            return;
        }
        List<String> settingsNames = new ArrayList<>(settingsTypes.keySet());

        ClassName provider = ClassName.get("jakarta.inject", "Provider");
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("jakarta.inject", "Inject"));
        TypeSpec.Builder generated = TypeSpec.classBuilder(flatName(type) + BOOLEAN_OPTIONS_SUFFIX)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "ALL").build())
            .addAnnotation(ClassName.bestGuess(ConsuloClasses.consulo.annotation.component.ExtensionImpl))
            .addSuperinterface(ClassName.bestGuess(projectLevel ? PROJECT_SEARCHABLE_BOOLEAN_OPTIONS : SEARCHABLE_BOOLEAN_OPTIONS));
        CodeBlock.Builder getOptions = CodeBlock.builder();
        for (int i = 0; i < settingsNames.size(); i++) {
            TypeElement settingsType = settingsTypes.get(settingsNames.get(i));
            ClassName settingsClass = ClassName.get(settingsType);
            // the service is bound to its api - an implementation is cast from it
            TypeElement apiType = findServiceApi(settingsType);
            ClassName apiClass = ClassName.get(apiType);
            generated.addField(ParameterizedTypeName.get(provider, apiClass), "mySettings" + i, Modifier.PRIVATE, Modifier.FINAL);
            constructor.addParameter(ParameterizedTypeName.get(provider, apiClass), "settings" + i);
            constructor.addStatement("mySettings$L = settings$L", i, i);
            if (apiType.equals(settingsType)) {
                getOptions.addStatement("$T settings$L = mySettings$L.get()", settingsClass, i, i);
            }
            else {
                getOptions.addStatement("$T settings$L = ($T) mySettings$L.get()", settingsClass, i, settingsClass, i);
            }
        }

        ClassName option = ClassName.bestGuess(SEARCHABLE_BOOLEAN_OPTION);
        getOptions.add("return $T.of(new $T[] {", List.class, option);
        boolean first = true;
        for (BooleanOption booleanOption : options) {
            String settings = "settings" + settingsNames.indexOf(booleanOption.settings().getQualifiedName().toString());
            CodeBlock setter = hookCall(type, hook, booleanOption.settings(), settings) == null
                ? CodeBlock.of("value -> $L.$L(value)", settings, booleanOption.setter())
                : CodeBlock.of("value -> {\n$>$L.$L(value);\n$L;\n$<}", settings, booleanOption.setter(), hookCall(type, hook, booleanOption.settings(), settings));
            getOptions.add(first ? "\n$>$>" : ",\n");
            getOptions.add("new $T($L, $L::$L, $L)", option, booleanOption.text(), settings, booleanOption.getter(), setter);
            first = false;
        }
        getOptions.add("$<$<\n});\n");

        ExecutableElement displayName = findMethod(type, "getDisplayName");
        CodeBlock name = displayName == null ? null : returnedText(displayName);
        ClassName localizeValue = ClassName.bestGuess(LOCALIZE_VALUE);

        generated.addMethod(constructor.build())
            .addMethod(MethodSpec.methodBuilder("getConfigurableId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", id)
                .build())
            .addMethod(MethodSpec.methodBuilder("getConfigurableName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(localizeValue)
                .addStatement("return $L", name != null ? name : CodeBlock.of("$T.empty()", localizeValue))
                .build())
            .addMethod(MethodSpec.methodBuilder("getOptions")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), option))
                .addCode(getOptions.build())
                .build());

        String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        String generatedName = flatName(type) + BOOLEAN_OPTIONS_SUFFIX;
        JavaFileObject file = processingEnv.getFiler().createSourceFile(
            packageName.isEmpty() ? generatedName : packageName + "." + generatedName,
            type
        );
        try (Writer writer = file.openWriter()) {
            JavaFile.builder(packageName, generated.build()).build().writeTo(writer);
        }
    }

    /**
     * The check boxes of one method: {@code CheckBox box = CheckBox.create(text)}, {@code Service s = ...} and
     * {@code propertyBuilder.add(box, s::getX, s::setX)}.
     */
    private void collectBooleanOptions(TreePath methodPath, TypeElement scope, List<BooleanOption> options) {
        Map<String, CodeBlock> checkBoxes = new LinkedHashMap<>();
        Map<String, TypeElement> services = new LinkedHashMap<>();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                // the check boxes of a nested class are not this method's
                return null;
            }

            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                String name = node.getName().toString();
                if (node.getInitializer() instanceof MethodInvocationTree creation
                    && creation.getMethodSelect() instanceof MemberSelectTree select
                    && select.getIdentifier().contentEquals("create")
                    && creation.getArguments().size() == 1
                    && isType(mySourceTexts.resolveType(getCurrentPath(), select.getExpression()), CHECK_BOX)
                    && creation.getArguments().getFirst() instanceof MethodInvocationTree textCall) {
                    CodeBlock text = mySourceTexts.toText(new TreePath(new TreePath(getCurrentPath(), creation), textCall), textCall, scope);
                    if (text != null) {
                        checkBoxes.put(name, text);
                    }
                }
                else if (node.getType() instanceof IdentifierTree || node.getType() instanceof MemberSelectTree) {
                    TypeElement type = mySourceTexts.resolveType(getCurrentPath(), (ExpressionTree) node.getType());
                    if (type != null && findServiceApi(type) != null) {
                        services.put(name, type);
                    }
                }
                return super.visitVariable(node, unused);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (node.getMethodSelect() instanceof MemberSelectTree select
                    && select.getIdentifier().contentEquals("add")
                    && node.getArguments().size() == 3
                    && node.getArguments().get(0) instanceof IdentifierTree box
                    && checkBoxes.containsKey(box.getName().toString())
                    && node.getArguments().get(1) instanceof MemberReferenceTree getter
                    && node.getArguments().get(2) instanceof MemberReferenceTree setter
                    && getter.getMode() == MemberReferenceTree.ReferenceMode.INVOKE
                    && setter.getMode() == MemberReferenceTree.ReferenceMode.INVOKE
                    && getter.getQualifierExpression() instanceof IdentifierTree getterOwner
                    && setter.getQualifierExpression() instanceof IdentifierTree setterOwner
                    && getterOwner.getName().contentEquals(setterOwner.getName())
                    && services.containsKey(getterOwner.getName().toString())) {
                    TypeElement settings = services.get(getterOwner.getName().toString());
                    String getterName = getter.getName().toString();
                    String setterName = setter.getName().toString();
                    if (hasBooleanGetter(settings, getterName) && hasBooleanSetter(settings, setterName)) {
                        options.add(new BooleanOption(checkBoxes.get(box.getName().toString()), settings, getterName, setterName));
                    }
                }
                return super.visitMethodInvocation(node, unused);
            }
        }.scan(methodPath, null);
    }

    private static boolean isType(TypeElement type, String name) {
        return type != null && type.getQualifiedName().contentEquals(name);
    }

    /**
     * The type annotated {@code @ServiceAPI} of the application or the project - the type itself or one of its super
     * types - which is what the service is bound to. Null when there is none.
     */
    private TypeElement findServiceApi(TypeElement type) {
        List<TypeElement> queue = new ArrayList<>(List.of(type));
        while (!queue.isEmpty()) {
            TypeElement current = queue.removeFirst();
            AnnotationMirror serviceApi = findAnnotation(current, SERVICE_API);
            if (serviceApi != null) {
                AnnotationValue value = annotationValue(serviceApi, "value");
                boolean supported = value != null && value.getValue() instanceof VariableElement scope
                    && (scope.getSimpleName().contentEquals("APPLICATION") || scope.getSimpleName().contentEquals("PROJECT"));
                return supported ? current : null;
            }
            if (current.getSuperclass().getKind() == TypeKind.DECLARED) {
                queue.add((TypeElement) ((DeclaredType) current.getSuperclass()).asElement());
            }
            for (TypeMirror superInterface : current.getInterfaces()) {
                if (superInterface.getKind() == TypeKind.DECLARED) {
                    queue.add((TypeElement) ((DeclaredType) superInterface).asElement());
                }
            }
        }
        return null;
    }

    private boolean isProjectService(TypeElement serviceApi) {
        AnnotationValue value = annotationValue(findAnnotation(serviceApi, SERVICE_API), "value");
        return value != null && value.getValue() instanceof VariableElement scope && scope.getSimpleName().contentEquals("PROJECT");
    }

    private boolean hasBooleanGetter(TypeElement type, String name) {
        for (ExecutableElement method : ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(type))) {
            if (method.getSimpleName().contentEquals(name) && method.getParameters().isEmpty()
                && method.getModifiers().contains(Modifier.PUBLIC) && !method.getModifiers().contains(Modifier.STATIC)
                && isBoolean(method.getReturnType())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBooleanSetter(TypeElement type, String name) {
        for (ExecutableElement method : ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(type))) {
            if (method.getSimpleName().contentEquals(name) && method.getParameters().size() == 1
                && method.getModifiers().contains(Modifier.PUBLIC) && !method.getModifiers().contains(Modifier.STATIC)
                && isBoolean(method.getParameters().getFirst().asType())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBoolean(TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN
            || type.getKind() == TypeKind.DECLARED && ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals("java.lang.Boolean");
    }

    private static ExecutableElement findAfterOptionChange(TypeElement type) {
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (findAnnotation(method, AFTER_OPTION_CHANGE) != null
                && method.getModifiers().contains(Modifier.STATIC)
                && !method.getModifiers().contains(Modifier.PRIVATE)
                && method.getParameters().size() <= 1) {
                return method;
            }
        }
        return null;
    }

    private static boolean declaresApply(TypeElement type) {
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            // afterApply() runs after the values are stored, so it is something besides storing them too
            if ((method.getSimpleName().contentEquals("apply") || method.getSimpleName().contentEquals("afterApply"))
                && !method.getModifiers().contains(Modifier.STATIC)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The call of the {@code @AfterOptionChange} method for an option stored in {@code settings} - null when there is
     * none, or it takes another settings object.
     */
    private CodeBlock hookCall(TypeElement type, ExecutableElement hook, TypeElement settingsType, String settings) {
        if (hook == null) {
            return null;
        }
        if (hook.getParameters().isEmpty()) {
            return CodeBlock.of("$T.$L()", ClassName.get(type), hook.getSimpleName());
        }
        TypeMirror parameter = processingEnv.getTypeUtils().erasure(hook.getParameters().getFirst().asType());
        if (processingEnv.getTypeUtils().isAssignable(processingEnv.getTypeUtils().erasure(settingsType.asType()), parameter)) {
            return CodeBlock.of("$T.$L($L)", ClassName.get(type), hook.getSimpleName(), settings);
        }
        return null;
    }

    /**
     * The localize text a method returns - {@code getDisplayName()} of a configurable.
     */
    private CodeBlock returnedText(ExecutableElement method) {
        MethodTree tree = myTrees.getTree(method);
        TreePath path = myTrees.getPath(method);
        if (tree == null || path == null || tree.getBody() == null || tree.getBody().getStatements().size() != 1
            || !(tree.getBody().getStatements().getFirst() instanceof ReturnTree returnTree)
            || !(returnTree.getExpression() instanceof MethodInvocationTree invocation)) {
            return null;
        }
        return mySourceTexts.toText(new TreePath(new TreePath(path, returnTree), invocation), invocation, (TypeElement) method.getEnclosingElement());
    }
}
