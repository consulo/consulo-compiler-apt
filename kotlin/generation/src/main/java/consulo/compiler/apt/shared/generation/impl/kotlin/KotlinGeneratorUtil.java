package consulo.compiler.apt.shared.generation.impl.kotlin;

import com.squareup.kotlinpoet.*;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.*;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinGeneratorUtil {
    public static TypeName toTypeName(GeneratedType generatedType) {
        if (generatedType instanceof GeneratedTypeWithNullability typeWithNullability) {
            TypeName name = toTypeName(typeWithNullability.type());

            return switch (typeWithNullability.nullability()) {
                case UNSURE, NULLABLE -> name.copy(true, List.of());
                case NON_NULL -> name.copy(false, List.of());
            };
        }

        if (generatedType instanceof GeneratedClassType classType) {
            if ("java.lang.String".equals(classType.className())) {
                return TypeNames.STRING;
            }

            if ("java.lang.Object".equals(classType.className())) {
                return TypeNames.ANY;
            }

            if ("int".equals(classType.className())) {
                return TypeNames.INT;
            }

            if ("boolean".equals(classType.className())) {
                return TypeNames.BOOLEAN;
            }

            return ClassName.bestGuess(classType.className());
        }

        if (generatedType instanceof GeneratedParametrizedType parametrizedType) {
            ClassName typeName = (ClassName) toTypeName(parametrizedType.rawType());
            TypeName[] params = parametrizedType.argumentTypes().stream().map(KotlinGeneratorUtil::toTypeName).toArray(TypeName[]::new);
            return ParameterizedTypeName.get(typeName, params);
        }

        if (generatedType instanceof GeneratedWildcardType) {
            return WildcardTypeName.producerOf(TypeNames.ANY);
        }

        if (generatedType instanceof GeneratedArrayType arrayType) {
            TypeName innerType = toTypeName(arrayType.innerType());
            return ParameterizedTypeName.get(TypeNames.ARRAY, innerType);
        }

        throw new IllegalArgumentException(generatedType.toString());
    }

    public static KModifier toModifier(GeneratedModifier modifier) {
        return switch (modifier) {
            case PUBLIC -> KModifier.PUBLIC;
            case PRIVATE -> KModifier.PRIVATE;
            //case STATIC -> KModifier.STATIC;
            case FINAL -> KModifier.FINAL;
            default -> throw new IllegalArgumentException(modifier.name());
        };
    }
}
