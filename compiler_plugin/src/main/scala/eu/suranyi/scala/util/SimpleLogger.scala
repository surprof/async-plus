package eu.suranyi.scala.util

trait SimpleLogger:
  lazy protected val logger: TheLogger = TheLogger(getClass.getSimpleName)

case class TheLogger(clazz: String):
  import TheLogger.*
  private def format(level: Level, what: String) =
    s"${clazz}-${level}: ${what}"
  private def log(level: Level, what: String): Unit =
    if isEnabled(level) then println(format(level, what))
  def error(what: String) = log(Level.Error, what)
  def warn(what: String) = log(Level.Warning, what)
  def info(what: String) = log(Level.Info, what)
  def debug(what: String) = log(Level.Debug, what)

object TheLogger:
  private enum Level:
    case Debug, Info, Warning, Error
  private val currentLevel: Level = Level.Debug
  private def isEnabled(level: Level): Boolean = level.ordinal >= currentLevel.ordinal
