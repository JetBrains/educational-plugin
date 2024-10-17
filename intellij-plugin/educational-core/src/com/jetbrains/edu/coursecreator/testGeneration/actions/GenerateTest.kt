package com.jetbrains.edu.coursecreator.testGeneration.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.ProjectTaskManager
import com.intellij.util.io.createDirectories
import com.jetbrains.edu.coursecreator.testGeneration.TestGenerator
import com.jetbrains.edu.coursecreator.testGeneration.psi.manager.PsiHelperManager
import com.jetbrains.edu.coursecreator.testGeneration.util.TestProgressIndicator
import com.jetbrains.edu.coursecreator.testGeneration.util.TestedFileInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import java.awt.Dimension
import java.awt.Toolkit
import java.io.File
import javax.swing.JComponent
import javax.swing.JTextField
import kotlin.math.roundToInt

private const val DIALOG_WIDTH_RATIO = 0.2f

class GenerateTest : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.EDT

  override fun actionPerformed(e: AnActionEvent) {

    val dialog = FileNameForTestingDialog()
    if (!dialog.showAndGet()) {
      return
    }

    val testedFileInfo = getTestedFileInfo(e)

    val fileName = dialog.textField.text
    ApplicationManager.getApplication().executeOnPooledThread {
      ProjectTaskManager.getInstance(e.project).buildAllModules().onSuccess {
        ProgressManager.getInstance().run(
          object : Task.Backgroundable(e.project, "Generating test") { // TODO
            override fun run(indicator: ProgressIndicator) {
              val testIndicator = TestProgressIndicator(indicator)
              generateTest(testedFileInfo, fileName, testIndicator)
            }
          }
        )
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
    e.presentation.isEnabledAndVisible = project != null && isCourseJavaCreator(project)
                                         && virtualFile?.isTestsFile(project) == false
  }

  private fun isCourseJavaCreator(project: Project): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    return (CourseMode.EDUCATOR == course.courseMode
            || CourseMode.EDUCATOR == EduUtilsKt.getCourseModeForNewlyCreatedProject(project))
           && course.languageId == "JAVA" // TODO
  }


  private fun getTestedFileInfo(event: AnActionEvent): TestedFileInfo {
    val project = event.project ?: error("Project should not be null")
    val caret =
      event.dataContext.getData(CommonDataKeys.CARET)?.caretModel?.primaryCaret?.offset
      ?: error("Primary caret should be specified")
    val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: error("psiFile should be specified")
    val language = StudyTaskManager.getInstance(project).course?.languageById
                   ?: error("Unknown or unsupported language detected")
    val selectedTaskFile = project.selectedTaskFile ?: error("Task File is not selected")
    return TestedFileInfo(project, psiFile, caret, language, selectedTaskFile)
  }

  private fun generateTest(testedFileInfo: TestedFileInfo, testFilename: String, progressIndicator: TestProgressIndicator) {
    val project = testedFileInfo.project
    val selectedTaskFile = testedFileInfo.selectedTaskFile
    val packagePath = selectedTaskFile.getVirtualFile(project)?.getPackagePath(project) ?: error("There are no virtual file for such task")

    val task = selectedTaskFile.task
    val testDir = task.findTestDirs(project).first()
    val psiHelper = PsiHelperManager.getInstance(testedFileInfo.language).getPsiHelper(testedFileInfo.psiFile)

    val text = TestGenerator(project).generateTestSuite(psiHelper, testFilename, testedFileInfo, progressIndicator)
    val file = testDir.createAndWriteTestToFile(packagePath, testFilename, text, project)
    project.updateNavigator(file)
  }

  private fun VirtualFile.getPackagePath(project: Project) =
    pathRelativeToTask(project).replace("src/", "").replace(this.name, "") // TODO


  private fun VirtualFile.createAndWriteTestToFile(packagePath: String, testFilename: String, text: String, project: Project): File {
    val directory = toNioPath().resolve(packagePath)
    writeOnEdt(project) {
      directory.createDirectories()
    }
    val file = directory.resolve("$testFilename.java").toFile()
    writeOnEdt(project) {
      file.createNewFile()
      file.writeText(text)
    }
    return file
  }

  private fun writeOnEdt(project: Project, action: () -> Unit) { // TODO replace
    WriteCommandAction.runWriteCommandAction(project) {
      action.invoke()
    }
  }

  private fun Project.updateNavigator(file: File) {
    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    ProjectView.getInstance(this).refresh()
  }

  inner class FileNameForTestingDialog : DialogWrapper(true) {

    val textField = JTextField().apply {
      val screenWidth = Toolkit.getDefaultToolkit().screenSize.width
      val textFieldWidth = (screenWidth * DIALOG_WIDTH_RATIO).roundToInt()

      preferredSize = Dimension(textFieldWidth, preferredSize.height)
    }


    init {
      title = "Enter Test File Name" // TODO
      init()
    }

    override fun createCenterPanel(): JComponent = textField

  }

}