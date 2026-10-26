package consulo.compiler.apt.scalac

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}

import java.nio.file.{Path, Paths}

/**
 * Scala counterpart of `InjectingBindingProcessor` (javac) and `InjectingSymbolProcessor` (KSP).
 *
 * Usage: `-Xplugin:compiler.apt.scala.jar`, optional options:
 *  - `-P:consulo-binding:sources:<dir>` - dump generated binding sources into dir (for inspection only, they are compiled in-place)
 *  - `-P:consulo-binding:resources:<dir>` - where `META-INF/services` must be written (default: `-d` directory)
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class ConsuloBindingPlugin extends StandardPlugin:
  override val name: String = ConsuloBindingPlugin.Name

  override val description: String = "generates Consulo injecting bindings for @ServiceImpl/@ExtensionImpl/@TopicImpl/@ActionImpl"

  override val optionsHelp: Option[String] = Some(
    s"""  -P:${ConsuloBindingPlugin.Name}:sources:<dir>    dump generated binding sources into <dir>
       |  -P:${ConsuloBindingPlugin.Name}:resources:<dir>  write META-INF/services into <dir> (default: output directory)""".stripMargin
  )

  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    List(new ConsuloBindingPhase(ConsuloBindingOptions.parse(options)))

object ConsuloBindingPlugin:
  val Name = "consulo-binding"

case class ConsuloBindingOptions(sourcesDir: Option[Path], resourcesDir: Option[Path])

object ConsuloBindingOptions:
  private val SourcesPrefix = "sources:"
  private val ResourcesPrefix = "resources:"

  def parse(options: List[String]): ConsuloBindingOptions =
    def find(prefix: String): Option[Path] =
      options.collectFirst { case option if option.startsWith(prefix) => Paths.get(option.substring(prefix.length)) }

    ConsuloBindingOptions(find(SourcesPrefix), find(ResourcesPrefix))
