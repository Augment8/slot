package controllers

import javax.inject._

import actors.UserParentActor
import akka.pattern.ask
import akka.NotUsed
import akka.actor.Actor.Receive
import akka.actor._
import akka.event.Logging
import akka.stream.actor.{ActorPublisher, ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import akka.stream.{DelayOverflowStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.Timeout
import com.google.inject.assistedinject.Assisted
import org.joda.time.DateTime
import org.reactivestreams.{Publisher, Subscriber}
import play.api._
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object UserResponse {
  case class SessionId(sessionId: Long)
}

object TopicActor {
  case class Subscribe(actorRef: ActorRef)
}

class TopicActor extends ActorSubscriber {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  var subscribers: Set[ActorRef] = Set()

  override def receive: Receive = {
    case Terminated(actor) =>
      subscribers = subscribers - actor
    case TopicActor.Subscribe(sub) =>
      context.watch(sub)
      subscribers = subscribers + sub
    case value: JsValue =>
      subscribers.foreach(a => a ! value)
  }
}

object UserActor {
  def props(out: ActorRef, topic: ActorRef, userId: Long): Props = Props(new UserActor(out, topic, userId))
  trait Factory {
    // Corresponds to the @Assisted parameters defined in the constructor
    def apply(out: ActorRef, topic: ActorRef): Actor
  }
}
class UserActor (val out: ActorRef, val topic: ActorRef, val userId: Long) extends Actor {
  override def receive: Receive = {
    case value: JsObject =>
      topic ! value + ("userId", Json.toJson(userId))
    case value: JsValue =>
      topic ! value
  }

}
/*
object ViewActor {
  trait Factory {
    // Corresponds to the @Assisted parameters defined in the constructor
    def apply(out: ActorRef, topic: ActorRef): Actor
  }
}
class ViewActor (val out: ActorRef, val topic: ActorRef) extends Actor {
  override def receive: Receive = {
    case value: JsValue =>
      out ! value
  }

  override def preStart(): Unit = {
    super.preStart()
    topic ! TopicActor.Subscribe(context.self)
  }
}
*/

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()() // userActorを管理するActor
                              (implicit actorSystem: ActorSystem,
                                mat: Materializer,
                                ec: ExecutionContext) extends Controller {
  private val logger = org.slf4j.LoggerFactory.getLogger("controllers.HomeController")
  val topicActor = actorSystem.actorOf(Props[TopicActor])

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("hoge"))
  }

  def view = Action {
    Ok("hello")
  }
  /**
    * Creates a websocket.  `acceptOrResult` is preferable here because it returns a
    * Future[Flow], which is required internally.
    *
    * @return a fully realized websocket.
    */
  // JsValueいれられる JsValueがでてくるパイプとみなせる。実際のWebSocketはText or Byteなので、JsValue -> TextなTransformerがimplicitで挿入される。
  // jsonMessageFlowTransformer
  def indexWs: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] {
    case rh if true =>
      logger.info(s"$rh")
      inputterFutureFlow(rh).map { flow => // このflowに入力すればクライアントにとどく このflowの出力はクライアントからとどく。
        Right(flow)
      }.recover {
        case e: Exception =>
          logger.error("Cannot create websocket", e)
          val jsError = Json.obj("error" -> "Cannot create websocket")
          val result = InternalServerError(jsError)
          Left(result)
      }

    case rejected =>
      logger.error(s"Request ${rejected} failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }

  def viewWs: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    logger.info(s"$rh")
    viewerFutureFlow(rh).map { flow => // このflowに入力すればクライアントにとどく このflowの出力はクライアントからとどく。
      Right(flow)
    }.recover {
      case e: Exception =>
        logger.error("Cannot create websocket", e)
        val jsError = Json.obj("error" -> "Cannot create websocket")
        val result = InternalServerError(jsError)
        Left(result)
    }
  }

  /**
    * Checks that the WebSocket comes from the same origin.  This is necessary to protect
    * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
    *
    * See https://tools.ietf.org/html/rfc6455#section-1.3 and
    * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
    */
  def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  /**
    * Returns true if the value of the Origin header contains an acceptable value.
    */
  def originMatches(origin: String): Boolean = {
    true
    // origin.contains("localhost:9000") || origin.contains("localhost:19001")
  }

  /**
    * Creates a Future containing a Flow of JsValue in and out.
    */
  def viewerFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    // create an actor ref source and associated publisher for sink
    // webSocket用のフローをつくる 入り口をactorRef 出口をPublisherにする
    // 仮想的なもので、これらのフローは接続していないと予想される。

    // Create a user actor off the request id and attach it to the source
    // userActorからのメッセージをWebSocketに流す

    // actorSystem.scheduler.schedule(5 seconds, 1 seconds, topicIn, Json.toJson("foo"))

    // Future(Flow.fromFunction{a => Json.toJson(request.headers.get("Sec-WebSocket-Protocol").toString())})
    // Future(Flow.fromFunction[JsValue, JsValue](a => a).filter(_ => false))
    // 1秒間に一度カウントアップするSource
    // println(topicOut.toString)
    val source = Source.actorRef(1, OverflowStrategy.dropTail)
    val sink = Sink.asPublisher(false): Sink[JsValue, Publisher[JsValue]]
    val (inActor, outPublisher) = source.toMat(sink)(Keep.both).run

    topicActor ! TopicActor.Subscribe(inActor)
    Future(Flow.fromSinkAndSource(Sink.ignore, Source.fromPublisher(outPublisher)))

    // ActorFlow.actorRef[JsValue,JsValue](out => Props(classOf[ViewActor], out, topicActor))[JsValue, JsValue, NotUsed]
  }

  def inputterFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, _]] = {
    val source = Source.actorRef(1, OverflowStrategy.dropTail)
    val sink = Sink.asPublisher(false)
    val (inActor, outPublisher) = source.toMat(sink)(Keep.both).run

    //Future(Flow.fromSinkAndSource(sink, source))
    // Future(Flow.fromSinkAndSource(Sink.actorRef(topicActor, akka.actor.Status.Success(())), Source.fromPublisher(outPublisher)))

    val userId = request.cookies.get("SESSION_ID").fold(new DateTime().getMillis)(n => n.value.toLong)
    Future(ActorFlow.actorRef[JsValue, JsValue] { out =>
      val typeTransform: JsObject => JsObject = { o => o + ("type", Json.toJson("session_id"))}
      implicit val n = Json.writes[UserResponse.SessionId].transform(typeTransform)
      out ! Json.toJson(UserResponse.SessionId(userId))
      UserActor.props(out, topicActor, userId)
    })
  }

  /**
    * Creates a materialized flow for the websocket, exposing the source and sink.
    *
    * @return the materialized input and output of the flow.
    */
  def createWebSocketConnections(): (ActorRef, Publisher[JsValue]) = {

    // Creates a source to be materialized as an actor reference.
    val source: Source[JsValue, ActorRef] = {
      // If you want to log on a flow, you have to use a logging adapter.
      // http://doc.akka.io/docs/akka/2.4.4/scala/logging.html#SLF4J
      val logging = Logging(actorSystem.eventStream, logger.getName)

      // Creating a source can be done through various means, but here we want
      // the source exposed as an actor so we can send it messages from other
      // actors.j
      // JsValueのメッセージが送出されるアクターをつくる (送出される先はsinkで決定できる?)
      Source.actorRef[JsValue](10, OverflowStrategy.dropTail).log("actorRefSource")(logging)
    }

    // Creates a sink to be materialized as a publisher.  Fanout is false as we only want
    // a single subscriber here.
    val sink: Sink[JsValue, Publisher[JsValue]] = Sink.asPublisher(fanout = false)

    // Connect the source and sink into a flow, telling it to keep the materialized values,
    // and then kicks the flow into existence.
    source.toMat(sink)(Keep.both).run()
  }

  /**
    * Creates a flow of events from the websocket to the user actor.
    *
    * When the flow is terminated, the user actor is no longer needed and is stopped.
    *
    * @param userActor   the user actor receiving websocket events.
    * @param webSocketIn the "read" side of the websocket, that publishes JsValue to UserActor.
    * @return a Flow of JsValue in both directions.
    */
  def createWebSocketFlow(webSocketIn: Publisher[JsValue], userActor: ActorRef): Flow[JsValue, JsValue, NotUsed] = {
    // http://doc.akka.io/docs/akka/current/scala/stream/stream-flows-and-basics.html#stream-materialization
    // http://doc.akka.io/docs/akka/current/scala/stream/stream-integrations.html#integrating-with-actors

    // source is what comes in: browser ws events -> play -> publisher -> userActor
    // sink is what comes out:  userActor -> websocketOut -> play -> browser ws events
    val flow = {
      val sink = Sink.actorRef(userActor, akka.actor.Status.Success(()))  // Sucessは正常終了時のメッセージ
      val source = Source.fromPublisher(webSocketIn)
      Flow.fromSinkAndSource(sink, source)
    }

    // Unhook the user actor when the websocket flow terminates
    // http://doc.akka.io/docs/akka/current/scala/stream/stages-overview.html#watchTermination
    val flowWatch: Flow[JsValue, JsValue, NotUsed] = flow.watchTermination() { (_, termination) =>
      termination.foreach { done =>
        logger.info(s"Terminating actor $userActor")
        actorSystem.stop(userActor)
      }
      NotUsed
    }

    flow
  }

  /**
    * Creates a user actor with a given name, using the websocket out actor for output.
    *
    * @param name         the name of the user actor.
    * @param webSocketOut the "write" side of the websocket, that the user actor sends JsValue to.
    * @return a user actor for this ws connection.
    */
  def createUserActor(name: String, webSocketOut: ActorRef): Future[ActorRef] = {
    // Use guice assisted injection to instantiate and configure the child actor.
    //val userActorFuture = {
      implicit val timeout = Timeout(100.millis)
      // userParentActor にアクターをつくるようにたのむ。
      // (userParentActor ? UserParentActor.Create(name, webSocketOut)).mapTo[ActorRef]
    //}
    ///userActorFuture
    ???
  }

}
