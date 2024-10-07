package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic

class HyperskillTopicComparator private constructor() : Comparator<HyperskillTopic> {
  override fun compare(o1: HyperskillTopic, o2: HyperskillTopic): Int =
    compareBy<HyperskillTopic>({ it.id }, { it.title }, { it.theoryId }).compare(o1, o2)

  companion object {
    private fun compareMaps(o1: Map<Int, List<HyperskillTopic>>, o2: Map<Int, List<HyperskillTopic>>): Int {
      val valueComparator = UnorderedCollectionComparator(HyperskillTopicComparator())
      return MapComparator<Int, Collection<HyperskillTopic>>(valueComparator).compare(o1, o2)
    }

    infix fun Map<Int, List<HyperskillTopic>>.areNotEqual(other: Map<Int, List<HyperskillTopic>>): Boolean = compareMaps(this, other) != 0
  }
}