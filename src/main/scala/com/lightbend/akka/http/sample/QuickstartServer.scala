package com.lightbend.akka.http.sample

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.Duration
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import StatusCodes.InternalServerError
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import akka.stream.ActorMaterializer
import scala.util.control.NonFatal
import akka.http.scaladsl.server.Directives._

//#main-class
object QuickstartServer extends App with UserRoutes {
  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  //#server-bootstrapping

  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher

  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")

  val exHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) => ctx => {
        val message = Option(e.getMessage).getOrElse(s"${e.getClass.getName} (No error message supplied)")
        ctx.log.error(e, "Error during processing of request: '{}'. Completing with {} response. ", message, InternalServerError)
        ctx.complete(HttpResponse(InternalServerError, entity = s"There was an internal server error: $message"))
      }
    }

  //#main-class
  // from the UserRoutes trait
  val testErrorRoute = path("throw") { complete((1 / 0).toString()) }
  lazy val routes: Route = handleExceptions(exHandler) { userRoutes ~ testErrorRoute }
  //#main-class

  //#http-server
  val serverBindingFuture: Future[ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  // Commenting out because this block terminates the server prematurely
  //
  //  serverBindingFuture
  //    .flatMap(_.unbind())
  //    .onComplete { done =>
  //      println("within onComplete")
  //      done.failed.map { ex => log.error(ex, "Failed unbinding") }
  //      system.terminate()
  //    }

  Await.result(system.whenTerminated, Duration.Inf)
  //#http-server
  //#main-class
}
//#main-class
