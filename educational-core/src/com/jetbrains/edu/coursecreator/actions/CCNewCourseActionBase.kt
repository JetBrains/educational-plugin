package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile

// TODO: move `initializeCourseProject` and corresponding code into `GeneratorUtils`
// and call it in `createCourse` after code of `GeneratorUtils` and `EduGradleModuleGenerator` will be merged
abstract class CCNewCourseActionBase(name: String, description: String) : DumbAwareAction(name, description, null) {

  protected fun initializeCourseProject(courseProject: Project, course: Course) {
    val application = ApplicationManager.getApplication()

    var index = 1
    var taskIndex = 1
    for (lesson in course.lessons) {
      val lessonDir = courseProject.baseDir.findChild(EduNames.LESSON + index.toString())
      lesson.index = index
      if (lessonDir == null) continue
      for (task in lesson.getTaskList()) {
        val taskDir = lessonDir.findChild(EduNames.TASK + taskIndex.toString())
        task.index = taskIndex
        task.lesson = lesson
        if (taskDir == null) continue
        for (entry in task.getTaskFiles().entries) {
          application.invokeAndWait { application.runWriteAction { createAnswerFile(courseProject, taskDir, entry) } }
        }
        taskIndex += 1
      }
      index += 1
      taskIndex = 1
    }
    course.initCourse(true)
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    ProjectView.getInstance(courseProject).refresh()
  }

  private fun createAnswerFile(project: Project,
                               userFileDir: VirtualFile,
                               taskFileEntry: Map.Entry<String, TaskFile>) {
    val taskFile = taskFileEntry.value
    val file = EduUtils.findTaskFileInDir(taskFile, userFileDir)
    if (file == null) {
      LOG.warn("Failed to find file " + file)
      return
    }
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return

    CommandProcessor.getInstance().executeCommand(project,
        {
          ApplicationManager.getApplication().runWriteAction {
            document.replaceString(0, document.textLength, document.charsSequence)
          }
        },
        "Create answer document", "Create answer document")
    val listener = EduDocumentListener(taskFile, false)
    document.addDocumentListener(listener)
    taskFile.sortAnswerPlaceholders()
    taskFile.isTrackLengths = false

    for (placeholder in taskFile.answerPlaceholders) {
      placeholder.useLength = false
    }

    for (placeholder in taskFile.activePlaceholders) {
      replaceAnswerPlaceholder(document, placeholder)
    }

    CommandProcessor.getInstance().executeCommand(project,
        {
          ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveDocumentAsIs(document)
          }
        },
        "Create answer document", "Create answer document")
    document.removeDocumentListener(listener)
    taskFile.isTrackLengths = true
  }

  private fun replaceAnswerPlaceholder(document: Document,
                                       placeholder: AnswerPlaceholder) {
    val offset = placeholder.offset
    val text = document.getText(TextRange.create(offset, offset + placeholder.length))
    placeholder.taskText = text
    placeholder.init()
    val replacementText = placeholder.possibleAnswer

    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        document.replaceString(offset, offset + placeholder.length, replacementText)
        FileDocumentManager.getInstance().saveDocumentAsIs(document)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CCNewCourseActionBase::class.java)
  }
}
