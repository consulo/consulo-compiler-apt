package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.statement.GeneratedStatement;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public abstract class BaseGeneratedMethod implements GeneratedMethod {
    protected final GeneratedType myType;
    protected final String myName;
    protected final List<GeneratedVariable> myParameters = new ArrayList<>();
    protected final List<GeneratedModifier> myModifiers = new ArrayList<>();
    protected GeneratedStatement myStatement;

    public BaseGeneratedMethod(GeneratedType type, String name) {
        myType = type;
        myName = name;
    }

    @Override
    public void withStatement(GeneratedStatement statement) {
        myStatement = statement;
    }

    @Override
    public void withParameters(List<? extends GeneratedVariable> parameters) {
        myParameters.addAll(parameters);
    }

    @Override
    public void withModifiers(GeneratedModifier[] modifiers) {
        Collections.addAll(myModifiers, modifiers);
    }

    @Override
    public boolean hasModifier(GeneratedModifier modifier) {
        return myModifiers.contains(modifier);
    }

    @Override
    public GeneratedType getType() {
        return myType;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public List<GeneratedVariable> getParameters() {
        return myParameters;
    }
}
