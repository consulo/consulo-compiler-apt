package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.statement.GeneratedReturnStatement;
import consulo.compiler.apt.shared.generation.statement.GeneratedStatementVisitor;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class JavaGeneratedStatementVisitor implements GeneratedStatementVisitor<CodeBlock> {
    public static JavaGeneratedStatementVisitor INSTANCE = new JavaGeneratedStatementVisitor();

    @Override
    public CodeBlock visitReturnStatement(GeneratedReturnStatement generatedReturnStatement) {
        GeneratedExpression expression = generatedReturnStatement.expression();
        if (expression == null) {
            return CodeBlock.of("return;");
        }
        
        return CodeBlock.of("return $L;", expression.accept(JavaGeneratedExpressionVisitor.INSTANCE));
    }
}
