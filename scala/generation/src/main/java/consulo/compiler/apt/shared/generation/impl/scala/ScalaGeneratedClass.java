package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.BaseGeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Static members are placed into companion object and marked by {@code @static}, which makes them real JVM static members
 * of the class (same as {@code @JvmField}/{@code @JvmStatic} in Kotlin).
 * Inner classes are placed into companion object too - they are compiled as static nested classes ({@code Outer$Inner}).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratedClass extends BaseGeneratedClass {
    private static final String INDENT = "  ";

    public ScalaGeneratedClass(String packageName, String name) {
        super(packageName, name);
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        if (!myPackageName.isEmpty()) {
            builder.append("package ").append(myPackageName).append("\n\n");
        }

        builder.append("/**\n");
        builder.append(" * Generated code. Don't edit this class\n");
        builder.append(" */\n");
        builder.append(ScalaGeneratorUtil.NOWARN_ANNOTATION).append("\n");
        builder.append(buildDeclaration());
        return builder.toString();
    }

    /**
     * @return class declaration with companion object (if required), without package
     */
    private String buildDeclaration() {
        List<String> instanceMembers = new ArrayList<>();
        List<String> staticMembers = new ArrayList<>();

        for (GeneratedVariable field : myFields) {
            ScalaGeneratedVariable scalaField = (ScalaGeneratedVariable) field;
            if (scalaField.isStatic()) {
                staticMembers.add(ScalaGeneratorUtil.STATIC_ANNOTATION + "\n" + scalaField.toField());
            }
            else {
                instanceMembers.add(scalaField.toField());
            }
        }

        for (GeneratedMethod method : myMethods) {
            ScalaGeneratedMethod scalaMethod = (ScalaGeneratedMethod) method;
            if (scalaMethod.isStatic()) {
                staticMembers.add(ScalaGeneratorUtil.STATIC_ANNOTATION + "\n" + scalaMethod.toDef());
            }
            else {
                instanceMembers.add(scalaMethod.toDef());
            }
        }

        // @static members must precede other members of object
        for (GeneratedClass innerClass : myInnerClasses) {
            staticMembers.add(((ScalaGeneratedClass) innerClass).buildDeclaration());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("final class ").append(ScalaGeneratorUtil.escapeName(myName));

        for (int i = 0; i < mySuperInterfaces.size(); i++) {
            GeneratedType superInterface = mySuperInterfaces.get(i);
            builder.append(i == 0 ? " extends " : " with ");
            builder.append(ScalaGeneratorUtil.toTypeName(superInterface));
        }

        appendBody(builder, instanceMembers);

        if (!staticMembers.isEmpty()) {
            builder.append("\n\n");
            builder.append(ScalaGeneratorUtil.NOWARN_ANNOTATION).append("\n");
            builder.append("object ").append(ScalaGeneratorUtil.escapeName(myName));
            appendBody(builder, staticMembers);
        }
        builder.append("\n");
        return builder.toString();
    }

    private static void appendBody(StringBuilder builder, List<String> members) {
        if (members.isEmpty()) {
            return;
        }

        builder.append(" {\n");
        for (int i = 0; i < members.size(); i++) {
            if (i != 0) {
                builder.append("\n");
            }
            builder.append(indent(members.get(i).stripTrailing())).append("\n");
        }
        builder.append("}");
    }

    private static String indent(String text) {
        return INDENT + text.replace("\n", "\n" + INDENT);
    }

    @Override
    public void write(Path sourceDir) throws Exception {
        Path dir = myPackageName.isEmpty() ? sourceDir : sourceDir.resolve(myPackageName.replace('.', '/'));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(myName + ".scala"), build(), StandardCharsets.UTF_8);
    }

    @Override
    public void write(Writer writer) throws Exception {
        writer.write(build());
    }
}
