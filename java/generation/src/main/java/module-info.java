/**
 * @author VISTALL
 * @since 2024-08-22
 */
module compiler.apt.java.generation {
    requires compiler.apt.shared;
    requires com.squareup.javapoet;
    requires java.compiler;

    provides consulo.compiler.apt.shared.generation.GeneratedElementFactory
        with consulo.compiler.apt.shared.generation.impl.java.JavaGeneratedElementFactory;
}