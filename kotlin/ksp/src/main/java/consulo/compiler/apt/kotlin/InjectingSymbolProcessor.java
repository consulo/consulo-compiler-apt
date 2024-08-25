package consulo.compiler.apt.kotlin;

import com.google.devtools.ksp.UtilsKt;
import com.google.devtools.ksp.processing.*;
import com.google.devtools.ksp.symbol.*;
import consulo.compiler.apt.shared.ApiImplData;
import consulo.compiler.apt.shared.ComponentScope;
import consulo.compiler.apt.shared.ConsuloClasses;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import consulo.compiler.apt.shared.generation.GeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedArrayType;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedParametizedType;
import consulo.compiler.apt.shared.generation.type.GeneratedWildcardType;
import kotlin.Unit;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class InjectingSymbolProcessor implements SymbolProcessor {
    private record ApiInfo(KSDeclaration declaration, KSAnnotation annotation) {
    }

    private record ServiceInfo(String qualifiedName, KSFile file) implements Comparable<ServiceInfo> {
        @Override
        public int compareTo(ServiceInfo o) {
            return qualifiedName().compareTo(o.qualifiedName());
        }
    }

    private final CodeGenerator myCodeGenerator;
    private final KSPLogger myLogger;

    public InjectingSymbolProcessor(CodeGenerator codeGenerator, KSPLogger logger) {
        myCodeGenerator = codeGenerator;
        myLogger = logger;
    }

    @NotNull
    @Override
    public List<KSAnnotated> process(@NotNull Resolver resolver) {
        List<KSAnnotated> result = new ArrayList<>();

        GeneratedElementFactory factory = GeneratedElementFactory.of("kotlin");

        Map<String, Set<ServiceInfo>> providers = new HashMap<>();

        for (Map.Entry<String, String> entry : ApiImplData.getApiAnnotations().entrySet()) {
            String implClass = entry.getKey();
            String apiClass = entry.getValue();

            Sequence<KSAnnotated> symbols = resolver.getSymbolsWithAnnotation(implClass, false);

            SequencesKt.forEach(SequencesKt.filter(symbols, ksAnnotated -> !UtilsKt.validate(ksAnnotated, (t, t2) -> true)), it -> {
                result.add(it);
                return Unit.INSTANCE;
            });

            SequencesKt.forEach(symbols, it -> {
                if (it instanceof KSClassDeclaration clazz && UtilsKt.validate(clazz, (t, t2) -> true)) {
                    GeneratedClass newClass = factory.newClass(clazz.getPackageName().asString(), clazz.getSimpleName().asString() + "_Binding");

                    String qualifiedName = newClass.getPackageName() + "." + newClass.getName();

                    if (ConsuloClasses.consulo.annotation.component.TopicImpl.equals(implClass)) {
                        generateTopicBinding(clazz, newClass, apiClass, implClass, factory);

                        providers.computeIfAbsent(ConsuloClasses.consulo.component.bind.TopicBinding, t -> new TreeSet<>())
                            .add(new ServiceInfo(qualifiedName, clazz.getContainingFile()));
                    }
                    else {
                        generateInjectingBinding(clazz, newClass, apiClass, implClass, factory);

                        providers.computeIfAbsent(ConsuloClasses.consulo.component.bind.InjectingBinding, t -> new TreeSet<>())
                            .add(new ServiceInfo(qualifiedName, clazz.getContainingFile()));
                    }

                    try (OutputStreamWriter writer = new OutputStreamWriter(myCodeGenerator.createNewFile(new Dependencies(true, clazz.getContainingFile()), newClass.getPackageName(), newClass.getName(), "kt"), StandardCharsets.UTF_8)) {
                        newClass.write(writer);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                return Unit.INSTANCE;
            });
        }

        for (Map.Entry<String, Set<ServiceInfo>> entry : providers.entrySet()) {
            String serviceQName = entry.getKey();
            Set<ServiceInfo> classes = entry.getValue();

            KSFile[] ksFiles = classes.stream().map(ServiceInfo::file).toArray(KSFile[]::new);

            String serviceFile = "META-INF/services/" + serviceQName;
            Dependencies dependencies = new Dependencies(true, ksFiles);

            String data = classes.stream().map(ServiceInfo::qualifiedName).collect(Collectors.joining(System.lineSeparator()));

            try (OutputStreamWriter writer = new OutputStreamWriter(myCodeGenerator.createNewFile(dependencies, "", serviceFile, ""), StandardCharsets.UTF_8)) {
                writer.write(data);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private void generateInjectingBinding(KSClassDeclaration ktClass, GeneratedClass generatedClass, String apiClass, String implClass, GeneratedElementFactory f) {
        ApiInfo apiAnnotation = findAnnotationDeep(ktClass, apiClass);
        if (apiAnnotation == null) {
            throw new IllegalArgumentException("Found find " + apiClass + " in super list: " + ktClass.getQualifiedName().asString());
        }

        KSName qualifiedName = Objects.requireNonNull(ktClass.getQualifiedName());

        GeneratedClassType injectingBindingType = new GeneratedClassType(ConsuloClasses.consulo.component.bind.InjectingBinding);

        generatedClass.withSuperInterfaces(List.of(injectingBindingType));

        KSAnnotation implAnnotation = Objects.requireNonNull(findAnnotation(ktClass, implClass));

        List<GeneratedMethod> methods = new ArrayList<>();

        GeneratedMethod getComponentAnnotationClass = f.newMethod(
            new GeneratedParametizedType(new GeneratedClassType(Class.class), List.of(new GeneratedWildcardType())),
            "getComponentAnnotationClass"
        );

        getComponentAnnotationClass.withModifiers(GeneratedModifier.PUBLIC);
        getComponentAnnotationClass.withOverride();
        getComponentAnnotationClass.withStatement(f.newReturnStatement(f.newClassClassExpression(new GeneratedClassType(apiClass))));
        methods.add(getComponentAnnotationClass);

        GeneratedMethod getImplClass = f.newMethod(
            new GeneratedParametizedType(new GeneratedClassType(Class.class), List.of(new GeneratedWildcardType())),
            "getImplClass"
        );
        getImplClass.withModifiers(GeneratedModifier.PUBLIC);
        getImplClass.withOverride();
        getImplClass.withStatement(f.newReturnStatement(f.newClassClassExpression(new GeneratedClassType(qualifiedName.asString()))));
        methods.add(getImplClass);

        GeneratedMethod getApiClassName = f.newMethod(new GeneratedClassType(String.class), "getApiClassName");
        getApiClassName.withModifiers(GeneratedModifier.PUBLIC);
        getApiClassName.withOverride();
        getApiClassName.withStatement(f.newReturnStatement(f.newConstantExpression(apiAnnotation.declaration().getQualifiedName().asString())));
        methods.add(getApiClassName);

        GeneratedMethod getApiClass = f.newMethod(
            new GeneratedParametizedType(new GeneratedClassType(Class.class), List.of(new GeneratedWildcardType())),
            "getApiClass"
        );
        getApiClass.withModifiers(GeneratedModifier.PUBLIC);
        getApiClass.withOverride();
        getApiClass.withStatement(f.newReturnStatement(f.newClassClassExpression(new GeneratedClassType(apiAnnotation.declaration().getQualifiedName().asString()))));
        methods.add(getApiClass);

        KSFunctionDeclaration injectConstructor = findInjectConstructor(ktClass);

        GeneratedMethod getParametersCount = f.newMethod(new GeneratedClassType(int.class), "getParametersCount");
        getParametersCount.withModifiers(GeneratedModifier.PUBLIC);
        getParametersCount.withOverride();
        getParametersCount.withStatement(f.newReturnStatement(f.newConstantExpression(injectConstructor.getParameters().size())));
        methods.add(getParametersCount);

        Integer profilesMask = (Integer) findAnnotationValue(implAnnotation, "profiles");
        GeneratedMethod getComponentProfiles = f.newMethod(new GeneratedClassType(int.class), "getComponentProfiles");
        getComponentProfiles.withModifiers(GeneratedModifier.PUBLIC);
        getComponentProfiles.withOverride();
        getComponentProfiles.withStatement(f.newReturnStatement(f.newConstantExpression(profilesMask)));
        methods.add(getComponentProfiles);

        Boolean lazy = (Boolean) findAnnotationValue(implAnnotation, "lazy");
        if (lazy != null) {
            GeneratedMethod isLazy = f.newMethod(new GeneratedClassType(boolean.class), "isLazy");
            isLazy.withModifiers(GeneratedModifier.PUBLIC);
            isLazy.withOverride();
            isLazy.withStatement(f.newReturnStatement(f.newConstantExpression(lazy)));
            methods.add(isLazy);
        }

        Object value = findAnnotationValue(apiAnnotation.annotation(), "value");
        if (value instanceof KSType ksType) {
            String scopeName = ksType.getDeclaration().getSimpleName().asString();
            ComponentScope componentScope = ComponentScope.valueOf(scopeName);

            GeneratedClassType componentScopeType = new GeneratedClassType(ConsuloClasses.consulo.annotation.component.ComponentScope);
            GeneratedMethod getComponentScope = f.newMethod(componentScopeType, "getComponentScope");
            getComponentScope.withModifiers(GeneratedModifier.PUBLIC);
            getComponentScope.withOverride();
            getComponentScope.withStatement(f.newReturnStatement(
                f.newQualifiedExpression(f.newClassReferenceExpression(componentScopeType), f.newReferenceExpression(componentScope.name()))
            ));
            methods.add(getComponentScope);
        }

        GeneratedMethod getParameterTypes = f.newMethod(new GeneratedArrayType(new GeneratedClassType(Type.class)), "getParameterTypes");
        getParameterTypes.withModifiers(GeneratedModifier.PUBLIC);
        getParameterTypes.withOverride();
        if (injectConstructor.getParameters().isEmpty()) {
            getParameterTypes.withStatement(f.newReturnStatement(f.newQualifiedExpression(f.newClassReferenceExpression(injectingBindingType), f.newReferenceExpression("EMPTY_TYPES"))));
        }
        else {
            List<GeneratedExpression> arrayExpressions = new ArrayList<>();
            for (KSValueParameter parameter : injectConstructor.getParameters()) {
                arrayExpressions.add(f.newClassClassExpression(toType(parameter.getType().resolve())));
            }

            getParameterTypes.withStatement(f.newReturnStatement(f.newArrayExpression(arrayExpressions)));
        }
        methods.add(getParameterTypes);

        GeneratedMethod create = f.newMethod(new GeneratedClassType(Object.class), "create");
        String argsParam = "args";
        create.withParameters(List.of(f.newVariable(new GeneratedArrayType(new GeneratedClassType(Object.class)), argsParam)));
        create.withModifiers(GeneratedModifier.PUBLIC);
        create.withOverride();
        List<GeneratedExpression> newExpressionArgs = new ArrayList<>();

        if (!injectConstructor.getParameters().isEmpty()) {
            int index = 0;
            for (KSValueParameter parameter : injectConstructor.getParameters()) {
                GeneratedExpression pExpr = f.newArrayGetExpression(f.newReferenceExpression(argsParam), index++);
                pExpr = f.newClassCastExpression(toType(parameter.getType().resolve()), pExpr);
                newExpressionArgs.add(pExpr);
            }
        }
        create.withStatement(f.newReturnStatement(f.newExpression(toType(ktClass.asType(List.of())), newExpressionArgs)));
        methods.add(create);

        generatedClass.withMethods(methods);
    }

    private GeneratedClassType toType(KSType type) {
        KSDeclaration declaration = type.getDeclaration();

        KSName qualifiedName = Objects.requireNonNull(declaration.getQualifiedName());

        return new GeneratedClassType(qualifiedName.asString());
    }

    private void generateTopicBinding(KSClassDeclaration ktClass, GeneratedClass generatedClass, String apiClass, String implClass, GeneratedElementFactory factory) {
        ApiInfo apiAnnotation = findAnnotationDeep(ktClass, apiClass);
        if (apiAnnotation == null) {
            throw new IllegalArgumentException("Found find " + apiClass + " in super list");
        }

        generatedClass.withSuperInterfaces(List.of(new GeneratedClassType(ConsuloClasses.consulo.component.bind.TopicBinding)));

        Sequence<KSFunctionDeclaration> constructors = UtilsKt.getConstructors(ktClass);

        KSAnnotation implAnnotation = findAnnotation(ktClass, implClass);

        List<GeneratedMethod> methods = new ArrayList<>();


        generatedClass.withMethods(methods);

        if (Boolean.TRUE) {
            throw new UnsupportedOperationException(ktClass.getQualifiedName().asString());
        }
    }

    private KSFunctionDeclaration findInjectConstructor(KSClassDeclaration ktClass) {
        Sequence<KSFunctionDeclaration> constructors = UtilsKt.getConstructors(ktClass);

        for (KSFunctionDeclaration declaration : SequencesKt.asIterable(constructors)) {
            KSAnnotation annotation = findAnnotation(declaration, ConsuloClasses.jakarta.inject.Inject);
            if (annotation != null) {
                return declaration;
            }

            if (declaration.getParameters().isEmpty() && declaration.getModifiers().contains(Modifier.PUBLIC)) {
                return declaration;
            }
        }

        throw new RuntimeException("There no public constructor or constructor with @Inject annotation. Injecting impossible. Class: " + ktClass.getQualifiedName().asString());
    }

    private Object findAnnotationValue(KSAnnotation annotation, String parameter) {
        for (KSValueArgument argument : annotation.getArguments()) {
            if (parameter.equals(argument.getName().asString())) {
                return argument.getValue();
            }
        }

        for (KSValueArgument argument : annotation.getDefaultArguments()) {
            if (parameter.equals(argument.getName().asString())) {
                return argument.getValue();
            }
        }

        return null;
    }

    private static ApiInfo findAnnotationDeep(KSClassDeclaration annotated, String annotationClass) {
        KSAnnotation annotation = findAnnotation(annotated, annotationClass);
        if (annotation != null) {
            return new ApiInfo(annotated, annotation);
        }

        for (KSTypeReference reference : SequencesKt.asIterable(annotated.getSuperTypes())) {
            KSDeclaration declaration = reference.resolve().getDeclaration();

            if (declaration instanceof KSClassDeclaration deepClass) {
                ApiInfo deepAnnotation = findAnnotationDeep(deepClass, annotationClass);
                if (deepAnnotation != null) {
                    return deepAnnotation;
                }
            }
        }

        return null;
    }

    private static KSAnnotation findAnnotation(KSAnnotated annotated, String annotationClass) {
        Sequence<KSAnnotation> annotations = annotated.getAnnotations();

        for (KSAnnotation annotation : SequencesKt.asIterable(annotations)) {
            KSType type = annotation.getAnnotationType().resolve();

            KSDeclaration declaration = type.getDeclaration();

            KSName qualifiedName = declaration.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            String qName = qualifiedName.asString();

            if (Objects.equals(annotationClass, qName)) {
                return annotation;
            }
        }

        return null;
    }
}
