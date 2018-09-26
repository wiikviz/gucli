package ru.sber.cb.ap.gusli.actor.projects.read.category.create

import java.nio.file.Path

import akka.actor.{ActorRef, Props}
import ru.sber.cb.ap.gusli.actor.core.Category.{AddWorkflow, CategoryMetaResponse, GetCategoryMeta, WorkflowCreated}
import ru.sber.cb.ap.gusli.actor.core.{CategoryMeta, WorkflowMeta}
import ru.sber.cb.ap.gusli.actor.core.Workflow.BindEntity
import ru.sber.cb.ap.gusli.actor.core.dto.WorkflowDto
import ru.sber.cb.ap.gusli.actor.projects.read.category.ProjectMetaMaker
import ru.sber.cb.ap.gusli.actor.projects.read.category.create.WorkflowCreatorByFolder.ReadWorkflowFolder
import ru.sber.cb.ap.gusli.actor.projects.yamlfiles.{WorkflowFileFields, WorkflowOptionDto, YamlFileMapper}
import ru.sber.cb.ap.gusli.actor.{BaseActor, Request, Response}

object WorkflowCreatorByFolder {
  def apply(meta: WorkflowCreatorByFolderMeta): Props = Props(new WorkflowCreatorByFolder(meta))
  
  case class ReadWorkflowFolder(replyTo: Option[ActorRef] = None) extends Request
  
  case class WorkflowRead(replyTo: Option[ActorRef] = None) extends Response
  
}

class WorkflowCreatorByFolder(meta: WorkflowCreatorByFolderMeta) extends BaseActor {
  private val entities = scala.collection.mutable.ArrayBuffer[Long]()
  
  override def receive: Receive = {
    case ReadWorkflowFolder(replyTo) => this.meta.category ! GetCategoryMeta()
    case CategoryMetaResponse(meta) =>
      tryCreateWorkflow(meta)
    case WorkflowCreated(wf) => entities.foreach(wf ! BindEntity(_))
  }
  
  private def tryCreateWorkflow(meta: CategoryMeta): Unit = {
    val wfMetaTemp = extractMetaFileFields(meta)
    if (wfMetaTemp.isEmpty) Left("Meta File not found in " + this.meta.path)
    else {
      //TODO: Фильтр отрицательных сущностей
      entities ++= meta.entities
      this.meta.category ! AddWorkflow(inheritMeta(meta, wfMetaTemp))
    }
  }
  
  private def extractMetaFileFields(meta: CategoryMeta) = YamlFileMapper.readToWorkflowOptionDto(this.meta.path)
  
  private def inheritMeta(meta: CategoryMeta, wfMetaTemp: Option[WorkflowOptionDto]) =
    ProjectMetaMaker.workflowNonEmptyMeta(meta, wfMetaTemp.get)
  
}

trait WorkflowCreatorByFolderMeta {
  val path: Path
  val category: ActorRef
}

case class WorkflowCreatorByFolderMetaDefault(path: Path, category: ActorRef) extends WorkflowCreatorByFolderMeta
    