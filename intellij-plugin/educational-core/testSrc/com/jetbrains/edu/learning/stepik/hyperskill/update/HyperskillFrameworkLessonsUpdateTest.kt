package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.FrameworkLessonsUpdateTest

class HyperskillFrameworkLessonsUpdateTest : FrameworkLessonsUpdateTest<HyperskillCourse>() {

  override fun produceCourse(): HyperskillCourse = HyperskillCourse()

  override fun setupLocalCourse(course: HyperskillCourse) {
    course.hyperskillProject = HyperskillProject().apply {
      title = course.name
      description = course.description
    }
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2), HyperskillStage(3, "", 3))
  }

  override fun getUpdater(localCourse: HyperskillCourse): CourseUpdater<HyperskillCourse> = HyperskillCourseUpdaterNew(project, localCourse)
}
