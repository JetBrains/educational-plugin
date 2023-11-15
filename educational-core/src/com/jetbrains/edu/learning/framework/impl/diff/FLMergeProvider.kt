package com.jetbrains.edu.learning.framework.impl.diff

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.merge.MergeData
import com.intellij.openapi.vcs.merge.MergeProvider2
import com.intellij.openapi.vcs.merge.MergeSession
import com.intellij.openapi.vcs.merge.MergeSession.Resolution
import com.intellij.openapi.vcs.merge.MergeSessionEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.ColumnInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroUtils
import com.jetbrains.edu.learning.framework.impl.State
import com.jetbrains.edu.learning.messages.EduCoreBundle

class FLMergeProvider(
  private val project: Project,
  private val task: Task,
  private val leftState: State,
  private val baseState: State,
  private val rightState: State,
  private val initialBaseState: State = baseState,
): MergeProvider2 {
  override fun loadRevisions(file: VirtualFile): MergeData {
    return MergeData().apply {
      val filePath = file.pathRelativeToTask(project)
      ORIGINAL = baseState[filePath]?.encodeToByteArray() ?: ByteArray(0)
      CURRENT = leftState[filePath]?.encodeToByteArray() ?: ByteArray(0)
      LAST = rightState[filePath]?.encodeToByteArray() ?: ByteArray(0)
    }
  }

  override fun conflictResolvedForFile(file: VirtualFile) {}

  override fun isBinary(file: VirtualFile): Boolean {
    return file.fileType.isBinary
  }

  override fun createMergeSession(files: MutableList<VirtualFile>): MergeSession {
    return FLMergeSession()
  }

  inner class FLMergeSession: MergeSessionEx {
    override fun getMergeInfoColumns(): Array<ColumnInfo<out Any, out Any>> {
      return arrayOf(
        StatusColumn("Current task", true),
        StatusColumn("Target task", false)
      )
    }

    override fun canMerge(file: VirtualFile): Boolean {
      return true
    }

    override fun conflictResolvedForFile(file: VirtualFile, resolution: Resolution) {}

    override fun conflictResolvedForFiles(files: MutableList<out VirtualFile>, resolution: Resolution) {}

    override fun acceptFilesRevisions(files: MutableList<out VirtualFile>, resolution: Resolution) {
      for (file in files) {
        val filePath = file.pathRelativeToTask(project)
        val value = if (resolution == Resolution.AcceptedYours) {
          leftState[filePath]
        }
        else {
          rightState[filePath]
        }
        val taskDir = file.getTaskDir(project) ?: return

        @Suppress("UnstableApiUsage")
        invokeAndWaitIfNeeded {
          if (value == null) {
            runWriteAction {
              file.removeWithEmptyParents(taskDir)
            }
          }
          else {
            EduDocumentListener.modifyWithoutListener(task, filePath) {
              val document = runReadAction {
                FileDocumentManager.getInstance().getDocument(file)
              }
              if (document != null) {
                val expandedText = StringUtil.convertLineSeparators(
                  EduMacroUtils.expandMacrosForFile(project.toCourseInfoHolder(), file, value)
                )
                runWriteAction {
                  document.setText(expandedText)
                }
              }
            }
          }
        }
      }
    }
  }

  private inner class StatusColumn(
    defaultName: String,
    private val isLeft: Boolean,
  ): ColumnInfo<VirtualFile, String>(defaultName) {
    private val defaultGap = 10

    override fun valueOf(item: VirtualFile?): String {
      if (item == null) return ""
      val filePath = item.pathRelativeToTask(project)
      val baseContent = initialBaseState[filePath]
      val changedContent = if (isLeft) {
        leftState[filePath]
      }
      else {
        rightState[filePath]
      }
      if (baseContent == null) {
        if (changedContent == null) {
          return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.none")
        }
        return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.added")
      }
      if (changedContent == null) {
        return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.deleted")
      }
      return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.modified")
    }

    override fun getAdditionalWidth(): Int {
      return defaultGap
    }
  }
}