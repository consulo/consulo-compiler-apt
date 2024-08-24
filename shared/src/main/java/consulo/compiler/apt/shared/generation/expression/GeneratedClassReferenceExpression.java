package consulo.compiler.apt.shared.generation.expression;

import consulo.compiler.apt.shared.generation.type.GeneratedClassType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record GeneratedClassReferenceExpression(GeneratedClassType type) implements GeneratedExpression {
    @Override
    public <R> R accept(GeneratedExpressionVisitor<R> visitor) {
        return visitor.visitClassReferenceExpression(this);
    }
}
