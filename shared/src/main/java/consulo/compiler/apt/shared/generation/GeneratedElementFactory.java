package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.expression.*;
import consulo.compiler.apt.shared.generation.statement.GeneratedReturnStatement;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public interface GeneratedElementFactory {
    static GeneratedElementFactory of(String id) {
        for (GeneratedElementFactory factory : ServiceLoader.load(GeneratedElementFactory.class)) {
            if (Objects.equals(factory.getId(), id)) {
                return factory;
            }
        }

        throw new UnsupportedOperationException("Unknown generator factory for '" + id + "'");
    }

    String getId();

    GeneratedClass newClass(String packageName, String name);

    GeneratedVariable newVariable(GeneratedType type, String name);

    GeneratedMethod newMethod(GeneratedType type, String name);

    default GeneratedMethodCallExpression newMethodCallExpression(GeneratedExpression qualifier,
                                                                  String methodName,
                                                                  List<GeneratedExpression> arguments) {
        return new GeneratedMethodCallExpression(qualifier, methodName, arguments);
    }

    default GeneratedConstantExpression newConstantExpression(Object value) {
        return new GeneratedConstantExpression(value);
    }

    default GeneratedReferenceExpression newReferenceExpression(String reference) {
        return new GeneratedReferenceExpression(reference);
    }

    default GeneratedClassReferenceExpression newClassReferenceExpression(GeneratedClassType type) {
        return new GeneratedClassReferenceExpression(type);
    }

    default GeneratedReturnStatement newReturnStatement(GeneratedExpression expression) {
        return new GeneratedReturnStatement(expression);
    }
}
