package test;

import consulo.compiler.apt.shared.generator.LocalizeGenerator;
import consulo.compiler.apt.shared.generation.GeneratedClass;
import consulo.compiler.apt.shared.generation.GeneratedElementFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class KotlinLocalizeTest {
    @Test
    public void testSomeLocalize() throws Exception {
        doTest("test/SomeLocalize.yaml");
    }

    private void doTest(String fileName) throws Exception {
        GeneratedElementFactory factory = GeneratedElementFactory.of("kotlin");

        LocalizeGenerator generator = new LocalizeGenerator(factory);

        URL resource = getClass().getResource("/" + fileName);

        Path target = Path.of(resource.toURI());

        GeneratedClass generatedClass = generator.parse(fileName, target);

        StringWriter stringWriter = new StringWriter();
        try (stringWriter) {
            generatedClass.write(stringWriter);
        }

        String value = stringWriter.toString();

        URL expectedResult = getClass().getResource("/" + fileName.replace(".yaml", ".kt"));

        Path expected = Path.of(expectedResult.toURI());

        String expectedText = Files.readString(expected);

        Assertions.assertEquals(normalizeEndings(expectedText), normalizeEndings(value));
    }

    private static String normalizeEndings(String str) {
        return str.replace("\r\n", "\n");
    }
}
