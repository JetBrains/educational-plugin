package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage

class HyperskillStageComparator private constructor() : Comparator<HyperskillStage> {
  override fun compare(o1: HyperskillStage, o2: HyperskillStage): Int = when {
    o1.id != o2.id -> o1.id.compareTo(o2.id)
    o1.title != o2.title -> o1.title.compareTo(o2.title)
    o1.stepId != o2.stepId -> o1.stepId.compareTo(o2.stepId)
    o1.isCompleted != o2.isCompleted -> o1.isCompleted.compareTo(o2.isCompleted)
    else -> 0
  }

  companion object {
    infix fun List<HyperskillStage>.areNotEqual(other: List<HyperskillStage>): Boolean =
      SetComparator(HyperskillStageComparator()).compare(this, other) != 0
  }
}