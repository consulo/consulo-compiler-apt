package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.FieldSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedVariable;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class JavaGeneratedVariable extends BaseGeneratedVariable {
    public JavaGeneratedVariable(GeneratedType type, String name) {
        super(type, name);
    }

    public FieldSpec toField() {
        FieldSpec.Builder spec = FieldSpec.builder(JavaGeneratorUtil.toTypeName(myType), myName);
        for (GeneratedModifier modifier : myModifiers) {
            spec.addModifiers(JavaGeneratorUtil.toModifier(modifier));
        }

        if (myInitializerExpression != null) {
            JavaGeneratedExpression javaGeneratedExpression = (JavaGeneratedExpression) myInitializerExpression;

            spec.initializer(javaGeneratedExpression.generate());
        }

        return spec.build();
    }
}
