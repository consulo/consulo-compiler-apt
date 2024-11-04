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

            switch (typeWithNullability.nullability()) {
                case UNSURE:
                case NULLABLE:
                    return name.copy(true, List.of());
                case NON_NULL:
                    return name.copy(false, List.of());
            }

            return name;
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

        if (generatedType instanceof GeneratedParametizedType parametizedType) {
            ClassName typeName = (ClassName) toTypeName(parametizedType.rawType());
            TypeName[] params = parametizedType.argumentTypes().stream().map(KotlinGeneratorUtil::toTypeName).toArray(TypeName[]::new);
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
        switch (modifier) {
            case PUBLIC:
                return KModifier.PUBLIC;
            case PRIVATE:
                return KModifier.PRIVATE;
            //case STATIC:
            //    return KModifier.STATIC;
            case FINAL:
                return KModifier.FINAL;
            default:
                throw new IllegalArgumentException(modifier.name());
        }
    }
}
