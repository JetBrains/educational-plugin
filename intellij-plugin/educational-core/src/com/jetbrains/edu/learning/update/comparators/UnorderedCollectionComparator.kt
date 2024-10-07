package com.jetbrains.edu.learning.update.comparators

class UnorderedCollectionComparator<T>(private val elementComparator: Comparator<T>) : Comparator<Collection<T>> {
  override fun compare(o1: Collection<T>, o2: Collection<T>): Int {
    if (o1.size != o2.size) return o1.size.compareTo(o2.size)

    val sorted1 = o1.sortedWith(elementComparator)
    val sorted2 = o2.sortedWith(elementComparator)

    sorted1.zip(sorted2).forEach { (e1, e2) ->
      val comparison = elementComparator.compare(e1, e2)
      if (comparison != 0) return comparison
    }

    return 0
  }
}