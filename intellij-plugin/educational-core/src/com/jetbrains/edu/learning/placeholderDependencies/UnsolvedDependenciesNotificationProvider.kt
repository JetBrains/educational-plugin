package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.ext.getUnsolvedTaskDependencies
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.VisibleForTesting
import java.util.function.Function
import javax.swing.JComponent

class UnsolvedDependenciesNotificationProvider : EditorNotificationProvider, DumbAware {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isStudentProject()) {
      return null
    }
    val task = file.getContainingTask(project) ?: return null
    val taskDependencies = task.getUnsolvedTaskDependencies().sortedBy { it.index }
    if (taskDependencies.isEmpty()) {
      return null
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
