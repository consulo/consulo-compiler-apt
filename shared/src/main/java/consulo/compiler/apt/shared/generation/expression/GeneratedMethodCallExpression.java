package consulo.compiler.apt.shared.generation.expression;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record GeneratedMethodCallExpression(GeneratedExpression qualifier,
                                            String methodName,
                                            List<GeneratedExpression> arguments) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitMethodCallExpression(this);
    }
}
