package com.jetbrains.edu.coursecreator.actions

import com.intellij.icons.ExpUiIcons.Breakpoints
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.task.ProjectTaskManager
import com.intellij.util.io.createDirectories
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil
import com.jetbrains.edu.coursecreator.testGeneration.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.TestGenerator
import com.jetbrains.edu.coursecreator.testGeneration.TestProgressIndicator
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.pathRelativeToTask
import com.jetbrains.edu.learning.selectedTaskFile
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.JComponent
import javax.swing.JTextField


open class GenerateTest : AnAction() {


  override fun actionPerformed(e: AnActionEvent) {

    val dialog = SampleDialogWrapper()
    if (!dialog.showAndGet()) {
      return
    }
    val fileName = "${dialog.textField.text}.java"
    ApplicationManager.getApplication().executeOnPooledThread {
      ProjectTaskManager.getInstance(e.project).buildAllModules().onSuccess {
        ProgressManager.getInstance()
          .run(
            object : Task.Backgroundable(e.project, "Generating test") {
            override fun run(indicator: ProgressIndicator) {
              val testIndicator = TestProgressIndicator(indicator)
              generateTest(e, fileName, testIndicator)
            }
          })
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && isCourseJavaCreator(project)
  }

  private fun isCourseJavaCreator(project: Project): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    return (CourseMode.EDUCATOR == course.courseMode
            || CourseMode.EDUCATOR == EduUtilsKt.getCourseModeForNewlyCreatedProject(project))
           && course.languageId == "JAVA" // TODO
  }


  private fun generateTest(e: AnActionEvent, fileName: String, progressIndicator: TestProgressIndicator) {
    val project = e.project!!
    val selectedTaskFile = e.project!!.selectedTaskFile!!
    val packagePath = selectedTaskFile.getVirtualFile(project)?.pathRelativeToTask(project)?.replace("src/", "")!!
      .replace(selectedTaskFile.getVirtualFile(project)?.name!!, "")
    val task = selectedTaskFile.task
    val testDir = task.findTestDirs(project).first()

    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
    val language = StudyTaskManager.getInstance(project).course?.languageById
                   ?: error("There are no course or language instance") // TODO replace with the relevant behaviour
    val psiHelper = PsiHelper.getInstance(language)
    psiHelper.psiFile = psiFile

    val caret = e.dataContext.getData(CommonDataKeys.CARET)?.caretModel?.primaryCaret!!.offset // TODO additional checking for caret
    val text = TestGenerator(project).generateFileTests(psiHelper, fileName, caret, progressIndicator)


    val file = testDir.toNioPath().resolve(packagePath).apply { createDirectories() }.resolve(fileName).toFile().apply {
      createNewFile()
      writeText(text)
    }
    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    ProjectView.getInstance(project).refresh()

  }

  inner class SampleDialogWrapper : DialogWrapper(true) {

    val textField = JTextField().apply {
      val screenWidth = Toolkit.getDefaultToolkit().screenSize.width
      val textFieldWidth = screenWidth / 5

      preferredSize = Dimension(textFieldWidth, preferredSize.height)
    }


    init {
      title = "Enter Test File Name"
      init()
    }

    override fun createCenterPanel(): JComponent = textField

  }

}

