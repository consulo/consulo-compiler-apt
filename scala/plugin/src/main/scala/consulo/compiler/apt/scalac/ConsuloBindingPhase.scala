package consulo.compiler.apt.scalac

import consulo.compiler.apt.shared.ConsuloClasses.consulo.component.bind as Bind
import consulo.compiler.apt.shared.ServicesFiles
import consulo.compiler.apt.shared.generation.GeneratedElementFactory
import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.dotc.core.Symbols.ClassSymbol
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.report
import dotty.tools.dotc.typer.ImportInfo.withRootImports
import dotty.tools.dotc.typer.TyperPhase
import dotty.tools.dotc.util.SourceFile

import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * Runs right after typer. For each annotated class generates `<Name>_Binding` Scala source,
 * typechecks it and attaches it to the compilation unit of the annotated class - so the binding goes through
 * all remaining phases (pickler, backend) in the same run, and incremental compilers (zinc) see it as product of
 * the original source file.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class ConsuloBindingPhase(options: ConsuloBindingOptions) extends PluginPhase:
  override val phaseName: String = ConsuloBindingPhase.Name

  override val description: String = "generate Consulo injecting bindings"

  override val runsAfter: Set[String] = Set(TyperPhase.name)

  override def runOn(units: List[CompilationUnit])(using Context): List[CompilationUnit] =
    val factory = GeneratedElementFactory.of("scala")
    val providers = mutable.Map[String, mutable.Set[String]]()

    for unit <- units if !unit.isJava do
      val bindings = collectClasses(unit.tpdTree).flatMap(BindingGenerator.generate(_, factory))

      val generatedTrees = bindings.flatMap { binding =>
        providers.getOrElseUpdate(binding.serviceInterface, mutable.Set()) += binding.qualifiedName

        typecheck(binding, unit)
      }

      if generatedTrees.nonEmpty then
        unit.tpdTree = unit.tpdTree match
          case pkg: PackageDef => cpy.PackageDef(pkg)(pkg.pid, pkg.stats ++ generatedTrees)
          case tree => Thicket(tree :: generatedTrees)

    // always process known services - stale entries must be dropped even if recompiled sources have no bindings
    providers.getOrElseUpdate(Bind.InjectingBinding, mutable.Set())
    writeServices(providers.view.mapValues(_.toSet).toMap)

    units

  private def collectClasses(tree: Tree)(using Context): List[ClassSymbol] =
    val result = mutable.ListBuffer[ClassSymbol]()

    def visit(tree: Tree): Unit = tree match
      case PackageDef(_, stats) => stats.foreach(visit)
      case typeDef @ TypeDef(_, template: Template) =>
        result += typeDef.symbol.asClass
        template.body.foreach(visit)
      case _ =>

    visit(tree)
    result.toList

  private def typecheck(binding: BindingGenerator.Binding, owner: CompilationUnit)(using Context): Option[Tree] =
    val text = Using.resource(new StringWriter()) { writer =>
      binding.generatedClass.write(writer)
      writer.toString
    }

    val relativePath = binding.qualifiedName.replace('.', '/') + ".scala"

    val sourceName = options.sourcesDir match
      case Some(dir) =>
        val file = dir.resolve(relativePath)
        Files.createDirectories(file.getParent)
        Files.writeString(file, text, StandardCharsets.UTF_8)
        file.toString
      case None =>
        relativePath

    // compilation unit info of original source - generated class symbols are associated with original source file,
    // so zinc tracks binding classes as products of it (and removes them together)
    val unit = new CompilationUnit(SourceFile.virtual(sourceName, text), owner.info) {}
    val errorsBefore = ctx.reporter.errorCount

    ctx.typer.lateEnterUnit(typeCheck = true)(doTypeCheck => doTypeCheck())(using ctx.fresh.setCompilationUnit(unit).withRootImports)

    if ctx.reporter.errorCount > errorsBefore then None
    else Some(unit.tpdTree)

  private def writeServices(providers: Map[String, Set[String]])(using Context): Unit =
    val classesDir = Option(ctx.settings.outputDir.value.jpath).filter(Files.isDirectory(_))

    options.resourcesDir.orElse(classesDir) match
      case None =>
        if providers.values.exists(_.nonEmpty) then
          report.error(
            s"Output is not a directory. Specify -P:${ConsuloBindingPlugin.Name}:resources:<dir> for writing META-INF/services"
          )
      case Some(dir) =>
        for (service, implementations) <- providers do
          writeServiceFile(dir.resolve(ServicesFiles.SERVICES_PATH).resolve(service), implementations, classesDir)

  /**
   * Merge with existing file - incremental compilation (zinc) does not recompile all sources.
   * Entries of bindings, which class files were removed (source deleted), are dropped.
   */
  private def writeServiceFile(file: Path, implementations: Set[String], classesDir: Option[Path]): Unit =
    val existing =
      if Files.exists(file) then Using.resource(Files.newInputStream(file))(ServicesFiles.readServiceFile(_).asScala.toSet)
      else Set.empty[String]

    def isAlive(name: String) =
      classesDir.forall(dir => Files.exists(dir.resolve(name.replace('.', '/') + ".class")))

    val all = existing.filter(isAlive) ++ implementations
    if all != existing then
      Files.createDirectories(file.getParent)
      Using.resource(Files.newOutputStream(file)) { out =>
        ServicesFiles.writeServiceFile(all.toList.sorted.asJava, out)
      }

object ConsuloBindingPhase:
  val Name = "consuloBinding"
