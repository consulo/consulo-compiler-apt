package consulo.compiler.apt.shared.generation.impl.scala;

import consulo.compiler.apt.shared.generation.expression.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class ScalaGeneratedExpressionVisitor implements GeneratedExpressionVisitor<String> {
    public static final ScalaGeneratedExpressionVisitor INSTANCE = new ScalaGeneratedExpressionVisitor();

    @Override
    public String visitConstantExpression(GeneratedConstantExpression expression) {
        Object value = expression.value();
        if (value == null) {
            return "null";
        }

        if (value instanceof Integer || value instanceof Boolean || value instanceof Short || value instanceof Byte) {
            return String.valueOf(value);
        }

        if (value instanceof Long) {
            return value + "L";
        }

        if (value instanceof Float) {
            return value + "F";
        }

        if (value instanceof Double) {
            return value + "D";
        }

        if (value instanceof String str) {
            return ScalaGeneratorUtil.toStringLiteral(str);
        }

        throw new UnsupportedOperationException(String.valueOf(value));
    }

    @Override
    public String visitClassCastExpression(GeneratedClassCastExpression expression) {
        return asQualifier(expression.expression()) + ".asInstanceOf[" + ScalaGeneratorUtil.toTypeName(expression.type()) + "]";
    }

    @Override
    public String visitNewExpression(GeneratedNewExpression expression) {
        return "new " + ScalaGeneratorUtil.toTypeName(expression.type()) + "(" + joinArguments(expression.arguments()) + ")";
    }

    @Override
    public String visitArrayGetExpression(GeneratedArrayGetExpression expression) {
        return asQualifier(expression.expression()) + "(" + expression.in().accept(this) + ")";
    }

    @Override
    public String visitNewArrayExpression(GeneratedNewArrayExpression expression) {
        // element type is inferred from expected type (method return type)
        return "_root_.scala.Array(" + joinArguments(expression.expressions()) + ")";
    }

    @Override
    public String visitQualifiedExpression(GeneratedQualifiedExpression expression) {
        return asQualifier(expression.qualifier()) + "." + expression.expression().accept(this);
    }

    @Override
    public String visitReferenceExpression(GeneratedReferenceExpression expression) {
        return ScalaGeneratorUtil.escapeName(expression.reference());
    }

    @Override
    public String visitClassReferenceExpression(GeneratedClassReferenceExpression expression) {
        return ScalaGeneratorUtil.toClassName(expression.type());
    }

    @Override
    public String visitClassClassExpression(GeneratedClassClassExpression expression) {
        return "classOf[" + ScalaGeneratorUtil.toClassName(expression.type()) + "]";
    }

    @Override
    public String visitMethodCallExpression(GeneratedMethodCallExpression expression) {
        return asQualifier(expression.qualifier()) + "." + ScalaGeneratorUtil.escapeName(expression.methodName())
            + "(" + joinArguments(expression.arguments()) + ")";
    }

    private String joinArguments(List<GeneratedExpression> arguments) {
        return arguments.stream().map(it -> it.accept(this)).collect(Collectors.joining(", "));
    }

    private String asQualifier(GeneratedExpression expression) {
        String text = expression.accept(this);
        if (expression instanceof GeneratedNewExpression || expression instanceof GeneratedClassCastExpression) {
            return "(" + text + ")";
        }
        return text;
    }
}
