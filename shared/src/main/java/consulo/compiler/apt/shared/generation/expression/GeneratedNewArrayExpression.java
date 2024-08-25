package consulo.compiler.apt.shared.generation.expression;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public record GeneratedNewArrayExpression(List<GeneratedExpression> expressions) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitNewArrayExpression(this);
    }
}
