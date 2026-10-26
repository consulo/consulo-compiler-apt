package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.statement.GeneratedReturnStatement;
import consulo.compiler.apt.shared.generation.statement.GeneratedStatementVisitor;

/**
 * Statements are rendered as method body expression ({@code def foo(): T = <expression>}).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratedStatementVisitor implements GeneratedStatementVisitor<String> {
    public static final ScalaGeneratedStatementVisitor INSTANCE = new ScalaGeneratedStatementVisitor();

    @Override
    public String visitReturnStatement(GeneratedReturnStatement generatedReturnStatement) {
        GeneratedExpression expression = generatedReturnStatement.expression();
        if (expression == null) {
            return "()";
        }
        return expression.accept(ScalaGeneratedExpressionVisitor.INSTANCE);
    }
}
