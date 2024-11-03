package test.localize;

import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nullable;
import java.lang.Object;
import java.lang.String;

/**
 * Generated code. Don't edit this class
 */
public final class SomeLocalize {
  public static final String ID = "test.SomeLocalize";

  private static final LocalizeKey xdebugger_colors_page_name = LocalizeKey.of(ID, "xdebugger.colors.page.name", 0);

  private static final LocalizeKey debugger_configurable_display_name = LocalizeKey.of(ID, "debugger.configurable.display.name", 0);

  private static final LocalizeKey debugger_dataviews_display_name = LocalizeKey.of(ID, "debugger.dataviews.display.name", 0);

  private static final LocalizeKey debugger_stepping_display_name = LocalizeKey.of(ID, "debugger.stepping.display.name", 0);

  private static final LocalizeKey debugger_hotswap_display_name = LocalizeKey.of(ID, "debugger.hotswap.display.name", 0);

  private static final LocalizeKey xdebugger_default_content_title = LocalizeKey.of(ID, "xdebugger.default.content.title", 0);

  private static final LocalizeKey xdebugger_debugger_tab_title = LocalizeKey.of(ID, "xdebugger.debugger.tab.title", 0);

  private static final LocalizeKey xdebugger_attach_popup_title = LocalizeKey.of(ID, "xdebugger.attach.popup.title", 1);

  private static final LocalizeKey xdebugger_attach_host_popup_title = LocalizeKey.of(ID, "xdebugger.attach.host.popup.title", 2);

  private static final LocalizeKey xdebugger_attach_tolocal_popup_selectdebugger_title = LocalizeKey.of(ID, "xdebugger.attach.tolocal.popup.selectdebugger.title", 0);

  private static final LocalizeKey xdebugger_attach_tolocal_popup_recent = LocalizeKey.of(ID, "xdebugger.attach.tolocal.popup.recent", 0);

  public static LocalizeValue xdebuggerColorsPageName() {
    return xdebugger_colors_page_name.getValue();
  }

  public static LocalizeValue debuggerConfigurableDisplayName() {
    return debugger_configurable_display_name.getValue();
  }

  public static LocalizeValue debuggerDataviewsDisplayName() {
    return debugger_dataviews_display_name.getValue();
  }

  public static LocalizeValue debuggerSteppingDisplayName() {
    return debugger_stepping_display_name.getValue();
  }

  public static LocalizeValue debuggerHotswapDisplayName() {
    return debugger_hotswap_display_name.getValue();
  }

  public static LocalizeValue xdebuggerDefaultContentTitle() {
    return xdebugger_default_content_title.getValue();
  }

  public static LocalizeValue xdebuggerDebuggerTabTitle() {
    return xdebugger_debugger_tab_title.getValue();
  }

  public static LocalizeValue xdebuggerAttachPopupTitle(Object arg0) {
    return xdebugger_attach_popup_title.getValue(arg0);
  }

  public static LocalizeValue xdebuggerAttachHostPopupTitle(int some, @Nullable String aaa) {
    return xdebugger_attach_host_popup_title.getValue(some, aaa);
  }

  public static LocalizeValue xdebuggerAttachTolocalPopupSelectdebuggerTitle() {
    return xdebugger_attach_tolocal_popup_selectdebugger_title.getValue();
  }

  public static LocalizeValue xdebuggerAttachTolocalPopupRecent() {
    return xdebugger_attach_tolocal_popup_recent.getValue();
  }
}
