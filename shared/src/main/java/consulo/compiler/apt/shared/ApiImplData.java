package consulo.compiler.apt.shared;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public class ApiImplData {
    private static final Map<String, String> API_ANNOTATIONS = Map.of(
        ConsuloClasses.consulo.annotation.component.ServiceImpl, ConsuloClasses.consulo.annotation.component.ServiceAPI,
        ConsuloClasses.consulo.annotation.component.ExtensionImpl, ConsuloClasses.consulo.annotation.component.ExtensionAPI,
        ConsuloClasses.consulo.annotation.component.TopicImpl, ConsuloClasses.consulo.annotation.component.TopicAPI,
        ConsuloClasses.consulo.annotation.component.ActionImpl, ConsuloClasses.consulo.annotation.component.ActionAPI
    );

    public static String getApiAnnotation(String implAnnotation) {
        return API_ANNOTATIONS.get(implAnnotation);
    }

    public static Map<String, String> getApiAnnotations() {
        return API_ANNOTATIONS;
    }
}
