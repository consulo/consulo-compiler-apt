package consulo.compiler.apt.shared.generation.expression;

import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public record GeneratedNewExpression(GeneratedType type, List<GeneratedExpression> arguments) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitNewExpression(this);
    }
}
