package com.jetbrains.edu.learning.editor

import com.intellij.openapi.fileEditor.TrailingSpacesOptionsProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.getTaskFile

/**
 * Provider for [AnswerTask].
 * AnswerTask has no space and line break in its answer.
 */
class TrailingSpacesOptionsAnswerTaskProvider : TrailingSpacesOptionsProvider {

  override fun getOptions(project: Project, file: VirtualFile): TrailingSpacesOptionsProvider.Options? =
    when (file.name) {
      AnswerTask.ANSWER_FILE_NAME -> createAnswerOptions(project, file)
      else -> null
    }

  private fun createAnswerOptions(project: Project, file: VirtualFile): AnswerOptions? {
    val taskFile = file.getTaskFile(project) ?: return null
    return if (taskFile.name == AnswerTask.ANSWER_FILE_NAME && taskFile.task is AnswerTask) AnswerOptions else null
  }

  object AnswerOptions : TrailingSpacesOptionsProvider.Options {

    /**
     * Option in preferences "Ensure every saved file ends with a line break"
     *
     * Method allow adding new line at end of file(EOF)
     * @return true  - add new line
     *         false - otherwise
     */
    override fun getEnsureNewLineAtEOF(): Boolean = false

    /**
     * Option in preferences "Remove trailing blank lines at the end of saved files"
     *
     * Method allow removing blank lines
     * @return true  - remove blank lines
     *         false - otherwise
     */
    override fun getRemoveTrailingBlankLines(): Boolean? = null

    /**
     *
     * Method allows applying methods of this class to all lines in the file
     * or only to those that have been changed
     * @return true  - to modified files
     *         false - to all lines in file
     */
    override fun getChangedLinesOnly(): Boolean? = null

    /**
     * Option in preferences "Keep trailing spaces on caret line"
     *
     * Method allows removing spaces if the caret placed at the end of line
     * @return true  - allow
     *         false - otherwise
     */
    override fun getKeepTrailingSpacesOnCaretLine(): Boolean? = null

    /**
     * Option in preferences "Remove trailing spaces on:"
     *
     * Method allow removing spaces at the end lines
     * @return true  - remove spaces
     *         false - otherwise
     */
    override fun getStripTrailingSpaces(): Boolean? = null
  }
}