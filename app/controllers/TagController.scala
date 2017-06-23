package controllers

import java.util.UUID

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import security.UserAuthAction
import services.{ReadService, TagEventProducer}

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

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  def getTags = Action.async {
    val tagsF = readService.getTags
    tagsF map { tags => Ok(Json.toJson(tags)) }
  }


  def createTag() = userAuthAction.async {
    implicit request =>
      createTagForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest),
        data => {
          tagEventProducer.createTag(data.text, request.user.userId) map {
            tags => Ok(Json.toJson(tags))
          }
        }
      )
  }

  def deleteTag() = userAuthAction.async {
    implicit request =>
      deleteTagForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest),
        data => {
          tagEventProducer.deleteTag(data.id, request.user.userId) map {
            tags => Ok(Json.toJson(tags))
          }
        }
      )
  }
}
