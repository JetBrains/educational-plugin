package com.jetbrains.edu.learning.courseFormat

sealed class CourseVisibility(private val weight: Int) : Comparable<CourseVisibility> {

  class FeaturedVisibility(internal val inGroup: Int) : CourseVisibility(1)
  data object LocalVisibility : CourseVisibility(2)
  data object PublicVisibility : CourseVisibility(3)
  data object PrivateVisibility : CourseVisibility(4)

  override fun compareTo(other: CourseVisibility): Int {
    if (weight != other.weight) {
      return weight.compareTo(other.weight)
    }
    if (this is FeaturedVisibility && other is FeaturedVisibility) {
      return inGroup.compareTo(other.inGroup)
    }
    return 0
  }
}
