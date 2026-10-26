package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import consulo.compiler.apt.shared.generation.GeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratedElementFactory implements GeneratedElementFactory {
    @Override
    public String getId() {
        return "scala";
    }

    @Override
    public GeneratedClass newClass(String packageName, String name) {
        return new ScalaGeneratedClass(packageName, name);
    }

    @Override
    public GeneratedVariable newVariable(GeneratedType type, String name) {
        return new ScalaGeneratedVariable(type, name);
    }

    @Override
    public GeneratedMethod newMethod(GeneratedType type, String name) {
        return new ScalaGeneratedMethod(type, name);
    }
}
