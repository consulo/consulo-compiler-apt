module compiler.apt.scala.generation {
    requires compiler.apt.shared;

    exports consulo.compiler.apt.shared.generation.impl.scala;

    provides consulo.compiler.apt.shared.generation.GeneratedElementFactory
        with consulo.compiler.apt.shared.generation.impl.scala.ScalaGeneratedElementFactory;
}
