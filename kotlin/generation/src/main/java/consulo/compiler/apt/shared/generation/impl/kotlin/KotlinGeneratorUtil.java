package consulo.compiler.apt.shared.generation.impl.kotlin;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.KModifier;
import com.squareup.kotlinpoet.TypeName;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinGeneratorUtil {
    public static TypeName toTypeName(GeneratedType generatedType) {
        if (generatedType instanceof GeneratedClassType classType) {
            return ClassName.bestGuess(classType.className());
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
