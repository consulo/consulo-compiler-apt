package consulo.compiler.apt.shared.generation.statement;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public interface GeneratedStatementVisitor<R> {
    R visitReturnStatement(GeneratedReturnStatement generatedReturnStatement);
}
