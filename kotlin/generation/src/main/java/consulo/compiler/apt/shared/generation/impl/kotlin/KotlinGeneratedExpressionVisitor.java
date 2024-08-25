package consulo.compiler.apt.shared.generation.impl.kotlin;

import com.squareup.kotlinpoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class KotlinGeneratedExpressionVisitor implements GeneratedExpressionVisitor<CodeBlock> {
    public static final KotlinGeneratedExpressionVisitor INSTANCE = new KotlinGeneratedExpressionVisitor();

    @Override
    public CodeBlock visitConstantExpression(GeneratedConstantExpression expression) {
        Object value = expression.value();
        if (value instanceof Integer) {
            return CodeBlock.of("%L", value);
        }

        if (value instanceof String) {
            return CodeBlock.of("%S", value);
        }

        throw new UnsupportedOperationException(String.valueOf(value));
    }

    @Override
    public CodeBlock visitNewArrayExpression(GeneratedNewArrayExpression expression) {
        List<GeneratedExpression> expressions = expression.expressions();
        List<Object> arguments = new ArrayList<>(expressions.size());
        StringBuilder builder = new StringBuilder();
        builder.append("arrayOf(");
        for (int i = 0; i < expression.expressions().size(); i++) {
            if (i != 0) {
                builder.append(", ");
            }

            builder.append("%L");
            arguments.add(expressions.get(i).accept(this));
        }
        builder.append(")");
        return CodeBlock.of(builder.toString(), arguments.toArray());
    }

    @Override
    public CodeBlock visitQualifiedExpression(GeneratedQualifiedExpression expression) {
        return CodeBlock.of("%L.%L", expression.qualifier().accept(this), expression.expression().accept(this));
    }

    @Override
    public CodeBlock visitReferenceExpression(GeneratedReferenceExpression expression) {
        return CodeBlock.of("%L", expression.reference());
    }

    @Override
    public CodeBlock visitClassReferenceExpression(GeneratedClassReferenceExpression expression) {
        return CodeBlock.of("%T", KotlinGeneratorUtil.toTypeName(expression.type()));
    }

    @Override
    public CodeBlock visitClassClassExpression(GeneratedClassClassExpression expression) {
        return CodeBlock.of("%T::class.java", KotlinGeneratorUtil.toTypeName(expression.type()));
    }

    @Override
    public CodeBlock visitMethodCallExpression(GeneratedMethodCallExpression expression) {
        List<Object> args = new ArrayList<>();
        args.add((expression.qualifier().accept(this)));

        StringBuilder builder = new StringBuilder();
        builder.append("%L.");
        builder.append(expression.methodName());
        builder.append("(");
        for (int i = 0; i < expression.arguments().size(); i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append("%L");

            args.add(expression.arguments().get(i).accept(this));
        }
        builder.append(")");
        return CodeBlock.of(builder.toString(), args.toArray());
    }
}
