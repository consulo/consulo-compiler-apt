package consulo.compiler.apt.shared.generation.type;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record GeneratedClassType(String className) implements GeneratedType {
    public GeneratedClassType(Class<?> clazz) {
        this(clazz.getName());
    }
}
