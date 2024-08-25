package consulo.compiler.apt.shared.generation.type;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record GeneratedParametizedType(GeneratedType rawType, List<GeneratedType> argumentTypes) implements GeneratedType {
}
