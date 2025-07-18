package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.update.StudyItemUpdater.Companion.deleteFilesOnDisc
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SectionUpdate(localItem: Section?, remoteItem: Section?) : StudyItemUpdate<Section>(localItem, remoteItem)

data class SectionCreationInfo(
  val localCourse: Course,
  override val remoteItem: Section
) : SectionUpdate(null, remoteItem) {
  override suspend fun update(project: Project) {
    localCourse.addItem(remoteItem)
    remoteItem.init(localCourse, false)

    val parentDir = localCourse.getDir(project.courseDir) ?: error("Failed to find parent dir: ${localCourse.name}")
    withContext(Dispatchers.IO) {
      GeneratorUtils.createSection(project, remoteItem, parentDir)
    }

    YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
  }
}

data class SectionUpdateInfo(
  override val localItem: Section,
  override val remoteItem: Section,
  val lessonUpdates: List<LessonUpdate>
) : SectionUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    if (lessonUpdates.isNotEmpty()) {
      lessonUpdates.forEach { it.update(project) }
      localItem.sortItems()
    }

    localItem.index = remoteItem.index

    if (localItem.name != remoteItem.name) {
      val courseDir = project.courseDir
      val fromDir = localItem.getDir(courseDir) ?: error("Section dir wasn't found")

      localItem.name = remoteItem.name
      withContext(Dispatchers.IO) {
        val toDir = GeneratorUtils.createUniqueDir(courseDir, localItem)
        writeAction {
          fromDir.children.forEach { it.move(this, toDir) }
          fromDir.delete(this)
        }
      }

      YamlFormatSynchronizer.saveItemWithRemoteInfo(localItem)
    }

  }
}

data class SectionDeletionInfo(override val localItem: Section) : SectionUpdate(localItem, null) {
  override suspend fun update(project: Project) {
    val course = localItem.parent
    course.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)
  }
}