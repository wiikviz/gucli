package ru.sber.cb.ap.gusli.actor.core.project.read

import org.scalatest.FlatSpec
import ru.sber.cb.ap.gusli.actor.projects.read.MetaToChildInheritor

class MetaToChildInheritorSpec extends FlatSpec {
  "MetaToChildInheritor.inheritSetOfLong" should "have distinct values when add equals longs" in {
    val set = MetaToChildInheritor.inheritSetOfLong(Set[Long](1, 2), Some(Set[Long](2, 3)))
    assert(set.contains(1))
    assert(set.contains(2))
    assert(set.contains(3))
  }
  it should "delete values when add negative val" in {
    val set = MetaToChildInheritor.inheritSetOfLong(Set[Long](1, 2), Some(Set[Long](-1, 3)))
    assert(!set.contains(1))
    assert(set.contains(2))
    assert(set.contains(3))
  }
  it should "doesn't break when delete unexistance id" in {
    val set = MetaToChildInheritor.inheritSetOfLong(Set[Long](1, 2), Some(Set[Long](3, -4)))
    assert(set.contains(1))
    assert(set.contains(2))
    assert(set.contains(3))
    assert(!set.contains(4))
  }
  "MetaToChildInheritor.inheritMap" should "delete elems with delete symbol" in {
    val deleteSymbol = "-"
    val map = MetaToChildInheritor.inheritMap(
      Map("i.s" -> "select 1", "i2.s" -> "select 1"),
      Option(Map("i.s" -> deleteSymbol, "i3.s" -> "select 1")))
    assert(!map.contains("i.s"))
    assert(map.contains("i2.s"))
    assert(map.contains("i3.s"))
  }
}
