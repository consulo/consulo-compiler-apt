package consulo.compiler.apt.shared.generator;

import consulo.compiler.apt.shared.GenerationException;
import consulo.compiler.apt.shared.NameUtil;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class IconGroupGenerator {
    public record Icon(int width, int height) {
    }

    private final GeneratedElementFactory myFactory;

    public IconGroupGenerator(GeneratedElementFactory factory) {
        myFactory = factory;
    }

    public GeneratedClass parse(String relativePath, Path file) throws GenerationException {
        String pathWithoutExpresion = NameUtil.getNameWithoutExtension(relativePath).replace("/", ".");

        String pluginId = NameUtil.getPackageName(pathWithoutExpresion);
        String localizeId = NameUtil.getShortName(pathWithoutExpresion);

        String packageName = pluginId + ".icon";

        GeneratedClass generatedClass = myFactory.newClass(packageName, localizeId);
//        generatedClass.withFields(fields);
//        generatedClass.withMethods(methods);
        return generatedClass;
    }
}
