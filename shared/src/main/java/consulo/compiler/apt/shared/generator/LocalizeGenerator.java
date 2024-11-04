package consulo.compiler.apt.shared.generator;

import consulo.compiler.apt.shared.GenerationException;
import consulo.compiler.apt.shared.NameUtil;
import consulo.compiler.apt.shared.generation.*;
import consulo.compiler.apt.shared.generation.expression.GeneratedClassReferenceExpression;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.expression.GeneratedReferenceExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedNullability;
import consulo.compiler.apt.shared.generation.type.GeneratedType;
import consulo.compiler.apt.shared.generation.type.GeneratedTypeWithNullability;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Format;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class LocalizeGenerator {
    private record ParamInfo(String name, GeneratedType type) {
    }

    private static final Map<String, Class<?>> typesMap = new HashMap<>();

    static {
        typesMap.put("string", String.class);
        typesMap.put("string?", String.class);
        typesMap.put("byte", byte.class);
        typesMap.put("byte?", Byte.class);
        typesMap.put("short", short.class);
        typesMap.put("short?", Short.class);
        typesMap.put("int", int.class);
        typesMap.put("int?", Integer.class);
        typesMap.put("long", long.class);
        typesMap.put("long?", Long.class);
        typesMap.put("float", float.class);
        typesMap.put("float?", Float.class);
        typesMap.put("double", double.class);
        typesMap.put("double?", Double.class);
        typesMap.put("bool", boolean.class);
        typesMap.put("bool?", Boolean.class);
    }

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
            Map<String, Map<String, Object>> o = yaml.load(stream);

            for (Map.Entry<String, Map<String, Object>> entry : o.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);

                Map<String, Object> value = entry.getValue();

                String t = (String) value.get("text");
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

                List<ParamInfo> paramInfos = readParams(format, value);

                if (!paramInfos.isEmpty()) {
                    List<GeneratedVariable> params = new ArrayList<>(formatsByArgumentIndex.length);
                    List<GeneratedExpression> callArgs = new ArrayList<>(formatsByArgumentIndex.length);

                    for (ParamInfo paramInfo : paramInfos) {
                        callArgs.add(myFactory.newReferenceExpression(paramInfo.name()));

                        GeneratedVariable param = myFactory.newVariable(paramInfo.type(), paramInfo.name());

                        params.add(param);
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

    @SuppressWarnings("unchecked")
    private static List<ParamInfo> readParams(MessageFormat format, Map<String, Object> data) {
        Format[] formats = format.getFormatsByArgumentIndex();
        if (formats.length == 0) {
            return List.of();
        }

        List<String> types = (List<String>) data.get("types");
        List<String> names = (List<String>) data.get("names");

        List<ParamInfo> paramInfos = new ArrayList<>(formats.length);
        for (int i = 0; i < formats.length; i++) {
            Class<?> typeClass = Object.class;
            String name = "arg" + i;
            GeneratedNullability nullability = GeneratedNullability.NON_NULL;

            if (names != null && i < names.size()) {
                name = names.get(i);
            }

            if (types != null && i < types.size()) {
                String typeStr = types.get(i);

                Class<?> aClass = typesMap.get(typeStr);
                if (aClass == null) {
                    throw new IllegalArgumentException("Wrong type " + typeStr);
                }

                typeClass = aClass;
                if (typeStr.endsWith("?")) {
                    nullability = GeneratedNullability.NULLABLE;
                }
                else {
                    nullability = GeneratedNullability.NON_NULL;
                }
            }

            GeneratedType type = new GeneratedClassType(typeClass);

            paramInfos.add(new ParamInfo(name, new GeneratedTypeWithNullability(type, nullability)));
        }
        return paramInfos;
    }
}
