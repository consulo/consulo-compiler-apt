package consulo.compiler.apt.shared.generation.impl.java;

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
public class JavaGeneratedElementFactory implements GeneratedElementFactory {

    @Override
    public String getId() {
        return "java";
    }

    @Override
    public GeneratedClass newClass(String packageName, String name) {
        return new JavaGeneratedClass(packageName, name);
    }

    @Override
    public GeneratedVariable newVariable(GeneratedType type, String name) {
        return new JavaGeneratedVariable(type, JavaGeneratorUtil.escapeString(name));
    }

    @Override
    public GeneratedMethodCallExpression newMethodCallExpression(GeneratedExpression qualifier, String methodName, List<GeneratedExpression> arguments) {
        return new JavaGeneratedMethodCallExpression(qualifier, methodName, arguments);
    }

    @Override
    public GeneratedConstantExpression newConstantExpression(Object value) {
        if (value instanceof String strValue) {
            return new JavaStringGeneratedConstantExpression(strValue);
        }

        if (value instanceof Integer intValue) {
            return new JavaIntegerGeneratedConstantExpression(intValue);
        }

        throw new IllegalArgumentException(String.valueOf(value));
    }

    @Override
    public GeneratedReferenceExpression newReferenceExpression(String reference) {
        return new JavaGeneratedReferenceExpression(reference);
    }

    @Override
    public GeneratedClassReferenceExpression newClassReferenceExpression(GeneratedClassType type) {
        return new JavaGeneratedClassReferenceExpression(type);
    }
}
