package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedVariable;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class JavaGeneratedVariable extends BaseGeneratedVariable {
    public JavaGeneratedVariable(GeneratedType type, String name) {
        super(type, name);
    }

    public ParameterSpec toParameter() {
        ParameterSpec.Builder spec = ParameterSpec.builder(JavaGeneratorUtil.toTypeName(myType), myName);
        if (myType instanceof GeneratedClassType generatedClassType) {
            switch (generatedClassType.nullability()) {
                case NON_NULL:
                    if (generatedClassType.clazz() == null || !generatedClassType.clazz().isPrimitive()) {
                        spec.addAnnotation(ClassName.bestGuess("jakarta.annotation.Nonnull"));
                    }
                    break;
                case NULLABLE:
                    spec.addAnnotation(ClassName.bestGuess("jakarta.annotation.Nullable"));
                    break;
            }
        }
        return spec.build();
    }

    public FieldSpec toField() {
        FieldSpec.Builder spec = FieldSpec.builder(JavaGeneratorUtil.toTypeName(myType), myName);

        for (GeneratedModifier modifier : myModifiers) {
            spec.addModifiers(JavaGeneratorUtil.toModifier(modifier));
        }

        if (myInitializerExpression != null) {
            spec.initializer(myInitializerExpression.accept(JavaGeneratedExpressionVisitor.INSTANCE));
        }

        return spec.build();
    }
}
