package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.HyperskillItemUpdater
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.SectionUpdater
import com.jetbrains.edu.learning.update.comparators.HyperskillProjectComparator.Companion.isNotEqual
import com.jetbrains.edu.learning.update.comparators.HyperskillStageComparator.Companion.areNotEqual
import com.jetbrains.edu.learning.update.comparators.HyperskillTopicComparator.Companion.areNotEqual

class HyperskillCourseUpdaterNew(
  project: Project,
  course: HyperskillCourse
) : CourseUpdater<HyperskillCourse>(project, course), HyperskillItemUpdater<HyperskillCourse> {
  override fun createLessonUpdater(container: LessonContainer): LessonUpdater = HyperskillLessonUpdater(project, container)

  override fun createSectionUpdater(course: HyperskillCourse): SectionUpdater = HyperskillSectionUpdater(project, course)

  override fun isCourseChanged(localCourse: HyperskillCourse, remoteCourse: HyperskillCourse): Boolean =
    when {
      localCourse.isChanged(remoteCourse) -> true
      localCourse.isProjectChanged(remoteCourse) -> true
      else -> false
    }

  private fun HyperskillCourse.isProjectChanged(remoteCourse: HyperskillCourse): Boolean {
    val localProject = hyperskillProject ?: error("'hyperskillProject' is not initialized")
    val remoteProject = remoteCourse.hyperskillProject ?: error("'remoteCourse.hyperskillProject' is not initialized")

    val localTopics = taskToTopics
    val remoteTopics = remoteCourse.taskToTopics

    val localStages = stages
    val remoteStages = remoteCourse.stages

    return localProject isNotEqual remoteProject || localTopics areNotEqual remoteTopics || localStages areNotEqual remoteStages
  }
}
