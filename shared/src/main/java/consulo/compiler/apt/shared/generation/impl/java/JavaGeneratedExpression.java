package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.GeneratedExpression;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public interface JavaGeneratedExpression extends GeneratedExpression {
    CodeBlock generate();
}
