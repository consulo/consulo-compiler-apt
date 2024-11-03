package consulo.compiler.apt.shared.generation.type;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record GeneratedClassType(String className, Class<?> clazz, Nullability nullability) implements GeneratedType {
    public GeneratedClassType(Class<?> clazz) {
        this(clazz.getName(), clazz, Nullability.UNSURE);
    }

    public GeneratedClassType(String className) {
        this(className, null, Nullability.UNSURE);
    }
}
