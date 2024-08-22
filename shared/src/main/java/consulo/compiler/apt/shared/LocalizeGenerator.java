package consulo.compiler.apt.shared;

import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.expression.GeneratedClassReferenceExpression;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class LocalizeGenerator {
    private final GeneratedElementFactory myElementFactory;

    public LocalizeGenerator(GeneratedElementFactory elementFactory) {
        myElementFactory = elementFactory;
    }

    public GeneratedClass parse(String relativePath, Path file) throws GenerationException {
        String pathWithoutExpresion = NameUtil.getNameWithoutExtension(relativePath).replace("/", ".");

        String pluginId = NameUtil.getPackageName(pathWithoutExpresion);
        String localizeId = NameUtil.getShortName(pathWithoutExpresion);

        String packageName = pluginId + ".localize";

        GeneratedClassType localizeKey = new GeneratedClassType("consulo.localize.LocalizeKey");
        GeneratedClassType localizeValue = new GeneratedClassType("consulo.localize.LocalizeValue");

        List<GeneratedVariable> fields = new ArrayList<>();
       // List<MethodSpec> methodSpecs = new ArrayList<>();

        GeneratedVariable idField = myElementFactory.newVariable(new GeneratedClassType(String.class), "ID");
        idField.withModifiers(GeneratedModifier.PUBLIC, GeneratedModifier.STATIC, GeneratedModifier.FINAL);
        idField.withInitializer(myElementFactory.newConstantExpression(pluginId + "." + localizeId));
        fields.add(idField);

        Yaml yaml = new Yaml();
        try (Reader stream = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Map<String, String>> o = yaml.load(stream);

            for (Map.Entry<String, Map<String, String>> entry : o.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);

                Map<String, String> value = entry.getValue();

                String t = value.get("text");
                String text = t == null ? "" : t;

                String fieldName = NameUtil.normalizeName(key.replace(".", "_").replace(" ", "_"));

                MessageFormat format;
                try {
                    format = new MessageFormat(text);
                }
                catch (Exception e) {
                    throw new GenerationException("Failed to parse text: " + text, e);
                }

                Format[] formatsByArgumentIndex = format.getFormatsByArgumentIndex();

                GeneratedVariable keyField = myElementFactory.newVariable(localizeKey, fieldName);
                fields.add(keyField);
                keyField.withModifiers(GeneratedModifier.PRIVATE, GeneratedModifier.STATIC, GeneratedModifier.FINAL);

                List<GeneratedExpression> ofArguments = new ArrayList<>(3);
                ofArguments.add(myElementFactory.newReferenceExpression(idField.getName()));// ref;
                ofArguments.add(myElementFactory.newConstantExpression(key));
                ofArguments.add(myElementFactory.newConstantExpression(formatsByArgumentIndex.length));

                GeneratedClassReferenceExpression localizeKeyClass = myElementFactory.newClassReferenceExpression(localizeKey);
                keyField.withInitializer(myElementFactory.newMethodCallExpression(localizeKeyClass, "of", ofArguments));

//                FieldSpec.Builder fieldSpec = FieldSpec.builder(localizeKey, fieldName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
//                fieldSpec.initializer(CodeBlock.builder().add("$T.of($L, $S, $L)", localizeKey, "ID", key, formatsByArgumentIndex.length).build());
//                fieldSpecs.add(fieldSpec.build());
//
//                String methodName = NameUtil.normalizeName(NameUtil.captilizeByDot(key));
//
//                MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(methodName);
//                methodSpec.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
//                methodSpec.returns(localizeValue);
//                if (formatsByArgumentIndex.length > 0) {
//                    StringBuilder argCall = new StringBuilder("return " + fieldName + ".getValue(");
//
//                    for (int i = 0; i < formatsByArgumentIndex.length; i++) {
//                        String parameterName = "arg" + i;
//
//                        methodSpec.addParameter(Object.class, parameterName);
//
//                        if (i != 0) {
//                            argCall.append(", ");
//                        }
//
//                        argCall.append(parameterName);
//                    }
//
//                    argCall.append(")");
//                    methodSpec.addStatement("$L", argCall.toString());
//                }
//                else {
//                    methodSpec.addStatement("$L", "return " + fieldName + ".getValue()");
//                }
//                methodSpecs.add(methodSpec.build());
            }
        }
        catch (Exception e) {
            throw new GenerationException(e.getMessage(), e);
        }

        GeneratedClass generatedClass = myElementFactory.newClass(packageName, localizeId);
        generatedClass.withFields(fields);
        return generatedClass;
//        TypeSpec typeSpec = TypeSpec.classBuilder(localizeId)
//            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "ALL").build())
//            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//            .addFields(fieldSpecs)
//            .addMethods(methodSpecs)
//            .addJavadoc("Generated code. Don't edit this class")
//            .build();
//
//        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
//            .build();
//
//        javaFile.writeTo(outputDirectoryFile);
    }
}
