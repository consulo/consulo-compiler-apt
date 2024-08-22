package consulo.compiler.apt.shared.generation.impl.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import consulo.compiler.apt.shared.generation.GeneratedModifier;
import consulo.compiler.apt.shared.generation.type.GeneratedClassType;
import consulo.compiler.apt.shared.generation.type.GeneratedType;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

/**
 * @author VISTALL
 * @since 2024-08-22
 */
public class JavaGeneratorUtil {
    public static String escapeString(String name) {
        if (!SourceVersion.isName(name)) {
            return "_" + name;
        }

        return name;
    }

    public static TypeName toTypeName(GeneratedType generatedType) {
        if (generatedType instanceof GeneratedClassType classType) {
            return ClassName.bestGuess(classType.className());
        }

        throw new IllegalArgumentException(generatedType.toString());
    }

    public static Modifier toModifier(GeneratedModifier modifier) {
        switch (modifier) {
            case PUBLIC:
                return Modifier.PUBLIC;
            case PRIVATE:
                return Modifier.PRIVATE;
            case STATIC:
                return Modifier.STATIC;
            case FINAL:
                return Modifier.FINAL;
            default:
                throw new IllegalArgumentException(modifier.name());
        }
    }
}
