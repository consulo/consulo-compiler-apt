package test;

import consulo.compiler.apt.shared.LocalizeGenerator;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;

import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinLocalizeTest {
    public static void main(String[] args) throws Exception {
        GeneratedElementFactory factory = GeneratedElementFactory.of("kotlin");

        LocalizeGenerator generator = new LocalizeGenerator(factory);

        URL resource = KotlinLocalizeTest.class.getResource("/test/SomeLocalize.yaml");

        Path path = Path.of(resource.toURI());

        GeneratedClass generatedClass = generator.parse("test/SomeLocalize.yaml", path);

        StringWriter stringWriter = new StringWriter();
        try (stringWriter) {
            generatedClass.write(stringWriter);
        }

        String value = stringWriter.toString();

        System.out.println();
    }
}
