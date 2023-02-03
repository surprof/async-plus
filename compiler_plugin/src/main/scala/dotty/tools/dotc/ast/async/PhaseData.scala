package dotty.tools.dotc.ast.async

import dotty.tools.dotc.util.SourceFile

class PhaseData private(val sourceFile: SourceFile):
  private var imports: Boolean = false
  def isLive: Boolean = imports
  def setLive(): Unit = imports = true

object PhaseData:
  def apply(sourceFile: dotty.tools.dotc.interfaces.SourceFile): PhaseData = {
    new PhaseData(sourceFile match {
      case sf: SourceFile => sf
    })
  }
