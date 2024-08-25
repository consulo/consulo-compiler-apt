package consulo.compiler.apt.shared.generation.expression;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public record GeneratedArrayGetExpression(GeneratedExpression expression, GeneratedExpression in) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitArrayGetExpression(this);
    }
}
