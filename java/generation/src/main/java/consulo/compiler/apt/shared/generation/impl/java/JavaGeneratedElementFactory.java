package consulo.compiler.apt.shared.generation.impl.java;

import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import consulo.compiler.apt.shared.generation.GeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class JavaGeneratedElementFactory implements GeneratedElementFactory {

    @Override
    public String getId() {
        return "java";
    }

    @Override
    public GeneratedClass newClass(String packageName, String name) {
        return new JavaGeneratedClass(packageName, name);
    }

    @Override
    public GeneratedVariable newVariable(GeneratedType type, String name) {
        return new JavaGeneratedVariable(type, JavaGeneratorUtil.escapeString(name));
    }

    @Override
    public GeneratedMethod newMethod(GeneratedType type, String name) {
        return new JavaGeneratedMethod(type, name);
    }
}
