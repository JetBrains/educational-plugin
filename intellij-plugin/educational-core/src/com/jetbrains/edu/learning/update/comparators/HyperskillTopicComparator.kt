package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic

class HyperskillTopicComparator private constructor() : Comparator<HyperskillTopic> {
  override fun compare(o1: HyperskillTopic?, o2: HyperskillTopic?): Int = when {
    o1 === o2 -> 0
    o1 == null -> -1
    o2 == null -> 1
    o1.id != o2.id -> o1.id.compareTo(o2.id)
    o1.title != o2.title -> o1.title.compareTo(o2.title)
    o1.theoryId != o2.theoryId -> compareNullable(o1.theoryId, o2.theoryId)
    else -> 0
  }

  private fun <T : Comparable<T>> compareNullable(a: T?, b: T?): Int = when {
    a == null && b == null -> 0
    a == null -> -1
    b == null -> 1
    else -> a.compareTo(b)
  }

  companion object {
    private fun compareMaps(o1: Map<Int, List<HyperskillTopic>>, o2: Map<Int, List<HyperskillTopic>>): Int {
      val listComparator = ListComparator(HyperskillTopicComparator())
      return MapComparator<Int, List<HyperskillTopic>>(listComparator).compare(o1, o2)
    }

    infix fun Map<Int, List<HyperskillTopic>>.areNotEqual(other: Map<Int, List<HyperskillTopic>>): Boolean = compareMaps(this, other) != 0
  }
}