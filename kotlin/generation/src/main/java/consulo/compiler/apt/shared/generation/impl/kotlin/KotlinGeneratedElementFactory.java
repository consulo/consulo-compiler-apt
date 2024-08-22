package consulo.compiler.apt.shared.generation.impl.kotlin;

import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import consulo.compiler.apt.shared.generation.GeneratedVariable;
import consulo.compiler.apt.shared.generation.expression.*;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinGeneratedElementFactory implements GeneratedElementFactory {
    @Override
    public String getId() {
        return "kotlin";
    }

    @Override
    public GeneratedClass newClass(String packageName, String name) {
        return new KotlinGeneratedClass(packageName, name);
    }

    @Override
    public GeneratedVariable newVariable(GeneratedType type, String name) {
        return new KotlinGeneratedVariable(type, name);
    }

    @Override
    public GeneratedMethodCallExpression newMethodCallExpression(GeneratedExpression qualifier, String methodName, List<GeneratedExpression> arguments) {
        return null;
    }

    @Override
    public GeneratedConstantExpression newConstantExpression(Object value) {
        return null;
    }

    @Override
    public GeneratedReferenceExpression newReferenceExpression(String reference) {
        return null;
    }

    @Override
    public GeneratedClassReferenceExpression newClassReferenceExpression(GeneratedClassType type) {
        return null;
    }
}
