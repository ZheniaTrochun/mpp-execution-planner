package com.yevhenii.cluster.planner.server.graphs

import cats.effect.{ContextShift, IO}
import com.mongodb.ConnectionString
import com.softwaremill.tagging.@@
import com.typesafe.config.Config
import com.yevhenii.cluster.planner.server.dto.Task.TaskId
import com.yevhenii.cluster.planner.server.dto.TaskInit
import com.yevhenii.cluster.planner.server.entity.{DataEntity, GraphEntryEntity, GraphsEntity, PositionEntity, TaskEntity}
import com.yevhenii.cluster.planner.server.utils.Tags
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}

import scala.concurrent.ExecutionContext

class MongoTaskRepository(config: Config)(implicit ec: ExecutionContext @@ Tags.Mongo, cs: ContextShift[IO]) extends TaskRepository {

  val codecRegistry = fromRegistries(
    fromProviders(classOf[TaskEntity]),
    fromProviders(classOf[GraphsEntity]),
    fromProviders(classOf[GraphEntryEntity]),
    fromProviders(classOf[DataEntity]),
    fromProviders(classOf[PositionEntity]),
    DEFAULT_CODEC_REGISTRY
  )

  val uri: String = config.getString("mongo.uri")

  val host: String = config.getString("mongo.host")
  val port: Int = config.getInt("mongo.port")
  val databaseName: String = config.getString("mongo.database")
  val user: String = config.getString("mongo.user")
  val password: String = config.getString("mongo.password")

  val credentials = MongoCredential.createCredential(user, databaseName, password.toCharArray)

  val mongoClient: MongoClient = MongoClient(
    MongoClientSettings.builder()
      .applyConnectionString(new ConnectionString(s"mongodb://$host:$port"))
      .credential(credentials)
      .codecRegistry(codecRegistry)
      .build()
  )
  val database: MongoDatabase = mongoClient.getDatabase(databaseName).withCodecRegistry(codecRegistry)

  val tasksCollection: MongoCollection[TaskEntity] = database.getCollection("tasks")
  val graphsCollection: MongoCollection[GraphsEntity] = database.getCollection("graphs")

  override def init(): IO[Unit] = {
    initTask(TaskInit("default"))
      .flatMap(id => IO(println(id)))
  }

  override def initTask(task: TaskInit): IO[TaskId] = {
    createTask(task)
      .flatMap(createDefaultGraphs)
      .map(_.toHexString)
  }

  def createDefaultGraphs(id: ObjectId): IO[ObjectId] = IO.fromFuture {
    IO.apply {
      graphsCollection.insertOne(GraphsEntity(id, List(), List()))
        .toFuture()
        .map(_ => id)
    }
  }

  def createTask(task: TaskInit): IO[ObjectId] = IO.fromFuture {
    val id = new ObjectId()
    IO.apply {
      tasksCollection.insertOne(TaskEntity(id, task.name))
        .toFuture()
        .map(_ => id)
    }
  }

  override def getGraphs(id: TaskId): IO[Option[GraphsEntity]] = IO.fromFuture {
    IO.apply {
      graphsCollection.find(equal("_id", new ObjectId(id)))
        .first()
        .toFutureOption()
    }
  }

  override def deleteTask(id: TaskId): IO[Either[String, Unit]] = {
    deleteGraphsOnly(id).flatMap {
      case Right(_) => deleteTaskOnly(id)
      case err => IO.pure(err)
    }
  }

  def deleteGraphsOnly(id: TaskId): IO[Either[String, Unit]] = IO.fromFuture {
    IO.apply {
      graphsCollection.deleteOne(equal("_id", new ObjectId(id)))
        .toFuture()
        .map { res =>
          if (res.getDeletedCount == 1) Right(())
          else Left(s"There is no graphs for [$id]")
        }
    }
  }

  def deleteTaskOnly(id: TaskId): IO[Either[String, Unit]] = IO.fromFuture {
    IO.apply {
      tasksCollection.deleteOne(equal("_id", new ObjectId(id)))
        .toFuture()
        .map { res =>
          if (res.getDeletedCount == 1) Right(())
          else Left(s"There is no task for [$id]")
        }
    }
  }

  override def updateGraphs(graphs: GraphsEntity): IO[Either[String, Unit]] = IO.fromFuture {
    IO.apply {
      graphsCollection.replaceOne(equal("_id", graphs._id), graphs)
        .toFuture()
        .map { res =>
          if (res.getModifiedCount == 1) Right(())
          else Left(s"There is no graphs for [${graphs._id}]")
        }
    }
  }

  override def getTasks(): IO[List[TaskEntity]] = IO.fromFuture {
    IO.apply {
      tasksCollection.find()
        .toFuture()
        .map(_.toList)
    }
  }
}
