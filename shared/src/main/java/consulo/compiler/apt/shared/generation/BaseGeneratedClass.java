package consulo.compiler.apt.shared.generation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-21
 */
public abstract class BaseGeneratedClass implements GeneratedClass {
    protected final String myPackageName;
    protected final String myName;
    protected List<GeneratedVariable> fields = new ArrayList<>();

    public BaseGeneratedClass(String packageName, String name) {
        myPackageName = packageName;
        myName = name;
    }

    @Override
    public String getPackageName() {
        return myPackageName;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public void withFields(List<? extends GeneratedVariable> fields) {
        this.fields.addAll(fields);
    }
}
