package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.MarketplaceItemUpdater
import com.jetbrains.edu.learning.update.SectionUpdater

class MarketplaceCourseUpdaterNew(
  project: Project,
  course: EduCourse
) : CourseUpdater<EduCourse>(project, course), MarketplaceItemUpdater<EduCourse> {
  override fun createLessonUpdater(container: LessonContainer): LessonUpdater = MarketplaceLessonUpdater(project, container)

  override fun createSectionUpdater(course: EduCourse): SectionUpdater = MarketplaceSectionUpdater(project, course)

  override fun isCourseChanged(localCourse: EduCourse, remoteCourse: EduCourse): Boolean = when {
    localCourse.marketplaceCourseVersion != remoteCourse.marketplaceCourseVersion -> true
    localCourse.isChanged(remoteCourse) -> true
    else -> false
  }
}