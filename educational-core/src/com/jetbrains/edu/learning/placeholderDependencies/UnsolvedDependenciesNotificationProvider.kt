package com.jetbrains.edu.learning.placeholderDependencies

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.getUnsolvedTaskDependencies
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.jetbrains.annotations.TestOnly

class UnsolvedDependenciesNotificationProvider(val project: Project) : EditorNotifications.Provider<UnsolvedDependenciesNotificationProvider.UnsolvedDependenciesNotificationPanel>(), DumbAware {
  companion object {
    val KEY: Key<UnsolvedDependenciesNotificationPanel> = Key.create("Edu.unsolvedDependencies")

    @VisibleForTesting
    fun getText(taskNames: List<String>): String {
      val pluralEndingIfNeeded = if (taskNames.size > 1) "s" else ""
      val taskNamesString = taskNames.joinToString(separator = ", ") { "'$it'" }
      return "Task$pluralEndingIfNeeded $taskNamesString should be solved first"
    }
  }

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): UnsolvedDependenciesNotificationPanel? {
    if (!EduUtils.isStudentProject(project)) {
      return null
    }
    val task = EduUtils.getTaskForFile(project, file) ?: return null
    val taskDependencies = task.getUnsolvedTaskDependencies().sortedBy { it.index }
    if (taskDependencies.isEmpty()) {
      return null
    }
    val panel = UnsolvedDependenciesNotificationPanel()
    panel.setText(getText(taskDependencies.map { it.name }))
    panel.createActionLabel("Solve '${taskDependencies[0].name}'") { NavigationUtils.navigateToTask(project, taskDependencies[0]) }
    return panel
  }


  class UnsolvedDependenciesNotificationPanel : EditorNotificationPanel() {
    @TestOnly
    fun getText(): String = myLabel.text
  }

}