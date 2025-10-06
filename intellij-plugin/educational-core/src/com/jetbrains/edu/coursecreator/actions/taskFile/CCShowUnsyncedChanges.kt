package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCShowUnsyncedChanges : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val file = getSelectedFile(e) ?: return
    val taskFile = file.getTaskFile(project) ?: return

    val storedState = CCFrameworkLessonManager.getInstance(project).getStateFromStorage(taskFile.task)
    val storageContents = storedState[taskFile.name]

    val currentDiffContent = DiffContentFactory.getInstance().create(project, file)
    val savedDiffContent = if (storageContents is BinaryContents) {
      DiffContentFactory.getInstance().createFromBytes(project, storageContents.bytes, file.fileType, file.name)
    } else {
      val text = storageContents?.textualRepresentation ?: "(null)"
      DiffContentFactory.getInstance().create(project, text, file.fileType)
    }

    val request = SimpleDiffRequest(
      EduCoreBundle.message("action.Educational.Educator.ShowUnsyncedChanges.diff.title"),
      savedDiffContent,
      currentDiffContent,
      EduCoreBundle.message("action.Educational.Educator.ShowUnsyncedChanges.diff.left", file.name),
      EduCoreBundle.message("action.Educational.Educator.ShowUnsyncedChanges.diff.right", file.name)
    )

    DiffManager.getInstance().showDiff(project, request, DiffDialogHints.FRAME)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    if (!CCUtils.isCourseCreator(project)) {
      return
    }

    val taskFile = getSelectedFile(e)?.getTaskFile(project) ?: return

    if (taskFile.task.lesson !is FrameworkLesson) return

    presentation.isEnabledAndVisible = SyncChangesStateManager.getInstance(project).getSyncChangesState(taskFile) != null
  }

  private fun getSelectedFile(e: AnActionEvent): VirtualFile? {

    return CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)?.singleOrNull()
  }

  companion object {
    const val ACTION_ID = "Educational.Educator.ShowUnsyncedChanges"
  }
}