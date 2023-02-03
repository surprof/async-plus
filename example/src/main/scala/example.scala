import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import cps.monads.given
import cps.annotation.async
import cps.annotation.rawasync

object Example:
  @rawasync def fetchGreeting: Future[String] =
    Future successful "Hi"

  @async def greet(): String =
    val greeting: String = fetchGreeting
    s"${greeting} from ${java.lang.Thread.currentThread().getName}!"

  def mmain(args: Array[String]): Unit =
    val f = Await.ready(greet(), Duration(1, "seconds"))
    f.failed map { ex => println(ex.getClass) }
    f map println
