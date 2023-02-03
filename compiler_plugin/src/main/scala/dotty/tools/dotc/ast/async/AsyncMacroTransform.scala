package dotty.tools.dotc.ast.async

import dotty.tools.dotc.ast.{tpd, untpd}
import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.ast.Trees.ApplyKind
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Names
import dotty.tools.dotc.core.Names.Name
import dotty.tools.dotc.interfaces.SourceFile
import dotty.tools.dotc.plugins.*
import dotty.tools.dotc.parsing.Parser
import dotty.tools.dotc.report
import dotty.tools.dotc.typer.TyperPhase
import eu.suranyi.scala.util.SimpleLogger

import scala.collection.concurrent

case object AsyncMacroTransform extends PluginPhase with SimpleLogger:
  override def phaseName: String = getClass.getSimpleName

  override val runsAfter = Set(Parser.name)
  override val runsBefore = Set(TyperPhase.name)

//  private def transform[T <: tpd.Tree](tree: T)(using Context): T =
//    val defname = Thread.currentThread.getStackTrace()(2).getMethodName
//    logger.debug(s"${defname}: Transforming ${tree.getClass}...")
//    tree

  //  override def transformIdent(tree: Ident)(using Context): Tree = transform(tree)
  //  override def transformSelect(tree: Select)(using Context): Tree = transform(tree)
  //  override def transformThis(tree: This)(using Context): Tree = transform(tree)
  //  override def transformSuper(tree: Super)(using Context): Tree = transform(tree)
  //  override def transformApply(tree: Apply)(using Context): Tree = transform(tree)
  //  override def transformTypeApply(tree: TypeApply)(using Context): Tree = transform(tree)
  //  override def transformLiteral(tree: Literal)(using Context): Tree = transform(tree)
  //  override def transformNew(tree: New)(using Context): Tree = transform(tree)
  //  override def transformTyped(tree: Typed)(using Context): Tree = transform(tree)
  //  override def transformAssign(tree: Assign)(using Context): Tree = transform(tree)
  //  override def transformBlock(tree: Block)(using Context): Tree = transform(tree)
  //  override def transformIf(tree: If)(using Context): Tree = transform(tree)
  //  override def transformClosure(tree: Closure)(using Context): Tree = transform(tree)
  //  override def transformMatch(tree: Match)(using Context): Tree = transform(tree)
  //  override def transformCaseDef(tree: CaseDef)(using Context): Tree = transform(tree)
  //  override def transformLabeled(tree: Labeled)(using Context): Tree = transform(tree)
  //  override def transformReturn(tree: Return)(using Context): Tree = transform(tree)
  //  override def transformWhileDo(tree: WhileDo)(using Context): Tree = transform(tree)
  //  override def transformTry(tree: Try)(using Context): Tree = transform(tree)
  //  override def transformSeqLiteral(tree: SeqLiteral)(using Context): Tree = transform(tree)
  //  override def transformInlined(tree: Inlined)(using Context): Tree = transform(tree)
  //  override def transformTypeTree(tree: TypeTree)(using Context): Tree = transform(tree)
  //  override def transformBind(tree: Bind)(using Context): Tree = transform(tree)
  //  override def transformAlternative(tree: Alternative)(using Context): Tree = transform(tree)
  //  override def transformUnApply(tree: UnApply)(using Context): Tree = transform(tree)
  //  override def transformValDef(tree: ValDef)(using Context): Tree = transform(tree)
  //  override def transformDefDef(tree: DefDef)(using Context): Tree = transform(tree)
  //  override def transformTypeDef(tree: TypeDef)(using Context): Tree = transform(tree)
  //  override def transformTemplate(tree: Template)(using Context): Tree = transform(tree)
  //  override def transformPackageDef(tree: PackageDef)(using Context): Tree = transform(tree)
  //  override def transformStats(trees: List[Tree])(using Context): List[Tree] = trees map transform
  private val phaseData/*: Map[SourceFile, PhaseData]*/ = concurrent.TrieMap.empty[SourceFile, PhaseData]
  private def initializePhaseFor(file: SourceFile) = phaseData.putIfAbsent(file, PhaseData(file))
  override def prepareForUnit(tree: tpd.Tree)(implicit ctx: Context): Context =
    initializePhaseFor(ctx.compilationUnit.source)
    ctx
  override def transformUnit(tree: tpd.Tree)(implicit ctx: Context): tpd.Tree =
    val unit = ctx.compilationUnit
    val sourceFile = unit.source
    val sourceFileName = sourceFile.name
    logger.info(s"Processing source ${sourceFileName}...")
    given PhaseData = phaseData(unit.source)
    val newtree = unit.untpdTree match {
      case untpd.PackageDef(pid, stats) =>
        logger.debug(s"Processing ${pid}...")
        untpd.PackageDef(pid, stats flatMap transformUntyped(true))
      case other => logger.warn(s"Cannot handle ${other.getClass}."); other
    }
    unit.untpdTree = newtree
    logger.info(s"Finished ${sourceFileName}.")
    tree

  //  override def transformOther(tree: Tree)(using Context): Tree = transform(tree)

  private val AnnotationClass = classOf[cps.annotation.async]
  private val AnnotationClassName = AnnotationClass.getCanonicalName.split('.').toSeq
  private def triggerPhase(tree: untpd.Tree)(part3: untpd.ImportSelector): Boolean =
    tree match {
      case Trees.Select(Trees.Ident(part1), part2) => // cps.annotation
        AnnotationClassName == Seq(part1.mangledString, part2.mangledString, part3.name.mangledString)
      case _ => false
    }

  private def TreeScalaPackage(using Context) = Trees.Ident(Names.termName("scala"))
  private def TreeScalaLanguage(using Context) = Trees.Select(TreeScalaPackage, Names.termName("language"))
  private def TypeTreeFuture(using Context) = Trees.Select(Trees.Select(TreeScalaPackage, Names.termName("concurrent")), Names.typeName("Future"))
  private def TreeCpsPackage(using Context) = Trees.Ident(Names.termName("cps"))
  private def TreeCpsAutomaticColoring(using Context) = Trees.Select(TreeCpsPackage, Names.termName("automaticColoring"))
  private def TreeCpsAsyncWithFuture(using Context) = Trees.TypeApply(Trees.Select(TreeCpsPackage, Names.termName("async")), List(TypeTreeFuture))
  private def SelectorGiven(using Context) = untpd.ImportSelector(Trees.Ident(Names.termName("")))
  private def SelectorImplicitConversions(using Context) = untpd.ImportSelector(Trees.Ident(Names.termName("implicitConversions")))
  private def transformUntyped(topLevel: Boolean)(tree: untpd.Tree)(implicit phaseData: PhaseData, ctx: Context): List[untpd.Tree] = tree match {
    case Trees.Import(expr, selectors) => if selectors exists triggerPhase(expr) then
      if topLevel then
        phaseData.setLive()
        logger.info("Enabled async annotation, automatic coloring and implicit conversions for the file.")
        List(
          tree,
          Trees.Import(TreeCpsAutomaticColoring, List(SelectorGiven)),       // import cps.automaticColoring.given
          Trees.Import(TreeScalaLanguage, List(SelectorImplicitConversions)) // import scala.language.implicitConversions
        )
      else
        report.error("This must be a toplevel import.", tree.sourcePos)
        List()
    else List(tree)
    case module @ untpd.ModuleDef(name, impl) =>
      logger.debug(s"3|${name.mangledString}|${impl}") // remove
      List(module.copy(impl = transformTemplate(impl))(phaseData.sourceFile))
    case defdef: untpd.DefDef => List(if phaseData.isLive && asyncAnnotated(defdef) then
      val ntpt = defdef.tpt match {
        case empty: untpd.TypeTree => empty // there is no explicit type given
        case other => untpd.AppliedTypeTree(TypeTreeFuture, List(other))
      }
      val rhs = defdef.unforced match {
        case tree: untpd.Tree => tree
        case other => report.error(s"Unexpected ${other}."); Trees.theEmptyTree
      }
      defdef.copy(tpt = ntpt, preRhs = Trees.Apply(TreeCpsAsyncWithFuture, List(rhs)))
    else
      defdef
    )
    case other => logger.warn(s"Cannot handle ${other.getClass}."); List(other)
  }

  private def transformTemplate(template: untpd.Template)(implicit phaseData: PhaseData, ctx: Context): untpd.Template = {
    template.copy(preBody = template.body flatMap transformUntyped(false)) // package restricted access
  }

  private def asyncAnnotated(tree: untpd.DefTree): Boolean = tree.rawMods.annotations exists { // package restricted access
    case Trees.Apply(Trees.Select(Trees.New(Trees.Ident(async)), init), _) => async.mangledString == AnnotationClassName.last && init.mangledString == "<init>"
    case _ => false
  }
