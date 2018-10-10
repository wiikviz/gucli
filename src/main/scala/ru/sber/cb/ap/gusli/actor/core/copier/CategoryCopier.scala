package ru.sber.cb.ap.gusli.actor.core.copier

import akka.actor.{ActorRef, Props}
import ru.sber.cb.ap.gusli.actor.core.copier.SingleSubcategoryCopier.SubcategoryCloneSuccessful
import ru.sber.cb.ap.gusli.actor.{BaseActor, Response}

object CategoryCopier {
  def apply(toProject: ActorRef, fromProject: ActorRef, toCategory: ActorRef, fromCategory: ActorRef, receiver: ActorRef): Props =
    Props(new CategoryCopier(toProject, fromProject, toCategory, fromCategory, receiver))

  case class CopiedSuccessfully() extends Response

}

class CategoryCopier(toProject: ActorRef, fromProject: ActorRef, toCategory: ActorRef, fromCategory: ActorRef, receiver: ActorRef) extends BaseActor {

  import CategoryCopier._
  import ru.sber.cb.ap.gusli.actor.core.Category._
  var subCatCount: Int = _

  override def preStart(): Unit = {
    fromCategory ! GetSubcategories()
  }

  override def receive: Receive = {
    case SubcategorySet(set) =>
      if (set.isEmpty) {
        receiver ! CopiedSuccessfully()
        context.stop(self)
      }
      else if (set.nonEmpty) {
        subCatCount = set.size
        for (c <- set) {
          context.actorOf(SingleSubcategoryCopier(toProject, fromProject, toCategory, c, self))
        }
      }
    case SubcategoryCloneSuccessful(cloned, fc) =>
      context.actorOf(CategoryCopier(toProject, fromProject, cloned, fc, self))
    case CopiedSuccessfully() =>
      subCatCount -= 1
      if (subCatCount == 0) {
        receiver ! CopiedSuccessfully()
        context.stop(self)
      }
  }
}