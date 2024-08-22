package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public interface GeneratedVariable extends GeneratedAnnotatedMember {
    String getName();

    GeneratedType getType();

    void withModifiers(GeneratedModifier... modifiers);

    void withInitializer(GeneratedExpression expression);
}
