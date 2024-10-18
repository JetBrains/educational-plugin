package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.marketplace.update.elements.MarketplaceCourseUpdate
import com.jetbrains.edu.learning.stepik.hyperskill.update.elements.HyperskillCourseUpdate
import com.jetbrains.edu.learning.update.comparators.EduFileComparator.Companion.areNotEqual

abstract class CourseUpdate<T : Course>(
  override val localItem: T,
  override val remoteItem: T
) : StudyItemUpdate<T>(localItem, remoteItem) {
  protected suspend fun baseUpdate(project: Project) {
    localItem.name = remoteItem.name
    localItem.description = remoteItem.description

    if (localItem.additionalFiles areNotEqual remoteItem.additionalFiles) {
      val baseDir = project.courseDir

      writeAction {
        localItem.additionalFiles.forEach { file ->
          baseDir.findChild(file.name)?.delete(this) ?: LOG.warn("${file.name} wasn't found and deleted")
        }
      }

      blockingContext {
        remoteItem.additionalFiles.forEach { file ->
          GeneratorUtils.createChildFile(project, baseDir, file.name, file.contents)
        }
      }

      localItem.additionalFiles = remoteItem.additionalFiles
    }

    localItem.sortItems()
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CourseUpdate::class.java)

    fun <T : Course> get(localCourse: T, remoteCourse: T): CourseUpdate<out Course> = when {
      localCourse is EduCourse && remoteCourse is EduCourse -> MarketplaceCourseUpdate(localCourse, remoteCourse)
      localCourse is HyperskillCourse && remoteCourse is HyperskillCourse -> HyperskillCourseUpdate(localCourse, remoteCourse)
      else -> error("Unsupported course types: local=${localCourse::class.simpleName}, remote=${remoteCourse::class.simpleName}")
    }
  }
}
