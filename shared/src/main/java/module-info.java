/**
 * @author VISTALL
 * @since 2024-08-20
 */
module compiler.apt.shared {
    requires org.yaml.snakeyaml;
    requires com.squareup.javapoet;
    requires java.compiler;

    exports consulo.compiler.apt.shared;
    exports consulo.compiler.apt.shared.generation;
    exports consulo.compiler.apt.shared.generation.type;
    exports consulo.compiler.apt.shared.generation.expression;
    //exports consulo.compiler.apt.shared.generation.statement;

    exports consulo.compiler.apt.shared.generation.impl;
}