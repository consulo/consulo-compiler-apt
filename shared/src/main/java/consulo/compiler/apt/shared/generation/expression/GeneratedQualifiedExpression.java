package consulo.compiler.apt.shared.generation.expression;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public record GeneratedQualifiedExpression(GeneratedExpression qualifier, GeneratedExpression expression) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitQualifiedExpression(this);
    }
}
