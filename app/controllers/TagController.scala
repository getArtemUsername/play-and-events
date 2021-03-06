package controllers

import java.util.UUID

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import security.UserAuthAction
import services.{ReadService, TagEventProducer}

import scala.util.{Failure, Success}

/**
  *
  * TagController class
  * <p/>
  * Description...
  *
  * @author artem klevakin
  */
class TagController(tagEventProducer: TagEventProducer,
                    userAuthAction: UserAuthAction,
                    readService: ReadService) extends Controller {

  case class CreateTagData(text: String)

  case class DeleteTagData(id: UUID)

  import play.api.data.Forms._

  val createTagForm = Form {
    mapping(
      "text" -> nonEmptyText
    )(CreateTagData.apply)(CreateTagData.unapply)
  }
  val deleteTagForm = Form {
    mapping(
      "id" -> uuid
    )(DeleteTagData.apply)(DeleteTagData.unapply)
  }

  def getTags: Action[AnyContent] = Action {
    val tagsT = readService.getAllTags
    tagsT match  {
      case Failure(th) => InternalServerError
      case Success(tags) => Ok(Json.toJson(tags))
    }
  }


  def createTag(): Action[AnyContent] = userAuthAction {
    implicit request =>
      createTagForm.bindFromRequest.fold(
        formWithErrors => BadRequest,
        data => {
          tagEventProducer.createTag(data.text, request.user.userId)
          Ok
        }
      )
  }

  def deleteTag(): Action[AnyContent]  = userAuthAction {
    implicit request =>
      deleteTagForm.bindFromRequest.fold(
        formWithErrors => BadRequest,
        data => {
          tagEventProducer.deleteTag(data.id, request.user.userId)
          Ok
        }
      )
  }
}
