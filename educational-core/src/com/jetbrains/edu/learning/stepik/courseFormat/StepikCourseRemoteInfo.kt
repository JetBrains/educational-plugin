package com.jetbrains.edu.learning.stepik.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Tag
import com.jetbrains.edu.learning.courseFormat.remote.CourseRemoteInfo
import java.util.*

class StepikCourseRemoteInfo : CourseRemoteInfo {
  // publish to stepik
  var isPublic: Boolean = false
  var isAdaptive = false
  var isIdeaCompatible = true
  var id: Int = 0
  var updateDate = Date(0)
  var sectionIds: MutableList<Int> = mutableListOf() // in CC mode is used to store top-level lessons section id
  var instructors: MutableList<Int> = mutableListOf()
  var additionalMaterialsUpdateDate = Date(0)

  // do not publish to stepik
  var loadSolutions = true // disabled for reset courses

  override fun isCourseValid(course: Course): Boolean {
    if (!isAdaptive) return true
    val lessons = course.lessons
    if (lessons.size == 1) {
      return !lessons[0].getTaskList().isEmpty()
    }
    return true
  }

  override fun getTags(): List<Tag> =
    if (isAdaptive) {
      listOf(Tag(EduNames.ADAPTIVE))
    }
    else listOf()
}
