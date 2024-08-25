package consulo.compiler.apt.kotlin;

import com.google.devtools.ksp.processing.SymbolProcessor;
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment;
import com.google.devtools.ksp.processing.SymbolProcessorProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 2024-08-24
 */
public class InjectingSymbolProcessorProvider implements SymbolProcessorProvider {
    @NotNull
    @Override
    public SymbolProcessor create(@NotNull SymbolProcessorEnvironment env) {
        return new InjectingSymbolProcessor(env.getCodeGenerator(), env.getLogger());
    }
}
