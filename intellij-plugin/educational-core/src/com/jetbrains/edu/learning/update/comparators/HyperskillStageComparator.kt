package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage

class HyperskillStageComparator private constructor() : Comparator<HyperskillStage> {
  override fun compare(o1: HyperskillStage, o2: HyperskillStage): Int =
    compareBy(HyperskillStage::id, HyperskillStage::title, HyperskillStage::stepId, HyperskillStage::isCompleted).compare(o1, o2)

  companion object {
    infix fun List<HyperskillStage>.areNotEqual(other: List<HyperskillStage>): Boolean =
      UnorderedCollectionComparator(HyperskillStageComparator()).compare(this, other) != 0
  }
}