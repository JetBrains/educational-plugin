package com.jetbrains.edu.learning.update.comparators

class ListComparator<T>(private val elementComparator: Comparator<T>) : Comparator<List<T>> {
  override fun compare(o1: List<T>, o2: List<T>): Int {
    if (o1.size != o2.size) return o1.size.compareTo(o2.size)
    o1.indices.forEach { i ->
      val comparison = elementComparator.compare(o1[i], o2[i])
      if (comparison != 0) return comparison
    }
    return 0
  }
}