package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import consulo.compiler.apt.shared.generation.expression.GeneratedClassReferenceExpression;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public record JavaGeneratedClassReferenceExpression(GeneratedClassType type) implements GeneratedClassReferenceExpression,
    JavaGeneratedExpression {

    @Override
    public CodeBlock generate() {
        ClassName className = ClassName.bestGuess(type().className());
        return CodeBlock.of("$T", className);
    }
}
