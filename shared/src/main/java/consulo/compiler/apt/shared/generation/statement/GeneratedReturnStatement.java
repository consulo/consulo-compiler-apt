package consulo.compiler.apt.shared.generation.statement;

import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public record GeneratedReturnStatement(GeneratedExpression expression) implements GeneratedStatement {
    @Override
    public <R> R accept(GeneratedStatementVisitor<R> visitor) {
        return visitor.visitReturnStatement(this);
    }
}
