package consulo.compiler.apt.shared.generation.expression;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public interface GeneratedExpressionVisitor<R> {
    default R visitReferenceExpression(GeneratedReferenceExpression expression) {
        throw new UnsupportedOperationException();
    }

    default R visitMethodCallExpression(GeneratedMethodCallExpression expression) {
        throw new UnsupportedOperationException();
    }

    default R visitClassReferenceExpression(GeneratedClassReferenceExpression expression) {
        throw new UnsupportedOperationException();
    }

    default R visitClassClassExpression(GeneratedClassClassExpression expression) {
        throw new UnsupportedOperationException();
    }

    default R visitConstantExpression(GeneratedConstantExpression expression) {
        throw new UnsupportedOperationException();
    }

    default R visitQualifiedExpression(GeneratedQualifiedExpression expression) {
        throw new UnsupportedOperationException();
    }

    default R visitNewArrayExpression(GeneratedNewArrayExpression expression) {
        throw new UnsupportedOperationException();
    }
}
