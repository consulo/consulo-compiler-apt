/*
 * Copyright 2013-2022 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.internal.injecting.binding;

import com.squareup.javapoet.*;
import consulo.compiler.apt.shared.ApiImplData;
import consulo.compiler.apt.shared.ComponentScope;
import consulo.compiler.apt.shared.ConsuloClasses;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author VISTALL
 * @since 16-Jun-22
 * See https://github.com/google/auto/blob/master/service/processor/src/main/java/com/google/auto/service/processor/AutoServiceProcessor.java
 */
@SupportedAnnotationTypes({ConsuloClasses.consulo.annotation.component.ServiceImpl,
    ConsuloClasses.consulo.annotation.component.ExtensionImpl,
    ConsuloClasses.consulo.annotation.component.TopicImpl,
    ConsuloClasses.consulo.annotation.component.ActionImpl
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class InjectingBindingProcessor extends BindingProcessor {
    private static record AnnotationResolveInfo(AnnotationMirror annotation, TypeElement typeElement) {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return true;
        }

        ClassName componentScopeClassName = ClassName.bestGuess(ConsuloClasses.consulo.annotation.component.ComponentScope);

        AnnotationSpec suppressWarning = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", CodeBlock.of("$S", "ALL")).build();

        Filer filer = processingEnv.getFiler();

        Map<String, Set<String>> providers = new HashMap<>();

        String injectingBindingClassName = "consulo.component.bind.InjectingBinding";
        ClassName injectingBindingClass = ClassName.bestGuess(injectingBindingClassName);

        GeneratedElementFactory factory = GeneratedElementFactory.of("java");

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);

            String implClassName = annotation.getQualifiedName().toString();

            String apiClassName = ApiImplData.getApiAnnotation(implClassName);
            if (apiClassName == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getQualifiedName() + " not supported");
                return false;
            }

            ClassName apiClass = ClassName.bestGuess(apiClassName);

            for (Element element : elementsAnnotatedWith) {
                if (!(element instanceof TypeElement)) {
                    continue;
                }

                TypeElement typeElement = (TypeElement) element;
                AnnotationResolveInfo apiInfo = findAnnotationInSuper(typeElement, apiClassName);
                if (apiInfo == null) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can't find single @" + apiClassName + " annotation for: " + typeElement.getQualifiedName() + " in super classes.", typeElement);
                    return false;
                }

                try {
                    String bindingQualifiedName = typeElement.getQualifiedName() + "_Binding";

                    providers.computeIfAbsent(injectingBindingClassName, (c) -> new HashSet<>()).add(bindingQualifiedName);

                    GeneratedClass generatedClass = factory.newClass(typeElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "_Binding");
                    generatedClass.withSuperInterfaces(List.of(new GeneratedClassType(injectingBindingClassName)));

                    TypeSpec.Builder bindBuilder = TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "_Binding");
                    bindBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
                    bindBuilder.addAnnotation(suppressWarning);
                    bindBuilder.addSuperinterface(injectingBindingClass);

                    bindBuilder.addMethod(MethodSpec.methodBuilder("getApiClass").returns(Class.class).addModifiers(Modifier.PUBLIC)
                        .addCode(CodeBlock.of("return $T.class;", ClassName.bestGuess(apiInfo.typeElement().getQualifiedName().toString()))).build());

                    bindBuilder.addMethod(MethodSpec.methodBuilder("getApiClassName").returns(String.class).addModifiers(Modifier.PUBLIC)
                        .addCode(CodeBlock.of("return $S;", apiInfo.typeElement().getQualifiedName().toString())).build());

                    bindBuilder.addMethod(MethodSpec.methodBuilder("getImplClass").returns(Class.class).addModifiers(Modifier.PUBLIC).addCode(CodeBlock.of("return $T.class;", typeElement)).build());

                    bindBuilder.addMethod(MethodSpec.methodBuilder("getComponentAnnotationClass").addModifiers(Modifier.PUBLIC).returns(Class.class)
                        .addCode(CodeBlock.of("return $T.class;", apiClass)).build());

                    ComponentScope scope;
                    // use TopicImpl scope
                    if (Objects.equals(apiClassName, ConsuloClasses.consulo.annotation.component.TopicAPI)) {
                        AnnotationMirror topicImpl = findAnnotation(typeElement, ConsuloClasses.consulo.annotation.component.TopicImpl);
                        scope = getScope(processingEnv.getElementUtils(), Objects.requireNonNull(topicImpl));
                    }
                    else {
                        scope = getScope(processingEnv.getElementUtils(), apiInfo.annotation());
                    }

                    bindBuilder.addMethod(
                        MethodSpec.methodBuilder("getComponentScope")
                            .addModifiers(Modifier.PUBLIC)
                            .returns(componentScopeClassName)
                            .addCode(CodeBlock.of("return $T.$L;", componentScopeClassName, scope.name()))
                            .build());

                    if (!isLazy(processingEnv.getElementUtils(), apiInfo.annotation())) {
                        bindBuilder.addMethod(MethodSpec.methodBuilder("isLazy").addModifiers(Modifier.PUBLIC).returns(boolean.class).addCode(CodeBlock.of("return false;")).build());
                    }

                    List<? extends VariableElement> injectParameters = null;

                    List<? extends Element> allMembers = processingEnv.getElementUtils().getAllMembers(typeElement);
                    for (Element member : allMembers) {
                        if (member instanceof ExecutableElement) {
                            Name simpleName = member.getSimpleName();
                            if ("<init>".equals(simpleName.toString())) {
                                List<? extends VariableElement> parameters = ((ExecutableElement) member).getParameters();

                                AnnotationMirror injectAnno = findAnnotation(member, ConsuloClasses.jakarta.inject.Inject);
                                if (injectAnno != null) {
                                    injectParameters = parameters;
                                    break;
                                }
                            }
                        }
                    }

                    if (injectParameters == null) {
                        for (Element member : allMembers) {
                            if (member instanceof ExecutableElement) {
                                Name simpleName = member.getSimpleName();
                                if ("<init>".equals(simpleName.toString())) {
                                    List<? extends VariableElement> parameters = ((ExecutableElement) member).getParameters();

                                    // default constructor
                                    if (parameters.size() == 0 && member.getModifiers().contains(Modifier.PUBLIC)) {
                                        injectParameters = parameters;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (injectParameters == null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "There no public constructor or constructor with @Inject annotation. Injecting impossible", typeElement);
                        return false;
                    }

                    AnnotationMirror implAnnotation = Objects.requireNonNull(findAnnotation(typeElement, implClassName), implClassName);

                    int componentProfiles = getComponentProfiles(processingEnv.getElementUtils(), implAnnotation);

                    bindBuilder.addMethod(MethodSpec.methodBuilder("getComponentProfiles").addModifiers(Modifier.PUBLIC).returns(int.class).addCode(CodeBlock.of("return $L;", componentProfiles)).build());

                    bindBuilder.addMethod(MethodSpec.methodBuilder("getParametersCount").addModifiers(Modifier.PUBLIC).returns(int.class).addCode(CodeBlock.of("return $L;", injectParameters.size())).build());

                    List<TypeName> paramTypes = new ArrayList<>();

                    if (!injectParameters.isEmpty()) {
                        AppendTypeResult types = appendTypes(injectParameters, paramTypes, "return ", ";", true);

                        bindBuilder.addMethod(
                            MethodSpec.methodBuilder("getParameterTypes").addModifiers(Modifier.PUBLIC).returns(Type[].class).addCode(CodeBlock.of(types.result(), types.types().toArray())).build());
                    }
                    else {
                        bindBuilder.addMethod(MethodSpec.methodBuilder("getParameterTypes").addModifiers(Modifier.PUBLIC).returns(Type[].class).addCode(CodeBlock.of("return EMPTY_TYPES;")).build());
                    }

                    List<TypeName> argsTypes = new ArrayList<>();
                    argsTypes.add(toTypeName(typeElement));
                    argsTypes.addAll(paramTypes);

                    StringBuilder newCreationBuilder = new StringBuilder();
                    newCreationBuilder.append("return new $T(");
                    for (int i = 0; i < injectParameters.size(); i++) {
                        if (i != 0) {
                            newCreationBuilder.append(", ");
                        }
                        newCreationBuilder.append("($T) args[").append(i).append("]");
                    }

                    newCreationBuilder.append(");");

                    bindBuilder.addMethod(MethodSpec.methodBuilder("create").addParameter(Object[].class, "args").addModifiers(Modifier.PUBLIC).returns(Object.class)
                        .addCode(CodeBlock.of(newCreationBuilder.toString(), argsTypes.toArray())).build());

                    TypeSpec bindClass = bindBuilder.build();

                    PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);

                    JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), bindClass).build();

                    JavaFileObject bindingObject = filer.createSourceFile(bindingQualifiedName);
                    try (Writer writer = bindingObject.openWriter()) {
                        javaFile.writeTo(writer);
                    }
                }
                catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), typeElement);
                }
            }
        }

        generateConfigFiles(providers);

        return true;
    }

    private static ComponentScope getScope(Elements elementUtils, AnnotationMirror annotation) {
        TypeElement typeElement = (TypeElement) annotation.getAnnotationType().asElement();

        if (typeElement.getQualifiedName().contentEquals(ConsuloClasses.consulo.annotation.component.ActionAPI)) {
            // actions always bind in application injecting scope
            return ComponentScope.APPLICATION;
        }

        Map<? extends ExecutableElement, ? extends AnnotationValue> map = elementUtils.getElementValuesWithDefaults(annotation);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : map.entrySet()) {
            ExecutableElement key = entry.getKey();
            AnnotationValue value = entry.getValue();

            if (key.getSimpleName().contentEquals("value")) {
                VariableElement attValue = (VariableElement) value.getValue();

                return ComponentScope.valueOf(attValue.getSimpleName().toString());
            }
        }

        throw new UnsupportedOperationException(annotation.getClass().getName());
    }

    private static int getComponentProfiles(Elements elementUtils, AnnotationMirror annotation) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> map = elementUtils.getElementValuesWithDefaults(annotation);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : map.entrySet()) {
            ExecutableElement key = entry.getKey();
            AnnotationValue value = entry.getValue();

            if (key.getSimpleName().contentEquals("profiles")) {
                return (Integer) value.getValue();
            }
        }

        throw new UnsupportedOperationException(annotation.getClass().getName());
    }

    private static boolean isLazy(Elements elementUtils, AnnotationMirror annotation) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> map = elementUtils.getElementValuesWithDefaults(annotation);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : map.entrySet()) {
            ExecutableElement key = entry.getKey();
            AnnotationValue value = entry.getValue();

            if (key.getSimpleName().contentEquals("lazy")) {
                return (Boolean) value.getValue();
            }
        }

        return true;
    }

    // TODO cache it ? per super type
    private static AnnotationResolveInfo findAnnotationInSuper(TypeElement typeElement, String annotationClass) {
        Set<AnnotationResolveInfo> targets = new LinkedHashSet<>();
        findAnnotationInSuper(typeElement, annotationClass, new HashSet<>(), targets);

        // FIXME [VISTALL] this is dirty hack since we have two api annotations
        if (ConsuloClasses.consulo.annotation.component.ActionAPI.equals(annotationClass)) {
            AnnotationResolveInfo actionGroupInfo = null;
            AnnotationResolveInfo actionInfo = null;

            for (AnnotationResolveInfo target : targets) {
                String qName = target.typeElement().getQualifiedName().toString();
                if (qName.equals("consulo.ui.ex.action.AnAction")) {
                    actionInfo = target;
                }
                else if (qName.equals("consulo.ui.ex.action.ActionGroup")) {
                    actionGroupInfo = target;
                }
            }

            if (actionInfo != null && actionGroupInfo != null) {
                targets.remove(actionInfo);
            }
        }

        if (targets.isEmpty() || targets.size() != 1) {
            return null;
        }
        return targets.iterator().next();
    }

    private static void findAnnotationInSuper(TypeElement typeElement, String annotationClass, Set<TypeElement> processed, Set<AnnotationResolveInfo> targets) {
        if (!processed.add(typeElement)) {
            return;
        }

        AnnotationMirror annotation = findAnnotation(typeElement, annotationClass);
        if (annotation != null) {
            targets.add(new AnnotationResolveInfo(annotation, typeElement));
        }

        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass != null) {
            if (superclass instanceof DeclaredType) {
                findAnnotationInSuper((TypeElement) ((DeclaredType) superclass).asElement(), annotationClass, processed, targets);
            }
        }

        for (TypeMirror typeMirror : typeElement.getInterfaces()) {
            if (typeMirror instanceof DeclaredType) {
                findAnnotationInSuper((TypeElement) ((DeclaredType) typeMirror).asElement(), annotationClass, processed, targets);
            }
        }
    }

    private static AnnotationMirror findAnnotation(Element typeElement, String annotationClass) {
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            DeclaredType annotationType = mirror.getAnnotationType();

            TypeElement annotationElement = (TypeElement) annotationType.asElement();

            if (annotationElement.getQualifiedName().contentEquals(annotationClass)) {
                return mirror;
            }
        }

        return null;
    }
}
