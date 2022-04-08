package com.jetbrains.edu.learning.courseFormat

import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.edu.learning.messages.EduCoreBundle

sealed class CourseVisibility(private val weight: Int) : Comparable<CourseVisibility> {

  open val tooltipText : String? = null
  open val textAttributes: SimpleTextAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES

  object PrivateVisibility : CourseVisibility(0) {
    override val tooltipText = EduCoreBundle.message("course.visibility.private")
  }

  object LocalVisibility : CourseVisibility(3)

  object PublicVisibility : CourseVisibility(4) {
    override val tooltipText = EduCoreBundle.message("course.visibility.public.not.approved")
    override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES
  }

  class FeaturedVisibility(internal val inGroup: Int) : CourseVisibility(1)

  class InProgressVisibility(internal val inGroup: Int) : CourseVisibility(2)

  override fun compareTo(other: CourseVisibility): Int {
    if (weight != other.weight) {
      return weight.compareTo(other.weight)
    }
    if (this is FeaturedVisibility && other is FeaturedVisibility) {
      return inGroup.compareTo(other.inGroup)
    }
    if (this is InProgressVisibility && other is InProgressVisibility) {
      return inGroup.compareTo(other.inGroup)
    }
    return 0
  }
}