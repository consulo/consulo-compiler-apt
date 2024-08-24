package consulo.compiler.apt.shared;

import consulo.compiler.apt.shared.generation.*;
import consulo.compiler.apt.shared.generation.expression.GeneratedClassReferenceExpression;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.expression.GeneratedReferenceExpression;
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
    private final GeneratedElementFactory myFactory;

    public LocalizeGenerator(GeneratedElementFactory factory) {
        myFactory = factory;
    }

    public GeneratedClass parse(String relativePath, Path file) throws GenerationException {
        String pathWithoutExpresion = NameUtil.getNameWithoutExtension(relativePath).replace("/", ".");

        String pluginId = NameUtil.getPackageName(pathWithoutExpresion);
        String localizeId = NameUtil.getShortName(pathWithoutExpresion);

        String packageName = pluginId + ".localize";

        GeneratedClassType localizeKey = new GeneratedClassType("consulo.localize.LocalizeKey");
        GeneratedClassType localizeValue = new GeneratedClassType("consulo.localize.LocalizeValue");

        List<GeneratedVariable> fields = new ArrayList<>();
        List<GeneratedMethod> methods = new ArrayList<>();

        GeneratedVariable idField = myFactory.newVariable(new GeneratedClassType(String.class), "ID");
        idField.withModifiers(GeneratedModifier.PUBLIC, GeneratedModifier.STATIC, GeneratedModifier.FINAL);
        idField.withInitializer(myFactory.newConstantExpression(pluginId + "." + localizeId));
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

                GeneratedVariable keyField = myFactory.newVariable(localizeKey, fieldName);
                fields.add(keyField);
                keyField.withModifiers(GeneratedModifier.PRIVATE, GeneratedModifier.STATIC, GeneratedModifier.FINAL);

                List<GeneratedExpression> ofArguments = new ArrayList<>(3);
                ofArguments.add(myFactory.newReferenceExpression(idField.getName()));// ref;
                ofArguments.add(myFactory.newConstantExpression(key));
                ofArguments.add(myFactory.newConstantExpression(formatsByArgumentIndex.length));

                GeneratedClassReferenceExpression localizeKeyClass = myFactory.newClassReferenceExpression(localizeKey);
                keyField.withInitializer(myFactory.newMethodCallExpression(localizeKeyClass, "of", ofArguments));

                String methodName = NameUtil.normalizeName(NameUtil.captilizeByDot(key));

                GeneratedMethod getValueMethod = myFactory.newMethod(localizeValue, methodName);
                getValueMethod.withModifiers(GeneratedModifier.PUBLIC, GeneratedModifier.STATIC);
                methods.add(getValueMethod);

                GeneratedReferenceExpression ref = myFactory.newReferenceExpression(fieldName);

                if (formatsByArgumentIndex.length > 0) {
                    List<GeneratedVariable> params = new ArrayList<>(formatsByArgumentIndex.length);
                    List<GeneratedExpression> callArgs = new ArrayList<>(formatsByArgumentIndex.length);

                    for (int i = 0; i < formatsByArgumentIndex.length; i++) {
                        String name = "arg" + i;

                        callArgs.add(myFactory.newReferenceExpression(name));

                        params.add(myFactory.newVariable(new GeneratedClassType(Object.class), name));
                    }

                    getValueMethod.withStatement(myFactory.newReturnStatement(myFactory.newMethodCallExpression(ref, "getValue", callArgs)));

                    getValueMethod.withParameters(params);
                }
                else {

                    getValueMethod.withStatement(myFactory.newReturnStatement(myFactory.newMethodCallExpression(ref, "getValue", List.of())));
                }
            }
        }
        catch (Exception e) {
            throw new GenerationException(e.getMessage(), e);
        }

        GeneratedClass generatedClass = myFactory.newClass(packageName, localizeId);
        generatedClass.withFields(fields);
        generatedClass.withMethods(methods);
        return generatedClass;
    }
}
