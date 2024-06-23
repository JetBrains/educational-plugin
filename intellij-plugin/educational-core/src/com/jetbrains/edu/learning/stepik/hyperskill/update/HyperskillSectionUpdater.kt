package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.update.HyperskillItemUpdater
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.SectionUpdater

class HyperskillSectionUpdater(project: Project, course: Course) : SectionUpdater(project, course), HyperskillItemUpdater<Section> {
  override fun createLessonUpdater(section: Section): LessonUpdater = HyperskillLessonUpdater(project, section)
}