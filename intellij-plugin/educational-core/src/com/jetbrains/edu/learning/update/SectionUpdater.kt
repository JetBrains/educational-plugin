package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.update.elements.SectionCreationInfo
import com.jetbrains.edu.learning.update.elements.SectionDeletionInfo
import com.jetbrains.edu.learning.update.elements.SectionUpdate
import com.jetbrains.edu.learning.update.elements.SectionUpdateInfo

abstract class SectionUpdater(project: Project, private val course: Course) : StudyItemUpdater<Section, SectionUpdate>(project) {
  protected abstract fun createLessonUpdater(section: Section): LessonUpdater

  suspend fun collect(remoteCourse: Course): List<SectionUpdate> = collect(course.sections, remoteCourse.sections)

  override suspend fun collect(localItems: List<Section>, remoteItems: List<Section>): List<SectionUpdate> {
    val updates = mutableListOf<SectionUpdate>()

    val localSections = localItems.toMutableSet()
    val remoteSections = remoteItems.toMutableSet()

    while (localSections.isNotEmpty() || remoteSections.isNotEmpty()) {
      if (localSections.isEmpty()) {
        // new sections
        remoteSections.forEach { remoteSection ->
          updates.add(SectionCreationInfo(course, remoteSection))
        }
        remoteSections.clear()
      }
      if (remoteSections.isEmpty()) {
        // sections to be deleted
        localSections.forEach { localSection ->
          updates.add(SectionDeletionInfo(localSection))
        }
        localSections.clear()
      }

      // sections to be updated
      val localSection = localSections.firstOrNull() ?: continue
      val remoteSection = remoteSections.find { it.id == localSection.id }
      if (remoteSection == null) {
        updates.add(SectionDeletionInfo(localSection))
        localSections.remove(localSection)
      }
      else {
        val lessonUpdater = createLessonUpdater(localSection)
        val lessonUpdates = lessonUpdater.collect(remoteSection)
        if (lessonUpdates.isNotEmpty() || localSection.isOutdated(remoteSection) || localSection.isChanged(remoteSection)) {
          updates.add(SectionUpdateInfo(localSection, remoteSection, lessonUpdates))
        }

        localSections.remove(localSection)
        remoteSections.remove(remoteSection)
      }
    }

    return updates
  }

  private fun Section.isChanged(remoteSection: Section): Boolean =
    when {
      name != remoteSection.name -> true
      index != remoteSection.index -> true
      lessons.size != remoteSection.lessons.size -> true
      else -> false
    }
}