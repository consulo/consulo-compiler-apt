package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.expression.*;
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

        throw new UnsupportedOperationException("Unknown generator factory " + id);
    }

    String getId();

    GeneratedClass newClass(String packageName, String name);

    GeneratedVariable newVariable(GeneratedType type, String name);

    GeneratedMethodCallExpression newMethodCallExpression(GeneratedExpression qualifier,
                                                          String methodName,
                                                          List<GeneratedExpression> arguments);

    GeneratedConstantExpression newConstantExpression(Object value);

    GeneratedReferenceExpression newReferenceExpression(String reference);

    GeneratedClassReferenceExpression newClassReferenceExpression(GeneratedClassType type);
}
