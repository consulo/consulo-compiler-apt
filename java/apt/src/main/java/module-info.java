/**
 * @author VISTALL
 * @since 2024-08-20
 */
module compiler.apt.java {
    requires java.compiler;

    requires com.squareup.javapoet;

    requires compiler.apt.shared;

    provides javax.annotation.processing.Processor with
        consulo.internal.injecting.binding.InjectingBindingProcessor,
        consulo.internal.injecting.binding.TopicBindingProcessor;
}