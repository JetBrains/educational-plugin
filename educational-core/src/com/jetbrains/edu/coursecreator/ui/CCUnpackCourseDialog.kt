package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectForFile
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import java.io.File
import javax.swing.JComponent

class CCUnpackCourseDialog(val course: Course) : DialogWrapper(true) {
  private val LOG = Logger.getInstance(CCUnpackCourseDialog::class.java)
  private val myPanel: CCNewCoursePanel = CCNewCoursePanel()

  private var myCourse: Course = course

  init {
    title = "Unpack Course"
    setOKButtonText("Unpack")
    init()
    myPanel.setValidationListener(object : CCNewCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }
    })
    myPanel.setDescription(course.description)
    myPanel.setLanguage(course.languageID)
    myPanel.setCourseName(course.name)
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun doOKAction() {
    val course = myCourse
    val projectSettings = myPanel.projectSettings
    val location = myPanel.locationString
    val language = course.languageById
    if (language == null) return

    close(OK_EXIT_CODE)
    course.courseMode = CCUtils.COURSE_MODE
    EduConfiguratorManager.forLanguage(language)
        ?.courseBuilder
        ?.getCourseProjectGenerator(course)
        ?.doCreateCourseProject(location, projectSettings)
    val project = guessProjectForFile(VfsUtil.findFileByIoFile(File(location), true))
    if (project == null) return
    val application = ApplicationManager.getApplication()

    var index = 1
    var taskIndex = 1
    for (lesson in course.lessons) {
      val lessonDir = project.baseDir.findChild(EduNames.LESSON + index.toString())
      lesson.index = index
      if (lessonDir == null) continue
      for (task in lesson.getTaskList()) {
        val taskDir = lessonDir.findChild(EduNames.TASK + taskIndex.toString())
        task.index = taskIndex
        task.lesson = lesson
        if (taskDir == null) continue
        for (entry in task.getTaskFiles().entries) {
          application.invokeAndWait { application.runWriteAction { createAnswerFile(project, taskDir, entry) } }
        }
        taskIndex += 1
      }
      index += 1
      taskIndex = 1
    }
    course.initCourse(true)
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    ProjectView.getInstance(project).refresh()
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

    for (placeholder in taskFile.activePlaceholders) {
      replaceAnswerPlaceholder(document, placeholder)
    }
    for (placeholder in taskFile.answerPlaceholders) {
      placeholder.useLength = false
    }

    CommandProcessor.getInstance().executeCommand(project,
        {
          ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveDocumentAsIs(document)
          }
        },
        "Create answer document", "Create answer document")
    document.removeDocumentListener(listener)
  }

  private fun replaceAnswerPlaceholder(document: Document,
                                       placeholder: AnswerPlaceholder) {
    val offset = placeholder.offset
    val text = document.getText(TextRange.create(offset, offset + placeholder.realLength))
    placeholder.taskText = text
    placeholder.init()
    val replacementText = placeholder.possibleAnswer

    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        document.replaceString(offset, offset + placeholder.realLength, replacementText)
        FileDocumentManager.getInstance().saveDocumentAsIs(document)
      }
    }
  }
}
