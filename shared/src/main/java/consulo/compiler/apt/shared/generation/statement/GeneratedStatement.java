package consulo.compiler.apt.shared.generation.statement;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public interface GeneratedStatement {
    <R> R accept(GeneratedStatementVisitor<R> visitor);
}
