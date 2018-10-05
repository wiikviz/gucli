package ru.sber.cb.ap.gusli.actor.projects.write

import java.nio.file.Path

import akka.actor.Props
import ru.sber.cb.ap.gusli.actor.BaseActor
import ru.sber.cb.ap.gusli.actor.core.CategoryMeta
import ru.sber.cb.ap.gusli.actor.core.Workflow.WorkflowMetaResponse
import ru.sber.cb.ap.gusli.actor.core.extractor.WorkflowDtoExtractor
import ru.sber.cb.ap.gusli.actor.core.extractor.WorkflowDtoExtractor.WorkflowExtracted

object WorkflowWriter {
  def apply(path: Path, parentMeta: CategoryMeta): Props = Props(new WorkflowWriter(path, parentMeta))
}

class WorkflowWriter(path: Path, categoryMeta: CategoryMeta) extends BaseActor {
  
  override def receive: Receive = {
    case WorkflowMetaResponse(workflowMeta) =>
      context.actorOf(WorkflowDtoExtractor(sender, context.self))
    
    case WorkflowExtracted(workflowDto) =>
      MetaToHDD.writeWorkflowMetaToPath(workflowDto, path, categoryMeta)
  }
}
