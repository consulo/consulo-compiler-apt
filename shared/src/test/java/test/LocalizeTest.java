package test;

import consulo.compiler.apt.shared.LocalizeGenerator;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.impl.JavaGeneratedElementFactory;

import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class LocalizeTest {
    public static void main(String[] args) throws Exception {
        JavaGeneratedElementFactory factory = new JavaGeneratedElementFactory();

        LocalizeGenerator generator = new LocalizeGenerator(factory);

        URL resource = LocalizeTest.class.getResource("/test/SomeLocalize.yaml");

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
