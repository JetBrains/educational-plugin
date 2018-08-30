package com.jetbrains.edu.jbserver

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.ActionCallback
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse

class CourseUpdateNotifier(parentDisposable: Disposable): IntervalNotifier(parentDisposable) {

  override fun action(): ActionCallback {
    val callback = ActionCallback()
    ApplicationManager.getApplication().executeOnPooledThread {
      ProjectManager.getInstance().openProjects.forEach {
        if (!EduUtils.isStudyProject(it)) return@forEach
        val course = StudyTaskManager.getInstance(it).course as? EduCourse ?: return@forEach
        checkUpdate(course)
      }
      callback.setDone()
    }
    return callback
  }

}