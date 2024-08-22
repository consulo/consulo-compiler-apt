package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;
import consulo.compiler.apt.shared.generation.expression.GeneratedMethodCallExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record JavaGeneratedMethodCallExpression(GeneratedExpression qualifier,
                                                String methodName,
                                                List<GeneratedExpression> arguments) implements GeneratedMethodCallExpression, JavaGeneratedExpression {
    @Override
    public CodeBlock generate() {
        List<Object> args = new ArrayList<>();
        args.add(((JavaGeneratedExpression) qualifier()).generate());

        StringBuilder builder = new StringBuilder();
        builder.append("$L.");
        builder.append(methodName());
        builder.append("(");
        for (int i = 0; i < arguments().size(); i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append("$L");

            args.add(((JavaGeneratedExpression) arguments().get(i)).generate());
        }
        builder.append(")");
        return CodeBlock.of(builder.toString(), args.toArray());
    }
}
