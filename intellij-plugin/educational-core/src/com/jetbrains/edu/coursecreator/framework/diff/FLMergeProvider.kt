package com.jetbrains.edu.coursecreator.framework.diff

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
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.messages.EduCoreBundle

class FLMergeProvider(
  private val project: Project,
  private val task: Task,
  private val leftState: FLTaskState,
  private val baseState: FLTaskState,
  private val rightState: FLTaskState,
  private val initialBaseState: FLTaskState = baseState,
) : MergeProvider2 {
  override fun loadRevisions(file: VirtualFile): MergeData {
    return MergeData().apply {
      val filePath = file.pathRelativeToTask(project)
      ORIGINAL = baseState[filePath]?.encodeToByteArray() ?: emptyContent
      CURRENT = leftState[filePath]?.encodeToByteArray() ?: emptyContent
      LAST = rightState[filePath]?.encodeToByteArray() ?: emptyContent
    }
  }

  override fun conflictResolvedForFile(file: VirtualFile) {}

  override fun isBinary(file: VirtualFile): Boolean {
    return file.fileType.isBinary
  }

  override fun createMergeSession(files: List<VirtualFile>): MergeSession {
    return FLMergeSession()
  }

  inner class FLMergeSession : MergeSessionEx {
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

    override fun conflictResolvedForFiles(files: List<VirtualFile>, resolution: Resolution) {}

    override fun acceptFilesRevisions(files: List<VirtualFile>, resolution: Resolution) {
      for (file in files) {
        val fileName = file.name
        val value = if (resolution == Resolution.AcceptedYours) {
          leftState[fileName]
        }
        else {
          rightState[fileName]
        }

        @Suppress("UnstableApiUsage")
        invokeAndWaitIfNeeded {
          if (value == null) {
            runWriteAction {
              file.delete(javaClass)
            }
          }
          else {
            EduDocumentListener.modifyWithoutListener(task, fileName) {
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
  ) : ColumnInfo<VirtualFile, String>(defaultName) {
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
          return "-"
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

  companion object {
    private val emptyContent = ByteArray(0)
  }
}