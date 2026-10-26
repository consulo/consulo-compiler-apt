package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

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
        return JavaFile.builder(myPackageName, buildType(false)).build();
    }

    private TypeSpec buildType(boolean inner) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(myName);
        if (!inner) {
            builder.addJavadoc("Generated code. Don't edit this class");
        }
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (inner) {
            builder.addModifiers(Modifier.STATIC);
        }

        for (GeneratedType superInterface : mySuperInterfaces) {
            builder.addSuperinterface(JavaGeneratorUtil.toTypeName(superInterface));
        }

        for (GeneratedVariable field : myFields) {
            builder.addField(((JavaGeneratedVariable) field).toField());
        }

        for (GeneratedMethod method : myMethods) {
            builder.addMethod(((JavaGeneratedMethod) method).toMethod());
        }

        for (GeneratedClass innerClass : myInnerClasses) {
            builder.addType(((JavaGeneratedClass) innerClass).buildType(true));
        }

        return builder.build();
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
