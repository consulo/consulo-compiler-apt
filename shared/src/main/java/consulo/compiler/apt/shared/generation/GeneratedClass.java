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

    void write(Path sourceDir) throws Exception;

    void write(Writer writer) throws Exception;
}
