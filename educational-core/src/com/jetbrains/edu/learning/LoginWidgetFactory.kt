package com.jetbrains.edu.learning

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.isPreview

abstract class LoginWidgetFactory : StatusBarWidgetFactory {
  protected abstract val widgetId: String

  override fun getId(): String = widgetId

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  override fun isAvailable(project: Project): Boolean {
    if (!EduUtils.isEduProject(project)) return false
    val course = StudyTaskManager.getInstance(project).course
    return if (course != null && !course.isPreview && !isLocalCourse(project)) {
      isWidgetAvailable(course)
    }
    else false
  }

  override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

  abstract fun isWidgetAvailable(course: Course): Boolean

  private fun isLocalCourse(project: Project): Boolean =
    PropertiesComponent.getInstance(project).getBoolean(CCCreateCoursePreviewDialog.IS_LOCAL_COURSE)
}