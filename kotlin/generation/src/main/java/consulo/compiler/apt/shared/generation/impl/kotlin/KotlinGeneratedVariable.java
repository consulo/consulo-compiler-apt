package consulo.compiler.apt.shared.generation.impl.kotlin;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.PropertySpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedVariable;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinGeneratedVariable extends BaseGeneratedVariable {
    public KotlinGeneratedVariable(GeneratedType type, String name) {
        super(type, name);
    }

    public PropertySpec toJvmField() {
        PropertySpec.Builder builder = PropertySpec.builder(myName, KotlinGeneratorUtil.toTypeName(myType));
        builder.addModifiers(myModifiers.stream()
            .filter(modifier -> modifier != GeneratedModifier.STATIC && modifier != GeneratedModifier.FINAL)
            .map(KotlinGeneratorUtil::toModifier)
            .toList());
        builder.addAnnotation(ClassName.bestGuess("kotlin.jvm.JvmField"));

        return builder.build();
    }
}
