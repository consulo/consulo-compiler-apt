package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedVariable;

import javax.lang.model.element.Modifier;
import java.io.Writer;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-21
 */
public class JavaGeneratedClass extends BaseGeneratedClass {
    public JavaGeneratedClass(String packageName, String name) {
        super(packageName, name);
    }

    private JavaFile build() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(myName);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (GeneratedVariable field : fields) {
            builder.addField(((JavaGeneratedVariable) field).toField());
        }

        return JavaFile.builder(myPackageName, builder.build()).build();
    }

    @Override
    public void write(Path sourceDir) throws Exception {
        build().writeTo(sourceDir);
    }

    @Override
    public void write(Writer writer) throws Exception {
        build().writeTo(writer);
    }
}
