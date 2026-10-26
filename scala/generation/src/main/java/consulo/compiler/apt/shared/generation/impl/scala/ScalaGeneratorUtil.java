package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * All references are emitted fully qualified with {@code _root_} prefix, since generated code is placed into user packages,
 * where a package named like {@code scala} or {@code java} can shadow the real one.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratorUtil {
    public static final String STATIC_ANNOTATION = "@_root_.scala.annotation.static";
    public static final String NOWARN_ANNOTATION = "@_root_.scala.annotation.nowarn";

    private static final Map<String, String> PRIMITIVES = Map.of(
        "boolean", "Boolean",
        "byte", "Byte",
        "short", "Short",
        "char", "Char",
        "int", "Int",
        "long", "Long",
        "float", "Float",
        "double", "Double",
        "void", "Unit"
    );

    private static final Set<String> KEYWORDS = Set.of(
        "abstract", "case", "catch", "class", "def", "do", "else", "enum", "export", "extends", "false", "final", "finally", "for",
        "forSome", "given", "if", "implicit", "import", "lazy", "macro", "match", "new", "null", "object", "override", "package",
        "private", "protected", "return", "sealed", "super", "then", "this", "throw", "trait", "true", "try", "type", "val", "var",
        "while", "with", "yield", "_"
    );

    public static String toTypeName(GeneratedType generatedType) {
        if (generatedType instanceof GeneratedTypeWithNullability typeWithNullability) {
            // nullability is not expressed - explicit nulls are not enabled by default
            return toTypeName(typeWithNullability.type());
        }

        if (generatedType instanceof GeneratedClassType classType) {
            return toClassName(classType);
        }

        if (generatedType instanceof GeneratedParametrizedType parametrizedType) {
            List<GeneratedType> arguments = parametrizedType.argumentTypes();
            String args = arguments.stream().map(ScalaGeneratorUtil::toTypeName).collect(Collectors.joining(", "));
            return toTypeName(parametrizedType.rawType()) + "[" + args + "]";
        }

        if (generatedType instanceof GeneratedWildcardType) {
            return "?";
        }

        if (generatedType instanceof GeneratedArrayType arrayType) {
            return "_root_.scala.Array[" + toTypeName(arrayType.innerType()) + "]";
        }

        throw new IllegalArgumentException(generatedType.toString());
    }

    public static String toClassName(GeneratedClassType classType) {
        Class<?> clazz = classType.clazz();
        String name = clazz != null ? clazz.getCanonicalName() : classType.className();

        String primitive = PRIMITIVES.get(name);
        if (primitive != null) {
            return "_root_.scala." + primitive;
        }

        return "_root_." + name;
    }

    public static String escapeName(String name) {
        if (KEYWORDS.contains(name) || !isPlainIdentifier(name)) {
            return "`" + name + "`";
        }
        return name;
    }

    private static boolean isPlainIdentifier(String name) {
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0)) || name.charAt(0) == '$') {
            return false;
        }

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isJavaIdentifierPart(c) || c == '$') {
                return false;
            }
        }
        return true;
    }

    public static String toStringLiteral(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    }
                    else {
                        builder.append(c);
                    }
            }
        }
        builder.append('"');
        return builder.toString();
    }

    public static String toModifiers(List<GeneratedModifier> modifiers) {
        // public is default, final & static are expressed by declaration kind (val, companion object)
        if (modifiers.contains(GeneratedModifier.PRIVATE)) {
            return "private ";
        }
        return "";
    }
}
