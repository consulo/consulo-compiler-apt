package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.FieldSpec;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class JavaGeneratedVariable implements GeneratedVariable {
    private final GeneratedType myType;
    private final String myName;
    private final List<GeneratedModifier> myModifiers = new ArrayList<>();
    private GeneratedExpression myInitializerExpression;

    public JavaGeneratedVariable(GeneratedType type, String name) {
        myType = type;
        myName = name;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public GeneratedType getType() {
        return myType;
    }

    @Override
    public void withModifiers(GeneratedModifier[] modifiers) {
        Collections.addAll(myModifiers, modifiers);
    }

    @Override
    public void withInitializer(GeneratedExpression expression) {
        myInitializerExpression = expression;
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
