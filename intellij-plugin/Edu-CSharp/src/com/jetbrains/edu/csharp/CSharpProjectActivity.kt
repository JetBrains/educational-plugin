package com.jetbrains.edu.csharp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.model.MonitoringStartMode
import com.jetbrains.rider.model.dpaModel
import com.jetbrains.rider.projectView.solution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CSharpProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isEduProject() || project.course?.languageId != CSharpLanguage.id) return
    val manager = StudyTaskManager.getInstance(project)
    val connection = ApplicationManager.getApplication().messageBus.connect(manager)
    val listener = CSharpVirtualFileListener(project)
    connection.subscribe(VirtualFileManager.VFS_CHANGES, listener)

    // otherwise monitoring tab will pop up instead of the task description tab
    // each time we check an output task
    withContext(Dispatchers.EDT) {
      project.solution.dpaModel.monitoringStartMode.set(MonitoringStartMode.OnDebug)
    }
  }
}