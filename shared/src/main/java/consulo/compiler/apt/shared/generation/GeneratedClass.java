package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-21
 */
public interface GeneratedClass {
    String getPackageName();

    String getName();

    void withSuperInterfaces(List<? extends GeneratedType> superInterfaces);

    void withFields(List<? extends GeneratedVariable> fields);

    void withMethods(List<? extends GeneratedMethod> methods);

    /**
     * Inner classes are generated as static nested classes. Package name of inner class is ignored
     */
    void withInnerClasses(List<? extends GeneratedClass> innerClasses);

    void write(Path sourceDir) throws Exception;

    void write(Writer writer) throws Exception;
}
