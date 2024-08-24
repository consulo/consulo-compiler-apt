package consulo.compiler.apt.shared.generation;

import consulo.compiler.apt.shared.generation.type.GeneratedType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-21
 */
public abstract class BaseGeneratedClass implements GeneratedClass {
    protected final String myPackageName;
    protected final String myName;
    protected final List<GeneratedVariable> myFields = new ArrayList<>();
    protected final List<GeneratedMethod> myMethods = new ArrayList<>();
    protected final List<GeneratedType> mySuperInterfaces = new ArrayList<>();

    public BaseGeneratedClass(String packageName, String name) {
        myPackageName = packageName;
        myName = name;
    }

    @Override
    public void withSuperInterfaces(List<? extends GeneratedType> superInterfaces) {
        mySuperInterfaces.addAll(superInterfaces);
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
        myFields.addAll(fields);
    }

    @Override
    public void withMethods(List<? extends GeneratedMethod> methods) {
        myMethods.addAll(methods);
    }
}
