package consulo.compiler.apt.shared;

/**
 * @author VISTALL
 * @since 2024-08-20
 */
public interface ConsuloClasses {
    interface jakarta {
        interface inject {
            String Inject = "jakarta.inject.Inject";
        }
    }

    interface consulo {
        interface component {
            interface bind {
                String InjectingBinding = "consulo.component.bind.InjectingBinding";
                String TopicBinding = "consulo.component.bind.TopicBinding";
            }
        }

        interface annotation {
            interface component {
                String ComponentScope = "consulo.annotation.component.ComponentScope";

                String ServiceImpl = "consulo.annotation.component.ServiceImpl";
                String ExtensionImpl = "consulo.annotation.component.ExtensionImpl";
                String TopicImpl = "consulo.annotation.component.TopicImpl";
                String ActionImpl = "consulo.annotation.component.ActionImpl";

                String ActionAPI = "consulo.annotation.component.ActionAPI";
                String TopicAPI = "consulo.annotation.component.TopicAPI";
                String ExtensionAPI = "consulo.annotation.component.ExtensionAPI";
                String ServiceAPI = "consulo.annotation.component.ServiceAPI";
            }
        }
    }
}
