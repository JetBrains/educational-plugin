package com.jetbrains.edu.coursecreator.framework.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class SyncChangesEditorNotificationsProvider : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!CCUtils.isCourseCreator(project) || !isFeatureEnabled(EduExperimentalFeatures.CC_FL_SYNC_CHANGES)) return null

    val taskFile = file.getTaskFile(project) ?: return null
    if (taskFile.task.lesson !is FrameworkLesson) return null

    // check if changes are synced for this task file
    if (SyncChangesStateManager.getInstance(project).getSyncChangesState(taskFile) == null) return null

    return Function {
      EditorNotificationPanel().apply {
        text = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.Editor.Notification.description")
        icon(AllIcons.General.Information)
        createActionLabel(
          EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ActionLink.Changes.text"),
          { CCFrameworkLessonManager.getInstance(project).propagateChanges(taskFile.task, listOf(taskFile)) },
          true
        )
      }
    }
  }
}