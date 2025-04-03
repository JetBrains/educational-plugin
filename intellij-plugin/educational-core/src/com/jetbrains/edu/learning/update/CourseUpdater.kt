package com.jetbrains.edu.learning.update

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils.getChangeNotesVirtualFile
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.learning.CourseUpdateListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.update.comparators.EduFileComparator.Companion.areNotEqual
import com.jetbrains.edu.learning.update.elements.CourseUpdate
import com.jetbrains.edu.learning.update.elements.StudyItemUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class CourseUpdater<T : Course>(val project: Project, private val localCourse: T) : ItemUpdater<T> {
  protected abstract fun createLessonUpdater(container: LessonContainer): LessonUpdater
  protected abstract fun createSectionUpdater(course: T): SectionUpdater

  suspend fun collect(remoteCourse: T): Collection<StudyItemUpdate<StudyItem>> {
    val updates = mutableListOf<StudyItemUpdate<StudyItem>>()

    val sectionUpdater = createSectionUpdater(localCourse)
    val sectionUpdates = sectionUpdater.collect(remoteCourse)
    updates.addAll(sectionUpdates)

    val lessonUpdater = createLessonUpdater(localCourse)
    val lessonUpdates = lessonUpdater.collect(remoteCourse)
    updates.addAll(lessonUpdates)

    if (updates.isNotEmpty() || localCourse.isOutdated(remoteCourse) || isCourseChanged(localCourse, remoteCourse)) {
      // If any changes are detected in the course items, keep in mind that sorting may be required
      updates.add(CourseUpdate.get(localCourse, remoteCourse))
    }

    return updates
  }

  suspend fun update(remoteCourse: T) {
    val updates = collect(remoteCourse)
    updates.forEach {
      it.update(project)
    }

    showUpdateNotification()
    withContext(Dispatchers.EDT) {
      if (project.isDisposed) return@withContext
      val course = project.course ?: return@withContext
      project.messageBus.syncPublisher(CourseUpdateListener.COURSE_UPDATE).courseUpdated(project, course)
    }
  }

  private fun showUpdateNotification() {
    val changeNotesVirtualFile = getChangeNotesVirtualFile(project)
    val openChangeNotesAction = if (changeNotesVirtualFile == null || changeNotesVirtualFile.length <= 0) {
      null
    }
    else {
      object : AnAction(EduCoreBundle.message("marketplace.see.whats.new")) {
        override fun actionPerformed(e: AnActionEvent) {
          FileEditorManager.getInstance(project).openFile(changeNotesVirtualFile, true)
        }
      }
    }
    CCNotificationUtils.showInfoNotification(project, EduCoreBundle.message("action.course.updated"), action = openChangeNotesAction)
  }

  abstract fun isCourseChanged(localCourse: T, remoteCourse: T): Boolean

  protected fun T.isChanged(remoteCourse: T): Boolean =
    when {
      name != remoteCourse.name -> true
      additionalFiles areNotEqual remoteCourse.additionalFiles -> true
      else -> false
    }
}