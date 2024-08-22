package consulo.compiler.apt.shared.generation.impl.kotlin;

import com.squareup.kotlinpoet.FileSpec;
import com.squareup.kotlinpoet.TypeSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedVariable;

import java.io.Writer;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinGeneratedClass extends BaseGeneratedClass {
    public KotlinGeneratedClass(String packageName, String name) {
        super(packageName, name);
    }

    public FileSpec build() {
        FileSpec.Builder fileSpec = FileSpec.builder(myPackageName, myName);
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(myName);

        TypeSpec.Builder companion = TypeSpec.companionObjectBuilder();
        for (GeneratedVariable field : fields) {
            KotlinGeneratedVariable kotlinField = (KotlinGeneratedVariable) field;

            companion.addProperty(kotlinField.toJvmField());
        }

        classBuilder.addType(companion.build());
        fileSpec.addType(classBuilder.build());
        return fileSpec.build();
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
