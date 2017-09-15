package controllers

import model.{NavigationData, WebPageData}
import org.joda.time.DateTime
import play.Logger
import play.api.data.Form
import play.api.mvc.Controller
import security.UserAuthAction
import services.RewindService

import scala.util.{Failure, Success}

/**
  * AdminController class
  */
class AdminController(userAuthAction: UserAuthAction, rewindService: RewindService) extends Controller {

  import play.api.data.Forms._

  val rewindRequestForm = Form {
    mapping(
      "destionation" → longNumber
    )(RewindRequestData.apply)(RewindRequestData.unapply)
  }

  case class RewindRequestData(destination: Long)

  def admin = userAuthAction {
    request ⇒
      if (request.user.isAdmin) {
        Ok(views.html.pages.admin(NavigationData(Some(request.user), isLoggedIn = true), WebPageData("Admin")))
      } else Forbidden
  }

  def rewind() = userAuthAction {
    implicit request ⇒
      if (request.user.isAdmin) {
        rewindRequestForm.bindFromRequest.fold(
          errors ⇒ BadRequest,
          data ⇒ {
            val dateTime = new DateTime(data.destination)
            val resultT = rewindService.refreshState(Some(dateTime))
            resultT match {
              case Failure(th) ⇒
                Logger.error("Error occurred while rewinding the events", th)
                InternalServerError(views.html.errorPage())
              case Success(result) ⇒ Ok
            }
          }
        )
      } else Forbidden
  }
}
