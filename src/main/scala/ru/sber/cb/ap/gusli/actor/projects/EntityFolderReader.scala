package ru.sber.cb.ap.gusli.actor.projects

import java.nio.file.Path

import akka.actor.{ActorRef, Props}
import ru.sber.cb.ap.gusli.actor.BaseActor
import ru.sber.cb.ap.gusli.actor.core.Entity.{AddChildEntity, EntityCreated}
import ru.sber.cb.ap.gusli.actor.core.EntityMetaDefault
import ru.sber.cb.ap.gusli.actor.projects.EntityFolderReader.ReadEntity
import ru.sber.cb.ap.gusli.actor.projects.EntityFolderResolver.ResolvePath
import ru.sber.cb.ap.gusli.actor.projects.yamlfiles.{YamlEntityMapper, YamlFilePathWorker}
import ru.sber.cb.ap.gusli.actor.projects.yamlfiles.YamlFilePathWorker._

object EntityFolderReader {
  def apply(meta: EntityFolderReaderMeta): Props = Props(new EntityFolderReader(meta))
  
  case class ReadEntity(replyTo: Option[ActorRef] = None)
}

case class EntityFolderReader(meta: EntityFolderReaderMeta) extends BaseActor {
  type Entity = ActorRef
  type Requester = ActorRef
  private val path = this.meta.path
  private val entity = this.meta.entity
  
  override def receive: Receive = {
    case ReadEntity(sender) =>
      println(s"ReadEntity STARTED IN $path with $entity")
      val files: List[Path] = getAllValidEntityChilds(path)
      files.foreach { p =>
        s"FOUND FILE/FOLDER ${p.getFileName}"
        doIfYaml(p)
        doIfFolder(p)
      }
    case EntityCreated(child: Entity) =>
      val pathResolver = context.actorOf(EntityFolderResolver(EntityFolderResolverMetaDefault(path, child)))
      pathResolver ! ResolvePath()
  }
  
  private def doIfYaml(path: Path): Unit = {
    if (isYaml(path)) {
      this.meta.entity ! AddChildEntity(createMetaFromFile(path))
      println(s"SEND AddChildEntity WITH FILE ${path.getFileName}")
    }
  }
  
  private def doIfFolder(path: Path): Unit = {
    if (!isYaml(path)) {
      entity ! AddChildEntity(createMetaFromFolder(path))
      s"SEND AddChildEntity WITH FOLDER ${path.getFileName}"
    }
  }
  
  private def createMetaFromFile(path: Path) = {
    val (id, name) = parseIdAndNameFromYaml(path)
    YamlEntityMapper.read(path, id, name)
  }
  
  private def createMetaFromFolder(path: Path) = {
    val (id, name) = parseIdAndNameFrom(path)
    val entityYaml = path.resolve("entity.yaml")
    if (entityYaml.toFile.exists())
      YamlEntityMapper.read(entityYaml, id, name)
    else
      EntityMetaDefault(id, name, "", YamlFilePathWorker.extractParentIdFromPath(path))
  }
}

trait EntityFolderReaderMeta {
  val path: Path
  val entity: ActorRef
}

case class EntityFolderReaderMetaDefault(path: Path, entity: ActorRef) extends EntityFolderReaderMeta