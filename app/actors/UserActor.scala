package actors

import javax.inject._

import akka.actor._
import akka.event.LoggingReceive
import com.google.inject.assistedinject.Assisted
import play.api.Configuration
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._

class UserActor @Inject()(@Assisted out: ActorRef,
                          @Assisted id: String,
                          configuration: Configuration) extends Actor with ActorLogging {


  override def preStart(): Unit = {
    super.preStart()
  }

  override def receive: Receive = LoggingReceive {
    case json: JsValue =>
      // When the user types in a stock in the upper right corner, this is triggered
      val symbol = (json \ "symbol").as[String]
//      stocksActor ! WatchStock(symbol)
  }
}

class UserParentActor @Inject()(childFactory: UserActor.Factory) extends Actor with InjectedActorSupport with ActorLogging {
  import UserParentActor._

  override def receive: Receive = LoggingReceive {
    case Create(id, out) =>
      val child: ActorRef = injectedChild(childFactory(out, id), s"userActor-$id")
      sender() ! child
  }
}

object UserParentActor {
  case class Create(id: String, out: ActorRef)
}

object UserActor {
  trait Factory {
    // Corresponds to the @Assisted parameters defined in the constructor
    def apply(out: ActorRef, id: String): Actor
  }
}