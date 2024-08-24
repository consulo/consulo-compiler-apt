package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.MethodSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class JavaGeneratedMethod extends BaseGeneratedMethod {
    public JavaGeneratedMethod(GeneratedType type, String name) {
        super(type, name);
    }

    public MethodSpec toMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(myName);
        builder.returns(JavaGeneratorUtil.toTypeName(myType));

        for (GeneratedModifier modifier : myModifiers) {
            builder.addModifiers(JavaGeneratorUtil.toModifier(modifier));
        }

        for (GeneratedVariable parameter : myParameters) {
            builder.addParameter(((JavaGeneratedVariable) parameter).toParameter());
        }

        if (myStatement != null) {
            builder.addCode(myStatement.accept(JavaGeneratedStatementVisitor.INSTANCE));
        }

        return builder.build();
    }
}
