package com.jetbrains.edu.learning.update.comparators

class ListComparator<T>(private val elementComparator: Comparator<T>) : Comparator<List<T>> {
  override fun compare(o1: List<T>, o2: List<T>): Int {
    if (o1.size != o2.size) return o1.size.compareTo(o2.size)
    o1.zip(o2).forEach { (e1, e2) ->
      val comparison = elementComparator.compare(e1, e2)
      if (comparison != 0) return comparison
    }
    return 0
  }
}