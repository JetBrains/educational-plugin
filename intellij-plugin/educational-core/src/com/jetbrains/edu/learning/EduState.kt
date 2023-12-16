package com.jetbrains.edu.learning

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task


data class EduState(
  val project: Project,
  val virtualFile: VirtualFile,
  val editor: Editor,
  val taskFile: TaskFile,
  val task: Task = taskFile.task,
  val answerPlaceholder: AnswerPlaceholder? = taskFile.getAnswerPlaceholder(editor.caretModel.offset)
)