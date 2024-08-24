package consulo.compiler.apt.shared.generation.expression;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record GeneratedConstantExpression(Object value) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitConstantExpression(this);
    }
}
