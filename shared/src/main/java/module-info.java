/**
 * @author VISTALL
 * @since 2024-08-20
 */
module compiler.apt.shared {
    requires org.yaml.snakeyaml;

    exports consulo.compiler.apt.shared;
    exports consulo.compiler.apt.shared.generation;
    exports consulo.compiler.apt.shared.generation.type;
    exports consulo.compiler.apt.shared.generation.expression;
    exports consulo.compiler.apt.shared.generation.statement;
    exports consulo.compiler.apt.shared.generator;

    uses consulo.compiler.apt.shared.generation.GeneratedElementFactory;
}