package consulo.compiler.apt.scalac

import consulo.annotation.component.{ComponentProfiles, ComponentScope, ExtensionAPI, ExtensionImpl}
import consulo.component.bind.{InjectingBinding, TopicBinding}
import dotty.tools.dotc.Main
import dotty.tools.dotc.interfaces.{Diagnostic, SimpleReporter}
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.io.File
import java.lang.reflect.ParameterizedType
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import javax.tools.ToolProvider
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/**
 * @author VISTALL
 * @since 2026-10-24
 */
class ConsuloBindingPluginTest:
  @TempDir
  var tempDir: Path = null

  private val ApiSources = Map(
    "test/api/MyExtension.java" ->
      """package test.api;
        |
        |import consulo.annotation.component.*;
        |
        |@ExtensionAPI(ComponentScope.PROJECT)
        |public interface MyExtension {
        |}
        |""".stripMargin,
    "test/api/MyService.java" ->
      """package test.api;
        |
        |import consulo.annotation.component.*;
        |
        |@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
        |public interface MyService {
        |}
        |""".stripMargin
  )

  @Test
  def testBindings(): Unit =
    val result = compile(Map(
      "test/impl/Impls.scala" ->
        """package test.impl
          |
          |import consulo.annotation.component.*
          |import jakarta.inject.Inject
          |
          |@ExtensionAPI(ComponentScope.MODULE)
          |trait ScalaExtension
          |
          |@ExtensionImpl(profiles = ComponentProfiles.PRODUCTION)
          |class MyExtensionImpl @Inject() (val service: test.api.MyService, val names: java.util.List[String], val count: Int)
          |  extends test.api.MyExtension
          |
          |@ServiceImpl
          |class MyServiceImpl extends test.api.MyService
          |
          |@ExtensionImpl
          |class ScalaExtensionImpl extends ScalaExtension
          |""".stripMargin
    ))

    assertEquals(Nil, result.errors)

    val services = Files.readAllLines(result.classes.resolve("META-INF/services/" + classOf[InjectingBinding].getName)).asScala.toList
    assertEquals(
      List(
        "test.impl.MyExtensionImpl_Binding",
        "test.impl.MyServiceImpl_Binding",
        "test.impl.ScalaExtensionImpl_Binding"
      ),
      services.filter(_.nonEmpty).sorted
    )

    val sourcesDir = tempDir.resolve("generated")
    assertTrue(Files.exists(sourcesDir.resolve("test/impl/MyExtensionImpl_Binding.scala")))

    val loader = new URLClassLoader(Array(result.classes.toUri.toURL), getClass.getClassLoader)

    def binding(name: String) =
      loader.loadClass(name).getConstructor().newInstance().asInstanceOf[InjectingBinding]

    val extension = binding("test.impl.MyExtensionImpl_Binding")
    assertEquals("test.api.MyExtension", extension.getApiClassName)
    assertEquals(loader.loadClass("test.api.MyExtension"), extension.getApiClass)
    assertEquals(loader.loadClass("test.impl.MyExtensionImpl"), extension.getImplClass)
    assertEquals(classOf[ExtensionAPI], extension.getComponentAnnotationClass)
    assertEquals(ComponentScope.PROJECT, extension.getComponentScope)
    assertEquals(ComponentProfiles.PRODUCTION, extension.getComponentProfiles)
    assertTrue(extension.isLazy)
    assertEquals(3, extension.getParametersCount)

    val types = extension.getParameterTypes
    assertEquals(loader.loadClass("test.api.MyService"), types(0))
    types(1) match
      case parameterized: ParameterizedType =>
        assertEquals(classOf[java.util.List[?]], parameterized.getRawType)
        assertEquals(List(classOf[String]), parameterized.getActualTypeArguments.toList)
      case other => fail(s"Expected parameterized type, but $other")
    assertEquals(classOf[Int], types(2))

    val serviceImpl = loader.loadClass("test.impl.MyServiceImpl").getConstructor().newInstance()
    val created = extension.create(Array[Object](serviceImpl, java.util.List.of("a"), Integer.valueOf(42)))
    assertEquals(loader.loadClass("test.impl.MyExtensionImpl"), created.getClass)
    assertEquals(42, created.getClass.getMethod("count").invoke(created))

    val service = binding("test.impl.MyServiceImpl_Binding")
    assertEquals(ComponentScope.APPLICATION, service.getComponentScope)
    assertFalse(service.isLazy)
    assertEquals(0, service.getComponentProfiles)
    assertSame(InjectingBinding.EMPTY_TYPES, service.getParameterTypes)

    val scalaExtension = binding("test.impl.ScalaExtensionImpl_Binding")
    assertEquals("test.impl.ScalaExtension", scalaExtension.getApiClassName)
    assertEquals(ComponentScope.MODULE, scalaExtension.getComponentScope)

  @Test
  def testTopicBinding(): Unit =
    val result = compile(Map(
      "test/impl/Topics.scala" ->
        """package test.impl
          |
          |import consulo.annotation.component.*
          |
          |@TopicAPI(ComponentScope.PROJECT)
          |trait MyListener:
          |  def changed(name: String, values: java.util.List[String], count: Int): Unit
          |
          |  def reset(): Unit
          |
          |  def parameterless: Unit = {}
          |
          |class RecordingListener extends MyListener:
          |  val calls = new java.util.ArrayList[String]()
          |
          |  override def changed(name: String, values: java.util.List[String], count: Int): Unit = calls.add(s"changed $name $values $count")
          |
          |  override def reset(): Unit = calls.add("reset")
          |
          |  override def parameterless: Unit = calls.add("parameterless")
          |""".stripMargin
    ))

    assertEquals(Nil, result.errors)

    val services = Files.readAllLines(result.classes.resolve("META-INF/services/" + classOf[TopicBinding].getName)).asScala.toList
    assertEquals(List("test.impl.MyListener_Binding"), services.filter(_.nonEmpty))

    val loader = new URLClassLoader(Array(result.classes.toUri.toURL), getClass.getClassLoader)
    val binding = loader.loadClass("test.impl.MyListener_Binding").getConstructor().newInstance().asInstanceOf[TopicBinding]
    assertEquals("test.impl.MyListener", binding.getApiClassName)

    val methods = binding.methods().map(m => m.getName -> m).toMap
    assertEquals(Set("changed", "reset", "parameterless"), methods.keySet)
    assertEquals(List(classOf[String], classOf[java.util.List[?]], classOf[Int]), methods("changed").getArgumentTypes.toList)
    assertSame(TopicBinding.EMPTY_TYPES, methods("reset").getArgumentTypes)

    val listener = loader.loadClass("test.impl.RecordingListener").getConstructor().newInstance()
    methods("changed").getInvoker.accept(listener, Array[Object]("a", java.util.List.of("b"), Integer.valueOf(1)))
    methods("reset").getInvoker.accept(listener, Array.empty[Object])
    methods("parameterless").getInvoker.accept(listener, Array.empty[Object])

    val calls = listener.getClass.getMethod("calls").invoke(listener).asInstanceOf[java.util.List[String]]
    assertEquals(List("changed a [b] 1", "reset", "parameterless"), calls.asScala.toList)

  @Test
  def testErrors(): Unit =
    val result = compile(Map(
      "test/impl/Errors.scala" ->
        """package test.impl
          |
          |import consulo.annotation.component.*
          |
          |@ExtensionImpl
          |object ObjectImpl extends test.api.MyExtension
          |
          |@ExtensionImpl
          |class WithoutApi
          |
          |@ExtensionImpl
          |class PrivateConstructor private () extends test.api.MyExtension
          |
          |object Holder:
          |  @ExtensionImpl
          |  class NestedExtension extends test.api.MyExtension
          |
          |@TopicAPI(ComponentScope.PROJECT)
          |trait WrongListener:
          |  def compute(): Int
          |""".stripMargin
    ))

    assertEquals(5, result.errors.size, result.errors.mkString("\n"))
    assertTrue(result.errors.exists(_.contains("can't be used on object")), result.errors.mkString("\n"))
    assertTrue(result.errors.exists(_.contains("Can't find single @ExtensionAPI")), result.errors.mkString("\n"))
    assertTrue(result.errors.exists(_.contains("There no public constructor")), result.errors.mkString("\n"))
    assertTrue(result.errors.exists(_.contains("can be used only on top level classes")), result.errors.mkString("\n"))
    assertTrue(result.errors.exists(_.contains("WrongListener.compute must return Unit")), result.errors.mkString("\n"))

  private case class CompileResult(classes: Path, errors: List[String])

  private def compile(scalaSources: Map[String, String]): CompileResult =
    val classes = Files.createDirectories(tempDir.resolve("classes"))
    val classpath = (List(classOf[ExtensionImpl], classOf[InjectingBinding], classOf[Inject], classOf[Option[?]], classOf[Tuple])
      .map(c => Path.of(c.getProtectionDomain.getCodeSource.getLocation.toURI).toString) :+ classes.toString)
      .distinct
      .mkString(File.pathSeparator)

    val javaFiles = writeSources(tempDir.resolve("api"), ApiSources)
    val javac = ToolProvider.getSystemJavaCompiler
    val javacResult = javac.run(null, null, null, (List("-d", classes.toString, "-classpath", classpath) ++ javaFiles.map(_.toString))*)
    assertEquals(0, javacResult)

    val scalaFiles = writeSources(tempDir.resolve("src"), scalaSources)

    val pluginDir = Path.of(classOf[ConsuloBindingPlugin].getProtectionDomain.getCodeSource.getLocation.toURI)
    val sourcesDir = tempDir.resolve("generated")

    val args = List(
      "-d", classes.toString,
      "-classpath", classpath,
      s"-Xplugin:$pluginDir",
      s"-P:${ConsuloBindingPlugin.Name}:sources:$sourcesDir"
    ) ++ scalaFiles.map(_.toString)

    val errors = mutable.ListBuffer[String]()
    val reporter: SimpleReporter = diagnostic =>
      if diagnostic.level == Diagnostic.ERROR then
        errors += diagnostic.message

    Main.process(args.toArray, reporter, null)
    CompileResult(classes, errors.toList)

  private def writeSources(dir: Path, sources: Map[String, String]): List[Path] =
    sources.toList.map { (path, text) =>
      val file = dir.resolve(path)
      Files.createDirectories(file.getParent)
      Files.writeString(file, text, StandardCharsets.UTF_8)
    }
