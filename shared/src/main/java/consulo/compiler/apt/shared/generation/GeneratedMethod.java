package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.statement.GeneratedStatement;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-21
 */
public interface GeneratedMethod {
    GeneratedType getType();

    String getName();

    List<GeneratedVariable> getParameters();

    void withParameters(List<? extends GeneratedVariable> parameters);

    void withModifiers(GeneratedModifier... modifiers);

    void withStatement(GeneratedStatement statement);

    boolean hasModifier(GeneratedModifier modifier);
}
