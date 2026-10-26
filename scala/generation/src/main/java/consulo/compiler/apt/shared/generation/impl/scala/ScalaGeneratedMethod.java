package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.BaseGeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratedMethod extends BaseGeneratedMethod {
    public ScalaGeneratedMethod(GeneratedType type, String name) {
        super(type, name);
    }

    public boolean isStatic() {
        return myModifiers.contains(GeneratedModifier.STATIC);
    }

    public String toDef() {
        StringBuilder builder = new StringBuilder();
        if (withOverride) {
            builder.append("override ");
        }
        builder.append(ScalaGeneratorUtil.toModifiers(myModifiers));
        builder.append("def ");
        builder.append(ScalaGeneratorUtil.escapeName(myName));
        builder.append("(");
        builder.append(myParameters.stream()
            .map(it -> ((ScalaGeneratedVariable) it).toParameter())
            .collect(Collectors.joining(", ")));
        builder.append("): ");
        builder.append(ScalaGeneratorUtil.toTypeName(myType));
        builder.append(" = ");
        if (myStatement != null) {
            builder.append(myStatement.accept(ScalaGeneratedStatementVisitor.INSTANCE));
        }
        else {
            builder.append("???");
        }
        return builder.toString();
    }
}
