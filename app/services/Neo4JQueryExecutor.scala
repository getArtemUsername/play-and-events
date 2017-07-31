package services

import org.neo4j.driver.v1._
import org.neo4j.driver.v1.summary.SummaryCounters
import play.api.Configuration
import play.api.{Logger => PlayLogger}

import scala.util.Try


/**
  * Neo4JQueryExecutor class
  */
class Neo4JQueryExecutor(configuration: Configuration) {
  val config = configuration.getConfig("neo4j")
    .getOrElse(throw new Exception("No config element for Neo4J!")).underlying

  val driver = GraphDatabase.driver(config.getString("url"),
    AuthTokens.basic(config.getString("username"), config.getString("password")))

  private def doWithSession[A](block: Session => A): Try[A] = {
    val session = driver.session()
    val resultT = Try {
      block(session)
    }
    session.close()
    resultT
  }

  def executeQuery(query: Neo4JQuery): Try[Seq[Record]] = {
    doWithSession {
      session =>
        val result = session.run(query.query, query.paramsAsJava)
        import collection.JavaConversions._
        result.list().toList
    }
  }

  def executeUpdate(update: Neo4JQuery): Try[SummaryCounters] = {
    doWithSession {
      session =>
        val result = session.run(update.query, update.paramsAsJava)
        val summary = result.consume()
        summary.counters()
    }
  }

  def executeBatch(updates: Seq[Neo4JQuery]): Try[Unit] = {
    val resultT = doWithSession {
      session =>
        val transaction = session.beginTransaction()
        updates.foreach {
          update =>
            transaction.run(update.query, update.paramsAsJava)
        }
        transaction.success()
        transaction.close()
    }
    resultT.recover {
      case th =>
        PlayLogger.error("Error occurred while execution the Neo4J batch", th)
    }
    resultT
  }
}


case class Neo4JQuery(query: String, params: Map[String, AnyRef]) {
  def paramsAsJava: java.util.Map[String, AnyRef] = {
    import collection.JavaConversions._
    mapAsJavaMap(params)
  }
}

object Neo4JQuery {
  def simple(query: String): Neo4JQuery = Neo4JQuery(query, Map.empty[String, AnyRef])
}


case class Neo4JUpdate(queries: Seq[Neo4JQuery])
