import com.softwaremill.macwire._
import controllers.AdminController
import controllers.AnswerController
import controllers.Assets
import controllers.AuthController
import controllers.MainController
import controllers.QuestionController
import controllers.TagController
import dao._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.routing.Router
import router.Routes
import scalikejdbc.config.DBs
import security.{UserAuthAction, UserAwareAction}
import services._

import scala.concurrent.Future

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach { configurator =>
      configurator.configure(context.environment)
    }
    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}

trait AppComponents extends BuiltInComponents
  with EvolutionsComponents with DBComponents with HikariCPComponents {
  lazy val assets: Assets = wire[Assets]
  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]
  lazy val maybeRouter = Option(router)
  override lazy val httpErrorHandler = wire[ProdErrorHandler]

  lazy val mainController = wire[MainController]
  lazy val authController = wire[AuthController]
  lazy val tagController = wire[TagController]
  lazy val eventValidator = wire[EventValidator]
  lazy val validationService = wire[ValidationService]

  lazy val neo4JReadDao = wire[Neo4JReadDao]
  lazy val neo4JQueryExecutor = wire[Neo4JQueryExecutor]

  lazy val tagEventConsumer = wire[TagEventConsumer]
  lazy val logRecordConsumer = wire[LogRecordConsumer]
  lazy val userEventConsumer = wire[UserEventConsumer]
  lazy val consumerAggregator = wire[ConsumerAggregator]

  lazy val logDao = wire[LogDao]
  lazy val sessionDao = wire[SessionDao]

  lazy val userDao = wire[UserDao]
  lazy val userService = wire[UserService]

  lazy val authService = wire[AuthService]
  lazy val userAuthAction = wire[UserAuthAction]
  lazy val userAwareAction = wire[UserAwareAction]
  lazy val readService = wire[ReadService]
  lazy val tagEventProducer = wire[TagEventProducer]
  lazy val userEventProducer = wire[UserEventProducer]

  lazy val rewindService = wire[RewindService]
  lazy val questionController = wire[QuestionController]
  lazy val questionEventProducer = wire[QuestionEventProducer]
  lazy val questionEventConsumer = wire[QuestionEventConsumer]
  
  lazy val answerController = wire[AnswerController]
  lazy val answerEventProducer = wire[AnswerEventProducer]
  lazy val answerEventConsumer = wire[AnswerEventConsumer]
  
  lazy val adminController = wire[AdminController]

  override lazy val dynamicEvolutions = new DynamicEvolutions

  applicationLifecycle.addStopHook { () =>
    DBs.closeAll()
    Future.successful(Unit)
  }

  val onStart = {
    DBs.setupAll()
    applicationEvolutions
  }
}
