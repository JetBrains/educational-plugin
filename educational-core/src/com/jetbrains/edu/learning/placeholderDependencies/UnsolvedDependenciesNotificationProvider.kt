package com.jetbrains.edu.learning.placeholderDependencies

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.getUnsolvedTaskDependencies
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

class UnsolvedDependenciesNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (!EduUtils.isStudentProject(project)) {
      return null
    }
    val task = file.getContainingTask(project) ?: return null
    val taskDependencies = task.getUnsolvedTaskDependencies().sortedBy { it.index }
    if (taskDependencies.isEmpty()) {
      return null
    }
    val panel = EditorNotificationPanel()
    panel.text = getText(taskDependencies.map { it.name })
    panel.createActionLabel("Solve '${taskDependencies[0].name}'") {
      NavigationUtils.navigateToTask(project, taskDependencies[0], task)
      EduCounterUsageCollector.taskNavigation(EduCounterUsageCollector.TaskNavigationPlace.UNRESOLVED_DEPENDENCY_NOTIFICATION)
    }
    return panel
  }

  companion object {
    val KEY: Key<EditorNotificationPanel> = Key.create("Edu.unsolvedDependencies")

    @VisibleForTesting
    fun getText(taskNames: List<String>): String {
      val taskNamesString = taskNames.joinToString(separator = ", ") { "'$it'" }
      return "${StringUtil.pluralize("Task", taskNames.size)} $taskNamesString should be solved first"
    }
  }
}
