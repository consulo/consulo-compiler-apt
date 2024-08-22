package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class BaseGeneratedVariable implements GeneratedVariable {
    protected final GeneratedType myType;
    protected final String myName;
    protected final List<GeneratedModifier> myModifiers = new ArrayList<>();
    protected GeneratedExpression myInitializerExpression;

    public BaseGeneratedVariable(GeneratedType type, String name) {
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
}
