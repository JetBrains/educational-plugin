package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.update.LessonUpdater
import com.jetbrains.edu.learning.update.MarketplaceItemUpdater
import com.jetbrains.edu.learning.update.SectionUpdater

class MarketplaceSectionUpdater(project: Project, course: Course) : SectionUpdater(project, course), MarketplaceItemUpdater<Section> {
  override fun createLessonUpdater(section: Section): LessonUpdater = MarketplaceLessonUpdater(project, section)
}