package actors

import actors.UserParentActor.Create
import actors.ViewerActor.Subscribe
import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import play.api.libs.json.JsValue

object ViewerActor {
  case class Subscribe(out: ActorRef)
}

class ViewerActor @Inject() extends Actor {
  var outs: Seq[ActorRef] = Seq()

  override def receive: Receive = {
    case Subscribe(out) =>
      outs = outs :+ out
    case json: JsValue =>
      outs.foreach(out => out ! json)
  }
}

object ViewParentActor {
   object Create
}

class ViewParentActor @Inject() extends Actor {
  override def receive: Receive = {
    case _: Create =>
  }
}