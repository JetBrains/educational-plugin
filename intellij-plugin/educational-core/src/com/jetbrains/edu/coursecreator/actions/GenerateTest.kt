package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.createDirectories
import com.jetbrains.edu.coursecreator.testGeneration.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.TestGenerator
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.pathRelativeToTask
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.storage.pathInStorage
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.JComponent
import javax.swing.JTextField


open class GenerateTest : AnAction() {


  override fun actionPerformed(e: AnActionEvent) {
    generateTest(e)
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


  private fun generateTest(e: AnActionEvent) {
    val project = e.project!!
    val dialog = SampleDialogWrapper()
    if (!dialog.showAndGet()) {
      return
    }
    val fileName = "${dialog.textField.text}.java"
    val selectedTaskFile = e.project!!.selectedTaskFile!!
    val packagePath = selectedTaskFile.getVirtualFile(project)?.pathRelativeToTask(project)?.replace("src/", "")!!
      .replace(selectedTaskFile.getVirtualFile(project)?.name!!, "")
    println(packagePath)
    val task = selectedTaskFile.task
    println(task.testDirs)
    val testDir = task.findTestDirs(project).first()
    println(testDir)

    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
    val language = StudyTaskManager.getInstance(project).course?.languageById
                   ?: error("There are no course or language instance") // TODO replace with the relevant behaviour
    val psiHelper = PsiHelper.getInstance(language)
    psiHelper.psiFile = psiFile

    val caret = e.dataContext.getData(CommonDataKeys.CARET)?.caretModel?.primaryCaret!!.offset // TODO additional checking for caret
    val text = TestGenerator(project).generateFileTests(psiHelper, fileName, caret)


    val file = testDir.toNioPath().resolve(packagePath).apply { createDirectories() }.resolve(fileName).toFile().apply {
      println(this.absolutePath)
      createNewFile()
      writeText(text)
    }
    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    VirtualFileManager.getInstance().findFileByNioPath(file.toPath())?.refresh(true, true)
    VirtualFileManager.getInstance().findFileByNioPath(file.toPath())?.refresh(false, true)
    VirtualFileManager.getInstance().syncRefresh()
//    TaskToolWindowView.getInstance(project).updateNavigationPanel()
    ProjectView.getInstance(project).refresh()

  }

  inner class SampleDialogWrapper : DialogWrapper(true) {

    val textField = JTextField().apply {
      val screenWidth = Toolkit.getDefaultToolkit().screenSize.width
      val textFieldWidth = screenWidth / 5

      // Step 2: Set preferred size of the textField
      preferredSize = Dimension(textFieldWidth, preferredSize.height)
    }


    init {
      title = "Enter Test File Name"
      init()
    }

    override fun createCenterPanel(): JComponent? = textField

  }

}

