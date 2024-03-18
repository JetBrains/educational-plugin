package com.jetbrains.edu.learning

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.jetbrains.edu.coursecreator.CCUtils.isLocalCourse
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.isPreview

abstract class LoginWidgetFactory : StatusBarWidgetFactory {
  protected abstract val widgetId: String

  override fun getId(): String = widgetId

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  override fun isAvailable(project: Project): Boolean {
    return StudyTaskManager.getLightInstance(project).course?.isWidgetAvailable(project) ?: run {
      project.messageBus.connect().subscribe(StudyTaskManager.COURSE_SET,
        object : CourseSetListener {
          override fun courseSet(course: Course) {
            val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
            statusBarWidgetsManager.updateWidget(this@LoginWidgetFactory)
          }
        })
      false
    }
  }

  private fun Course.isWidgetAvailable(project: Project): Boolean {
    return project.isEduProject() && !course.isPreview && !project.isLocalCourse && isWidgetAvailable(course)
  }

  override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

  abstract fun isWidgetAvailable(course: Course): Boolean
}