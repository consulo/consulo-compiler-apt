package consulo.compiler.apt.scalac

import consulo.compiler.apt.shared.generation.expression.GeneratedExpression
import consulo.compiler.apt.shared.generation.`type`.*
import consulo.compiler.apt.shared.generation.{GeneratedClass, GeneratedElementFactory, GeneratedMethod, GeneratedModifier}
import consulo.compiler.apt.shared.ConsuloClasses.consulo.annotation.component as Component
import consulo.compiler.apt.shared.ConsuloClasses.consulo.component.bind as Bind
import consulo.compiler.apt.shared.{ApiImplData, ComponentScope, ConsuloClasses}
import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.core.Annotations.Annotation
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.NameOps.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.TypeErasure
import dotty.tools.dotc.core.Types.*
import dotty.tools.dotc.report

import java.lang.reflect.Type as JType
import java.util.function.BiConsumer
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

/**
 * Builds binding classes using shared generation model. Mirrors `InjectingBindingProcessor` (javac).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
object BindingGenerator:
  /**
   * @param serviceInterface interface of `META-INF/services` entry
   * @param qualifiedName    qualified name of generated class
   */
  case class Binding(serviceInterface: String, qualifiedName: String, generatedClass: GeneratedClass)

  private val ParameterizedTypeImpl = "consulo.component.bind.ParameterizedTypeImpl"
  private val TopicMethod = "consulo.component.bind.TopicMethod"
  private val AnActionClass = "consulo.ui.ex.action.AnAction"
  private val ActionGroupClass = "consulo.ui.ex.action.ActionGroup"

  private final class GenerationError(message: String) extends Exception(message)

  def generate(cls: ClassSymbol, factory: GeneratedElementFactory)(using Context): List[Binding] =
    val topicBinding = annotation(cls, Component.TopicAPI).flatMap(_ => guarded(cls)(generateTopicBinding(cls, factory)))

    val injectingBinding = ApiImplData.getApiAnnotations.asScala.iterator.flatMap { (implClassName, apiClassName) =>
      annotation(cls, implClassName).flatMap { implAnnotation =>
        guarded(cls)(generateInjectingBinding(cls, implClassName, implAnnotation, apiClassName, factory))
      }
    }.nextOption()

    topicBinding.toList ++ injectingBinding

  private def guarded(cls: ClassSymbol)(body: => Binding)(using Context): Option[Binding] =
    try Some(body)
    catch
      case e: GenerationError =>
        report.error(e.getMessage, cls.srcPos)
        None

  /**
   * Mirrors `TopicBindingProcessor` (javac). Instead of lambdas - each topic method is invoked by inner class
   */
  private def generateTopicBinding(cls: ClassSymbol, f: GeneratedElementFactory)(using Context): Binding =
    val annotationName = "@" + shortName(Component.TopicAPI)

    if !cls.is(Flags.Trait) then
      fail(s"${cls.fullName} must be interface (trait)")

    checkTopLevel(cls, annotationName)

    if cls.typeParams.nonEmpty then
      fail(s"$annotationName can't be used on generic trait")

    val topicMethods = cls.info.decls.toList.filter { member =>
      member.is(Flags.Method) && !member.isConstructor && !member.isOneOf(Flags.Private | Flags.Synthetic | Flags.Accessor)
    }

    if topicMethods.isEmpty then
      fail(s"${cls.fullName} no methods for call")

    val packageName = packageNameOf(cls)
    val bindingName = cls.name.toString + "_Binding"
    val bindingQualifiedName = qualified(packageName, bindingName)

    val generatedClass = f.newClass(packageName, bindingName)
    generatedClass.withSuperInterfaces(List(GeneratedClassType(Bind.TopicBinding)).asJava)

    val topicBindingType = GeneratedClassType(Bind.TopicBinding)
    val objectType = GeneratedClassType(classOf[Object])
    val argsType = GeneratedArrayType(objectType)
    val receiverParam = "o"
    val argsParam = "args"

    val invokers = topicMethods.zipWithIndex.map { (topicMethod, index) =>
      if !topicMethod.info.finalResultType.isRef(defn.UnitClass) then
        fail(s"${cls.fullName}.${topicMethod.name} must return Unit")

      val parameterTypes = topicMethod.info.paramInfoss match
        case Nil => None // parameterless method - called without parentheses
        case single :: Nil => Some(single)
        case _ => fail(s"${cls.fullName}.${topicMethod.name} with several parameter lists is not supported")

      val invoker = f.newClass("", s"Invoker$index")
      invoker.withSuperInterfaces(List(
        GeneratedParametrizedType(GeneratedClassType(classOf[BiConsumer[?, ?]]), List[GeneratedType](objectType, argsType).asJava)
      ).asJava)

      val receiver = f.newClassCastExpression(classType(cls), f.newReferenceExpression(receiverParam))
      val call = parameterTypes match
        case None =>
          f.newQualifiedExpression(receiver, f.newReferenceExpression(topicMethod.name.toString))
        case Some(types) =>
          val arguments = types.zipWithIndex.map { (parameterType, argIndex) =>
            f.newClassCastExpression(generatedType(parameterType), f.newArrayGetExpression(f.newReferenceExpression(argsParam), argIndex))
          }
          f.newMethodCallExpression(receiver, topicMethod.name.toString, arguments.asJava)

      val accept = overrideMethod(f, GeneratedClassType(Void.TYPE), "accept", call)
      accept.withParameters(List(f.newVariable(objectType, receiverParam), f.newVariable(argsType, argsParam)).asJava)
      invoker.withMethods(List(accept).asJava)

      val typesExpression = parameterTypes.getOrElse(Nil) match
        case Nil => f.newQualifiedExpression(f.newClassReferenceExpression(topicBindingType), f.newReferenceExpression("EMPTY_TYPES"))
        case types => f.newArrayExpression(types.map(t => classLiteral(erasedClass(t), f)).asJava)

      val create = f.newMethodCallExpression(
        f.newClassReferenceExpression(GeneratedClassType(TopicMethod)),
        "create",
        List[GeneratedExpression](
          f.newConstantExpression(topicMethod.name.toString),
          typesExpression,
          f.newExpression(GeneratedClassType(s"$bindingQualifiedName.Invoker$index"), List.empty[GeneratedExpression].asJava)
        ).asJava
      )

      (invoker, create)
    }

    generatedClass.withMethods(List(
      overrideMethod(f, GeneratedClassType(classOf[String]), "getApiClassName", f.newConstantExpression(qualifiedName(cls))),
      overrideMethod(f, GeneratedArrayType(GeneratedClassType(TopicMethod)), "methods", f.newArrayExpression(invokers.map(_._2).asJava))
    ).asJava)
    generatedClass.withInnerClasses(invokers.map(_._1).asJava)

    Binding(Bind.TopicBinding, bindingQualifiedName, generatedClass)

  private def overrideMethod(f: GeneratedElementFactory, returnType: GeneratedType, name: String, expression: GeneratedExpression): GeneratedMethod =
    val method = f.newMethod(returnType, name)
    method.withModifiers(GeneratedModifier.PUBLIC)
    method.withOverride()
    method.withStatement(f.newReturnStatement(expression))
    method

  private def checkTopLevel(cls: ClassSymbol, annotationName: String)(using Context): Unit =
    if !cls.owner.is(Flags.PackageClass) then
      fail(s"$annotationName can be used only on top level classes")

  private def packageNameOf(cls: ClassSymbol)(using Context): String =
    val packageClass = cls.enclosingPackageClass
    if packageClass.isEffectiveRoot then "" else packageClass.fullName.toString

  private def qualified(packageName: String, name: String): String =
    if packageName.isEmpty then name else s"$packageName.$name"

  private def generateInjectingBinding(cls: ClassSymbol,
                                       implClassName: String,
                                       implAnnotation: Annotation,
                                       apiClassName: String,
                                       f: GeneratedElementFactory)(using Context): Binding =
    val annotationName = "@" + shortName(implClassName)

    if cls.is(Flags.Module) then
      fail(s"$annotationName can't be used on object. Use class")

    if cls.is(Flags.Trait) || cls.is(Flags.Abstract) then
      fail(s"$annotationName can't be used on abstract class or trait")

    checkTopLevel(cls, annotationName)

    if cls.typeParams.nonEmpty then
      fail(s"$annotationName can't be used on generic class")

    val (apiSymbol, apiAnnotation) = findApi(cls, apiClassName)

    val packageName = packageNameOf(cls)
    val bindingName = cls.name.toString + "_Binding"

    val generatedClass = f.newClass(packageName, bindingName)

    val injectingBindingType = GeneratedClassType(Bind.InjectingBinding)
    generatedClass.withSuperInterfaces(List(injectingBindingType).asJava)

    val classOfAny = GeneratedParametrizedType(GeneratedClassType(classOf[Class[?]]), List[GeneratedType](GeneratedWildcardType()).asJava)

    val methods = List.newBuilder[GeneratedMethod]

    def method(returnType: GeneratedType, name: String, expression: GeneratedExpression): GeneratedMethod =
      val method = overrideMethod(f, returnType, name, expression)
      methods += method
      method

    method(classOfAny, "getComponentAnnotationClass", f.newClassClassExpression(GeneratedClassType(apiClassName)))
    method(classOfAny, "getImplClass", classLiteral(cls, f))
    method(GeneratedClassType(classOf[String]), "getApiClassName", f.newConstantExpression(qualifiedName(apiSymbol)))
    method(classOfAny, "getApiClass", classLiteral(apiSymbol, f))

    val parameterTypes = injectConstructorParameters(cls)

    method(GeneratedClassType(classOf[Int]), "getParametersCount", f.newConstantExpression(parameterTypes.size))

    val profiles = annotationArgument(implAnnotation, "profiles").flatMap(constantValue) match
      case Some(value: Int) => value
      case _ => 0 // ComponentProfiles.DEFAULT
    method(GeneratedClassType(classOf[Int]), "getComponentProfiles", f.newConstantExpression(profiles))

    val lazyValue = annotationArgument(apiAnnotation, "lazy").flatMap(constantValue) match
      case Some(value: Boolean) => value
      case _ => true
    if !lazyValue then
      method(GeneratedClassType(classOf[Boolean]), "isLazy", f.newConstantExpression(false))

    val scope = componentScope(cls, implClassName, implAnnotation, apiClassName, apiAnnotation)
    val componentScopeType = GeneratedClassType(Component.ComponentScope)
    method(componentScopeType, "getComponentScope",
      f.newQualifiedExpression(f.newClassReferenceExpression(componentScopeType), f.newReferenceExpression(scope.name())))

    val typesExpression =
      if parameterTypes.isEmpty then
        f.newQualifiedExpression(f.newClassReferenceExpression(injectingBindingType), f.newReferenceExpression("EMPTY_TYPES"))
      else
        f.newArrayExpression(parameterTypes.map(typeExpression(_, f)).asJava)
    method(GeneratedArrayType(GeneratedClassType(classOf[JType])), "getParameterTypes", typesExpression)

    val argsParam = "args"
    val arguments = parameterTypes.zipWithIndex.map { (parameterType, index) =>
      f.newClassCastExpression(generatedType(parameterType), f.newArrayGetExpression(f.newReferenceExpression(argsParam), index))
    }
    val create = method(GeneratedClassType(classOf[Object]), "create", f.newExpression(classType(cls), arguments.asJava))
    create.withParameters(List(f.newVariable(GeneratedArrayType(GeneratedClassType(classOf[Object])), argsParam)).asJava)

    generatedClass.withMethods(methods.result().asJava)

    Binding(Bind.InjectingBinding, qualified(packageName, bindingName), generatedClass)

  private def fail(message: String): Nothing = throw GenerationError(message)

  private def findApi(cls: ClassSymbol, apiClassName: String)(using Context): (Symbol, Annotation) =
    val targets = cls.baseClasses.flatMap(base => annotation(base, apiClassName).map(base -> _))

    // same as javac processor - ActionGroup extends AnAction, both have own @ActionAPI
    val filtered =
      if apiClassName == Component.ActionAPI
        && targets.exists(_._1.fullName.toString == AnActionClass)
        && targets.exists(_._1.fullName.toString == ActionGroupClass)
      then targets.filterNot(_._1.fullName.toString == AnActionClass)
      else targets

    filtered match
      case single :: Nil => single
      case _ => fail(s"Can't find single @${shortName(apiClassName)} annotation for: ${cls.fullName} in super classes.")

  private def componentScope(cls: ClassSymbol,
                             implClassName: String,
                             implAnnotation: Annotation,
                             apiClassName: String,
                             apiAnnotation: Annotation)(using Context): ComponentScope =
    // actions always bind in application injecting scope
    if apiClassName == Component.ActionAPI then ComponentScope.APPLICATION
    else
      val source = if implClassName == Component.TopicImpl then implAnnotation else apiAnnotation
      annotationArgument(source, "value").map(enumConstantName) match
        case Some(name) if ComponentScope.values().exists(_.name() == name) => ComponentScope.valueOf(name)
        case other => fail(s"Can't resolve component scope for ${cls.fullName}: $other")

  private def injectConstructorParameters(cls: ClassSymbol)(using Context): List[Type] =
    val constructors = cls.info.decls.toList.filter(_.isConstructor)

    val injectClass = getClassIfDefined(ConsuloClasses.jakarta.inject.Inject)

    def isPublic(symbol: Symbol) = !symbol.isOneOf(Flags.Private | Flags.Protected) && !symbol.privateWithin.exists

    val constructor = constructors.find(c => injectClass.exists && c.hasAnnotation(injectClass))
      .orElse(constructors.find(c => isPublic(c) && c.info.paramInfoss.flatten.isEmpty))
      .getOrElse(fail("There no public constructor or constructor with @Inject annotation. Injecting impossible"))

    constructor.info.paramInfoss match
      case Nil => Nil
      case single :: Nil => single
      case _ => fail("Constructor with several parameter lists (including using clauses) is not supported for injecting")

  /** `Type` expression for `getParameterTypes`, same as javac processor: parameterized types are described by `ParameterizedTypeImpl` */
  private def typeExpression(tp: Type, f: GeneratedElementFactory)(using Context): GeneratedExpression =
    tp.widenDealias match
      case AppliedType(tycon, args) if tycon.typeSymbol != defn.ArrayClass =>
        val raw = classLiteral(tycon.typeSymbol, f)
        val arguments = args.map { arg =>
          val argClass = arg match
            case _: TypeBounds => defn.ObjectClass
            // scalac emits erased java.lang.Object for primitive type arguments in generic signatures
            case _ if arg.typeSymbol.isPrimitiveValueClass => defn.ObjectClass
            case _ => erasedClass(arg)
          classLiteral(argClass, f)
        }
        f.newExpression(GeneratedClassType(ParameterizedTypeImpl), (raw :: arguments).asJava)
      case other =>
        classLiteral(erasedClass(other), f)

  private def erasedClass(tp: Type)(using Context): Symbol =
    TypeErasure.erasure(tp) match
      case _: JavaArrayType => fail(s"Array type of injecting parameter is not supported: ${tp.show}")
      case erased => erased.classSymbol

  /** Type of cast in `create` - full (non-erased) type */
  private def generatedType(tp: Type)(using Context): GeneratedType =
    tp.widenDealias match
      case AppliedType(tycon, args) if tycon.typeSymbol == defn.ArrayClass =>
        GeneratedArrayType(generatedType(args.head))
      case AppliedType(tycon, args) =>
        val arguments = args.map {
          case _: TypeBounds => GeneratedWildcardType()
          case arg => generatedType(arg)
        }
        GeneratedParametrizedType(classType(tycon.typeSymbol), arguments.asJava)
      case other if other.typeSymbol.isClass =>
        classType(other.typeSymbol)
      case other =>
        classType(erasedClass(other))

  private def classType(symbol: Symbol)(using Context): GeneratedClassType =
    GeneratedClassType(qualifiedName(symbol))

  /** Scala has no raw class literals - `classOf` of generic class requires wildcard arguments (`classOf[java.util.List[?]]`) */
  private def classLiteral(symbol: Symbol, f: GeneratedElementFactory)(using Context): GeneratedExpression =
    val typeParams = symbol.typeParams
    val name = qualifiedName(symbol)
    val literalName = if typeParams.isEmpty then name else typeParams.map(_ => "?").mkString(s"$name[", ", ", "]")
    f.newClassClassExpression(GeneratedClassType(literalName))

  /** Source qualified name (`a.b.Outer.Inner`) - nested classes of objects and java static nested classes are owned by module classes */
  private def qualifiedName(symbol: Symbol)(using Context): String =
    val packageClass = symbol.enclosingPackageClass
    val names = ownerNames(symbol).mkString(".")
    if packageClass.isEffectiveRoot then names else s"${packageClass.fullName}.$names"

  private def ownerNames(symbol: Symbol)(using Context): List[String] =
    (symbol :: symbol.owner.ownersIterator.takeWhile(!_.is(Flags.PackageClass)).toList).reverse
      .map(_.name.stripModuleClassSuffix.toString)

  private def annotation(symbol: Symbol, annotationClassName: String)(using Context): Option[Annotation] =
    val annotationClass = getClassIfDefined(annotationClassName)
    if annotationClass.exists then symbol.getAnnotation(annotationClass) else None

  private def annotationArgument(annotation: Annotation, name: String)(using Context): Option[Tree] =
    val arguments = annotation.arguments
    arguments.collectFirst { case NamedArg(argName, arg) if argName.toString == name => arg }
      .orElse {
        // single positional argument of java annotation is `value`
        if name == "value" then arguments.collectFirst { case arg if !arg.isInstanceOf[NamedArg] => arg }
        else None
      }

  @tailrec
  private def strip(tree: Tree): Tree = tree match
    case NamedArg(_, arg) => strip(arg)
    case Typed(expr, _) => strip(expr)
    case Inlined(_, Nil, expr) => strip(expr)
    case Block(Nil, expr) => strip(expr)
    case _ => tree

  private def constantValue(tree: Tree)(using Context): Option[Any] =
    strip(tree) match
      case Literal(constant) => Some(constant.value)
      case other =>
        other.tpe.widenTermRefExpr match
          case ConstantType(constant) => Some(constant.value)
          case _ => None

  private def enumConstantName(tree: Tree)(using Context): String =
    val stripped = strip(tree)
    val symbol = if stripped.symbol.exists then stripped.symbol else stripped.tpe.termSymbol
    symbol.name.toString

  private def shortName(qualifiedName: String): String =
    qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
