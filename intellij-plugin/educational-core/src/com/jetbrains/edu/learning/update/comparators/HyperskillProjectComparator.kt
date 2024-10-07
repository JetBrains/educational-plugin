package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject

class HyperskillProjectComparator private constructor() : Comparator<HyperskillProject> {
  override fun compare(o1: HyperskillProject, o2: HyperskillProject): Int = when {
    o1.id != o2.id -> o1.id.compareTo(o2.id)
    o1.title != o2.title -> o1.title.compareTo(o2.title)
    o1.description != o2.description -> o1.description.compareTo(o2.description)
    else -> 0
  }

  companion object {
    private fun compare(o1: HyperskillProject, o2: HyperskillProject): Int = HyperskillProjectComparator().compare(o1, o2)

    infix fun HyperskillProject.isNotEqual(other: HyperskillProject): Boolean = compare(this, other) != 0
  }
}