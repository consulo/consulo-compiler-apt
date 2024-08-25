package consulo.compiler.apt.shared.generation.expression;

import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public record GeneratedClassCastExpression(GeneratedType type, GeneratedExpression expression) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitClassCastExpression(this);
    }
}
