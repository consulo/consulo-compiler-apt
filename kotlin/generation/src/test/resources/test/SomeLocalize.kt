@file:Suppress("warnings")

package test.localize

import consulo.localize.LocalizeKey
import consulo.localize.LocalizeValue
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Generated code. Don't edit this class
 */
public class SomeLocalize {
  public companion object {
    @JvmField
    public val ID: String = "test.SomeLocalize"

    private val xdebugger_colors_page_name: LocalizeKey = LocalizeKey.of(ID,
        "xdebugger.colors.page.name", 0)

    private val debugger_configurable_display_name: LocalizeKey = LocalizeKey.of(ID,
        "debugger.configurable.display.name", 0)

    private val debugger_dataviews_display_name: LocalizeKey = LocalizeKey.of(ID,
        "debugger.dataviews.display.name", 0)

    private val debugger_stepping_display_name: LocalizeKey = LocalizeKey.of(ID,
        "debugger.stepping.display.name", 0)

    private val debugger_hotswap_display_name: LocalizeKey = LocalizeKey.of(ID,
        "debugger.hotswap.display.name", 0)

    private val xdebugger_default_content_title: LocalizeKey = LocalizeKey.of(ID,
        "xdebugger.default.content.title", 0)

    private val xdebugger_debugger_tab_title: LocalizeKey = LocalizeKey.of(ID,
        "xdebugger.debugger.tab.title", 0)

    private val xdebugger_attach_popup_title: LocalizeKey = LocalizeKey.of(ID,
        "xdebugger.attach.popup.title", 1)

    private val xdebugger_attach_host_popup_title: LocalizeKey = LocalizeKey.of(ID,
        "xdebugger.attach.host.popup.title", 2)

    private val xdebugger_attach_tolocal_popup_selectdebugger_title: LocalizeKey =
        LocalizeKey.of(ID, "xdebugger.attach.tolocal.popup.selectdebugger.title", 0)

    private val xdebugger_attach_tolocal_popup_recent: LocalizeKey = LocalizeKey.of(ID,
        "xdebugger.attach.tolocal.popup.recent", 0)

    @JvmStatic
    public fun xdebuggerColorsPageName(): LocalizeValue = xdebugger_colors_page_name.getValue()

    @JvmStatic
    public fun debuggerConfigurableDisplayName(): LocalizeValue =
        debugger_configurable_display_name.getValue()

    @JvmStatic
    public fun debuggerDataviewsDisplayName(): LocalizeValue =
        debugger_dataviews_display_name.getValue()

    @JvmStatic
    public fun debuggerSteppingDisplayName(): LocalizeValue =
        debugger_stepping_display_name.getValue()

    @JvmStatic
    public fun debuggerHotswapDisplayName(): LocalizeValue =
        debugger_hotswap_display_name.getValue()

    @JvmStatic
    public fun xdebuggerDefaultContentTitle(): LocalizeValue =
        xdebugger_default_content_title.getValue()

    @JvmStatic
    public fun xdebuggerDebuggerTabTitle(): LocalizeValue = xdebugger_debugger_tab_title.getValue()

    @JvmStatic
    public fun xdebuggerAttachPopupTitle(arg0: Any?): LocalizeValue =
        xdebugger_attach_popup_title.getValue(arg0)

    @JvmStatic
    public fun xdebuggerAttachHostPopupTitle(some: Int, aaa: String?): LocalizeValue =
        xdebugger_attach_host_popup_title.getValue(some, aaa)

    @JvmStatic
    public fun xdebuggerAttachTolocalPopupSelectdebuggerTitle(): LocalizeValue =
        xdebugger_attach_tolocal_popup_selectdebugger_title.getValue()

    @JvmStatic
    public fun xdebuggerAttachTolocalPopupRecent(): LocalizeValue =
        xdebugger_attach_tolocal_popup_recent.getValue()
  }
}
