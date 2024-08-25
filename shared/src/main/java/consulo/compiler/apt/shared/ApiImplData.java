package consulo.compiler.apt.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-08-25
 */
public class ApiImplData {
    private static final Map<String, String> ourApiAnnotations = new HashMap<>();

    static {
        ourApiAnnotations.put(ConsuloClasses.consulo.annotation.component.ServiceImpl, ConsuloClasses.consulo.annotation.component.ServiceAPI);
        ourApiAnnotations.put(ConsuloClasses.consulo.annotation.component.ExtensionImpl, ConsuloClasses.consulo.annotation.component.ExtensionAPI);
        ourApiAnnotations.put(ConsuloClasses.consulo.annotation.component.TopicImpl, ConsuloClasses.consulo.annotation.component.TopicAPI);
        ourApiAnnotations.put(ConsuloClasses.consulo.annotation.component.ActionImpl, ConsuloClasses.consulo.annotation.component.ActionAPI);
    }

    public static String getApiAnnotation(String implAnnotation) {
        return ourApiAnnotations.get(implAnnotation);
    }

    public static Map<String, String> getApiAnnotations() {
        return ourApiAnnotations;
    }
}
