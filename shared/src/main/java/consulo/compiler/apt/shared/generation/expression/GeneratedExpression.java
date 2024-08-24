package consulo.compiler.apt.shared.generation.expression;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public interface GeneratedExpression {
    <R> R accept(GeneratedExpressionVisitor<R> visitor);
}
