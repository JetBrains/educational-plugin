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
    val keys1 = map1.keys
    val keys2 = map2.keys

    val keyComparison = keys1.toString().compareTo(keys2.toString())
    if (keyComparison != 0) return keyComparison

    for (key in keys1) {
      val valueComparison = valueComparator.compare(map1[key], map2[key])
      if (valueComparison != 0) return valueComparison
    }
    return 0
  }
}