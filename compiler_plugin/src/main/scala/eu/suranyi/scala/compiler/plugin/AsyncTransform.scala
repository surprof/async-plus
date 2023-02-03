package eu.suranyi.scala.compiler.plugin

import dotty.tools.dotc.ast.async.AsyncMacroTransform
import dotty.tools.dotc.plugins.*

import eu.suranyi.scala.util.SimpleLogger

import scala.collection.concurrent

class AsyncTransform extends StandardPlugin with SimpleLogger:
  override val name: String = getClass.getSimpleName
  override val description: String = "Transforms async annotations to macro invocations"
  override def init(options: List[String]): List[PluginPhase] =
    logger.info(s"Initialized compiler plugin ${name}...")
    AsyncMacroTransform :: Nil
