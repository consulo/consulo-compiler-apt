package consulo.compiler.apt.shared.generation.impl.kotlin;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.FunSpec;
import consulo.compiler.apt.shared.generation.BaseGeneratedMethod;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class KotlinGeneratedMethod extends BaseGeneratedMethod {
    public KotlinGeneratedMethod(GeneratedType type, String name) {
        super(type, name);
    }

    public FunSpec toStaticFunc() {
        FunSpec.Builder builder = FunSpec.builder(myName);
        builder.returns(KotlinGeneratorUtil.toTypeName(myType));

        builder.addAnnotation(ClassName.bestGuess("kotlin.jvm.JvmStatic"));

        for (GeneratedVariable parameter : myParameters) {
            KotlinGeneratedVariable kotlinVar = (KotlinGeneratedVariable) parameter;

            builder.addParameter(kotlinVar.toParameter());
        }

        builder.addModifiers(myModifiers.stream()
            .filter(modifier -> modifier != GeneratedModifier.STATIC && modifier != GeneratedModifier.FINAL)
            .map(KotlinGeneratorUtil::toModifier)
            .toList());

        if (myStatement != null) {
            builder.addCode(myStatement.accept(KotlinGeneratedStatementVisitor.INSTANCE));
        }

        return builder.build();
    }
}
