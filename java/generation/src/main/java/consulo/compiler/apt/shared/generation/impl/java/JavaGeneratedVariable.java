package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.*;
import consulo.compiler.apt.shared.generation.BaseGeneratedVariable;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;
import consulo.compiler.apt.shared.generation.type.GeneratedTypeWithNullability;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class JavaGeneratedVariable extends BaseGeneratedVariable {
    public JavaGeneratedVariable(GeneratedType type, String name) {
        super(type, name);
    }

    public ParameterSpec toParameter() {
        TypeName type = JavaGeneratorUtil.toTypeName(myType);

        if (myType instanceof GeneratedTypeWithNullability typeWithNullability) {
            switch (typeWithNullability.nullability()) {
                case NON_NULL:
                    // by default all types is nullable
                    break;
                case NULLABLE:
                    type = type.annotated(AnnotationSpec.builder(ClassName.get("org.jspecify.annotations", "Nullable")).build());
                    break;
            }
        }

        ParameterSpec.Builder spec = ParameterSpec.builder(type, myName);

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
