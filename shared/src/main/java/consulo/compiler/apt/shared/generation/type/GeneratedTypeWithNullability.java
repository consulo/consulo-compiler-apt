package consulo.compiler.apt.shared.generation.type;

/**
 * @author VISTALL
 * @since 2024-11-04
 */
public record GeneratedTypeWithNullability(GeneratedType type, GeneratedNullability nullability) implements GeneratedType {
}
