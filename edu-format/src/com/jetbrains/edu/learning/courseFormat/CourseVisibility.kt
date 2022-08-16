package com.jetbrains.edu.learning.courseFormat

sealed class CourseVisibility(private val weight: Int) : Comparable<CourseVisibility> {

  object PrivateVisibility : CourseVisibility(0)

  object LocalVisibility : CourseVisibility(3)

  object PublicVisibility : CourseVisibility(4)

  class FeaturedVisibility(internal val inGroup: Int) : CourseVisibility(1)

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