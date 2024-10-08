package com.jetbrains.edu.learning.update.comparators

import com.jetbrains.edu.learning.courseFormat.EduFile

class EduFileComparator private constructor(): Comparator<EduFile> {
  override fun compare(o1: EduFile, o2: EduFile): Int = compareBy(EduFile::name, { it.contents.textualRepresentation }).compare(o1, o2)

  companion object {
    infix fun List<EduFile>.areNotEqual(other: List<EduFile>): Boolean =
      UnorderedCollectionComparator(EduFileComparator()).compare(this, other) != 0
  }
}