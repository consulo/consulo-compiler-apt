package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.BaseGeneratedVariable;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratedVariable extends BaseGeneratedVariable {
    public ScalaGeneratedVariable(GeneratedType type, String name) {
        super(type, name);
    }

    public boolean isStatic() {
        return myModifiers.contains(GeneratedModifier.STATIC);
    }

    public String toParameter() {
        return ScalaGeneratorUtil.escapeName(myName) + ": " + ScalaGeneratorUtil.toTypeName(myType);
    }

    public String toField() {
        StringBuilder builder = new StringBuilder();
        builder.append(ScalaGeneratorUtil.toModifiers(myModifiers));
        builder.append("val ");
        builder.append(toParameter());
        builder.append(" = ");
        if (myInitializerExpression != null) {
            builder.append(myInitializerExpression.accept(ScalaGeneratedExpressionVisitor.INSTANCE));
        }
        else {
            builder.append("_root_.scala.compiletime.uninitialized");
        }
        return builder.toString();
    }
}
