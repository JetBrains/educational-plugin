package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.EduFile

class EduFileComparator private constructor(): Comparator<EduFile> {
  override fun compare(o1: EduFile?, o2: EduFile?): Int = when {
    o1 === o2 -> 0
    o1 == null -> -1
    o2 == null -> 1
    o1.name != o2.name -> o1.name.compareTo(o2.name)
    o1.contents.textualRepresentation != o2.contents.textualRepresentation -> {
      o1.contents.textualRepresentation.compareTo(o2.contents.textualRepresentation)
    }
    else -> 0
  }

  companion object {
    infix fun List<EduFile>.areNotEqual(other: List<EduFile>): Boolean = SetComparator(EduFileComparator()).compare(this, other) != 0
  }
}