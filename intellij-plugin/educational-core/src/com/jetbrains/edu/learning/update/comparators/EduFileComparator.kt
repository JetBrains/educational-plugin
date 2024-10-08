package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.EduFile

class EduFileComparator private constructor(): Comparator<EduFile> {
  override fun compare(o1: EduFile?, o2: EduFile?): Int = when {
    o1 === o2 -> 0
    o1 == null -> -1
    o2 == null -> 1
    o1.name != o2.name -> o1.name.compareTo(o2.name)
    o1.text != o2.text -> o1.text.compareTo(o2.name)
    else -> 0
  }

  companion object {
    infix fun List<EduFile>.areNotEqual(other: List<EduFile>): Boolean = ListComparator(EduFileComparator()).compare(this, other) != 0
  }
}