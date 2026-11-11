/**
 * @author VISTALL
 * @since 2024-08-20
 */
module compiler.apt.java {
    requires java.compiler;
    requires jdk.compiler;

    requires com.squareup.javapoet;

    requires compiler.apt.shared;

    // the searchable texts processor first - the binding processor claims @ExtensionImpl, so a processor after it
    // never sees the configurables
    provides javax.annotation.processing.Processor with
        consulo.internal.configurable.search.SearchableTextsProcessor,
        consulo.internal.injecting.binding.InjectingBindingProcessor,
        consulo.internal.injecting.binding.TopicBindingProcessor;
}