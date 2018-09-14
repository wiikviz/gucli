package ru.sber.cb.ap.gusli.actor.core

import akka.actor.{ActorRef, Props}
import ru.sber.cb.ap.gusli.actor.core.Project.{EntityFound, EntityNotFound, FindEntity}
import ru.sber.cb.ap.gusli.actor.{ActorListResponse, BaseActor, Request, Response}

import scala.collection.immutable
import scala.collection.immutable.HashMap

object Workflow {
  def apply(meta: WorkflowMeta, project: ActorRef): Props = Props(new Workflow(meta, project))

  case class GetWorkflowMeta(replyTo: Option[ActorRef] = None) extends Request

  case class ListEntities(replyTo: Option[ActorRef] = None) extends Request

  case class BindEntity(entityId: Long, replyTo: Option[ActorRef] = None) extends Request



  case class BindEntitySuccessful(entityId: Long) extends Response

  case class BindEntityFailedBecauseItNotExists(entityId: Long) extends Response

  case class EntityList(actorList: Seq[ActorRef]) extends ActorListResponse

  case class WorkflowMetaResponse(name: String, sqlFile: String) extends Response with WorkflowMeta

}

class Workflow(meta: WorkflowMeta, project: ActorRef) extends BaseActor {

  import Workflow._

  var awaitEntityBind: Map[Long, immutable.List[ActorRef]] = HashMap.empty[Long, List[ActorRef]]
  var boundEntitySet: Set[ActorRef] = Set.empty[ActorRef]

  override def receive: Receive = {
    case GetWorkflowMeta(sendTo) => sendTo.getOrElse(sender) ! WorkflowMetaResponse(meta.name, meta.sqlFile)

    case BindEntity(entityId, sendTo) =>
      val replyTo = sendTo.getOrElse(sender)
      val refs = awaitEntityBind.getOrElse(entityId, List[ActorRef]()).::(replyTo)

      awaitEntityBind = awaitEntityBind + (entityId -> refs)
      project ! FindEntity(entityId)

    case EntityNotFound(entityId) =>
      val refs = awaitEntityBind.getOrElse(entityId, List[ActorRef]())
      for (a <- refs)
        a ! BindEntityFailedBecauseItNotExists(entityId)

      awaitEntityBind = awaitEntityBind.filterKeys(id => id != entityId)

    case EntityFound(meta, entityRef) =>
      boundEntitySet = boundEntitySet + entityRef
      awaitEntityBind getOrElse(meta.id, Nil) foreach (_ ! BindEntitySuccessful(meta.id))
      awaitEntityBind = awaitEntityBind - meta.id

    case ListEntities(sendTo) =>
      sendTo getOrElse sender ! EntityList(boundEntitySet.toSeq)

  }
}

trait WorkflowMeta {
  def name: String

  def sqlFile: String
}

case class WorkflowMetaDefault(name: String, sqlFile: String) extends WorkflowMeta