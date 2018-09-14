package ru.sber.cb.ap.gusli.actor.core

import akka.actor.{ActorRef, Props}
import ru.sber.cb.ap.gusli.actor._
import ru.sber.cb.ap.gusli.actor.core.Entity._
import ru.sber.cb.ap.gusli.actor.core.search.EntityCollector
import ru.sber.cb.ap.gusli.actor.core.search.EntityCollector.GetAllSubEntities

import scala.collection.immutable.HashMap

object Entity {
  def apply(meta: EntityMeta): Props = Props(new Entity(meta))
  
  case class GetEntityMeta(replyTo: Option[ActorRef] = None) extends Request
  
  case class AddChildEntity(meta: EntityMeta, replyTo: Option[ActorRef] = None) extends Request

  case class GetChildren(replyTo: Option[ActorRef] = None) extends Request

  case class GetAllChildren(replyTo: Option[ActorRef] = None) extends Request

  case class EntityMetaResponse(id: Long, name: String, path: String) extends Response with EntityMeta
  
  case class EntityCreated(actorRef: ActorRef) extends ActorResponse

  case class ChildrenEntityList(actorList: Seq[ActorRef]) extends ActorListResponse
}

case class Entity(meta: EntityMeta) extends BaseActor {
  private var children: HashMap[Long, ActorRef] = HashMap.empty[Long, ActorRef]

  override def receive: Receive = {
    case m @ GetEntityMeta(sendTo) => sendEntityMeta(sendTo)
    case m @ AddChildEntity(meta, sendTo) =>
      log.info("{}", m)
      val replyTo = sendTo getOrElse sender
      val fromRegisty = children get meta.id
      if(fromRegisty isEmpty){
        val newEntity = context actorOf Entity(meta)
        children = children + (meta.id -> newEntity)
        replyTo ! EntityCreated(newEntity)
      } else replyTo ! EntityCreated(fromRegisty.get)
    case GetChildren(sendTo) =>
      sendTo getOrElse sender ! ChildrenEntityList(children.values.toSeq)
    case m @ GetAllChildren(sendTo) =>
      context.actorOf(EntityCollector(children.values.toList)) ! GetAllSubEntities(sendTo)
  }
  
  private def sendEntityMeta(sendTo: Option[ActorRef]) = {
    sendTo getOrElse sender ! EntityMetaResponse(meta.id, meta.name, meta.path)
  }
}

trait EntityMeta {
  def id: Long
  def name: String
  def path: String
}

case class EntityMetaDefault(id: Long, name: String, path: String) extends EntityMeta