package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.GeneratedReferenceExpression;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record JavaGeneratedReferenceExpression(String reference) implements JavaGeneratedExpression, GeneratedReferenceExpression {
    @Override
    public CodeBlock generate() {
        return CodeBlock.of("$L", reference());
    }
}
