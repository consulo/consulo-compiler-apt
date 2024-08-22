package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.expression.*;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public interface GeneratedElementFactory {
    GeneratedClass newClass(String packageName, String name);

    GeneratedVariable newVariable(GeneratedType type, String name);

    GeneratedMethodCallExpression newMethodCallExpression(GeneratedExpression qualifier,
                                                          String methodName,
                                                          List<GeneratedExpression> arguments);

    GeneratedConstantExpression newConstantExpression(Object value);

    GeneratedReferenceExpression newReferenceExpression(String reference);

    GeneratedClassReferenceExpression newClassReferenceExpression(GeneratedClassType type);
}
