/**
 * @author VISTALL
 * @since 2024-08-22
 */
module compiler.apt.kotlin.generation {
    requires compiler.apt.shared;
    requires java.compiler;
    requires com.squareup.kotlinpoet;

    provides consulo.compiler.apt.shared.generation.GeneratedElementFactory
        with consulo.compiler.apt.shared.generation.impl.kotlin.KotlinGeneratedElementFactory;
}