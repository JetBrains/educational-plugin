package com.jetbrains.edu.learning.update.comparators

class MapComparator<K : Comparable<K>, V>(
  private val valueComparator: Comparator<V>
) : Comparator<Map<K, V>> {
  override fun compare(map1: Map<K, V>?, map2: Map<K, V>?): Int = when {
    map1 === map2 -> 0
    map1 == null -> -1
    map2 == null -> 1
    map1.size != map2.size -> map1.size.compareTo(map2.size)
    else -> compareMapsNonNull(map1, map2)
  }

  private fun compareMapsNonNull(map1: Map<K, V>, map2: Map<K, V>): Int {
    val allKeys = map1.keys union map2.keys
    allKeys.forEach { key ->
      val value1 = map1[key]
      val value2 = map2[key]

      val valueComparison = compareNullable(value1, value2)
      if (valueComparison != 0) return valueComparison
    }
    return 0
  }

  private fun compareNullable(a: V?, b: V?): Int = when {
    a == null && b == null -> 0
    a == null -> -1
    b == null -> 1
    else -> valueComparator.compare(a, b)
  }
}