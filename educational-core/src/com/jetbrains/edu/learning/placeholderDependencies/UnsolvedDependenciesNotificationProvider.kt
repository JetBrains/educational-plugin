package com.jetbrains.edu.learning.placeholderDependencies

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotificationProvider.CONST_NULL
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.getUnsolvedTaskDependencies
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.util.function.Function
import javax.swing.JComponent

class UnsolvedDependenciesNotificationProvider : EditorNotificationProvider, DumbAware {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    if (!EduUtils.isStudentProject(project)) {
      return CONST_NULL
    }
    val task = file.getContainingTask(project) ?: return CONST_NULL
    val taskDependencies = task.getUnsolvedTaskDependencies().sortedBy { it.index }
    if (taskDependencies.isEmpty()) {
      return CONST_NULL
    }

    return Function {
      val panel = EditorNotificationPanel()
      panel.text = getText(taskDependencies.map { it.name })
      panel.createActionLabel(EduCoreBundle.message("action.solve.task.text", taskDependencies[0].name)) {
        NavigationUtils.navigateToTask(project, taskDependencies[0], task)
        EduCounterUsageCollector.taskNavigation(EduCounterUsageCollector.TaskNavigationPlace.UNRESOLVED_DEPENDENCY_NOTIFICATION)
      }
      panel
    }
  }

  companion object {
    @VisibleForTesting
    fun getText(taskNames: List<String>): String {
      val taskNamesString = taskNames.joinToString(separator = ", ") { "'$it'" }
      return "${StringUtil.pluralize("Task", taskNames.size)} $taskNamesString should be solved first"
    }
  }
}
