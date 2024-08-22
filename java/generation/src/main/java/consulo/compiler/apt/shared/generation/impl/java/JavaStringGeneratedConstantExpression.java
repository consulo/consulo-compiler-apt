package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.GeneratedConstantExpression;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record JavaStringGeneratedConstantExpression(String value) implements JavaGeneratedExpression, GeneratedConstantExpression {
    @Override
    public CodeBlock generate() {
        return CodeBlock.of("$S", value());
    }
}
