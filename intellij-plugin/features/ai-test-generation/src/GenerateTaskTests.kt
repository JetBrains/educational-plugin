package com.jetbrains.edu.ai.tests

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.ai.tests.connector.GenerateTaskTestConnector
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.getTask
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Right-click on the task directory and select 'Generate Tests for This Task' to run this action.
 */
class GenerateTaskTests : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val data = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    e.presentation.isEnabledAndVisible = (data?.findChild("task.md")
                                          ?: data?.findChild("task.html")) != null
    e.presentation.icon = AllIcons.RunConfigurations.TestState.Run_run
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val taskRoot = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: error("No task root found")
    val taskFile = taskRoot.findChild("task.md") ?: taskRoot.findChild("task.html") ?: error("No task file found")
    val generationData = computeGenerationData(project, taskRoot)

    runWithModalProgressBlocking(project, "Generating tests") {
      val taskDescription = taskFile.readText()
      val codeSnippet = generationData.codeFile.readText()

      val connector = GenerateTaskTestConnector.getInstance()
      val generatedCode = connector.generateTests(
        taskDescription = taskDescription,
        codeSnippet = codeSnippet,
        promptCustomization = generationData.promptCustomization
        // Default temperature value
        // Default LLM profile
      )

      writeCommandAction(project, "Applying Generated Code") {
        generationData.testFile.document.setText(generatedCode)
      }

      withContext(Dispatchers.EDT) {
        FileEditorManager.getInstance(project).openFile(generationData.testFile, true)
      }
    }
  }

  private fun computeGenerationData(project: Project, directory: VirtualFile): GenerationData {
    val task = directory.getTask(project) ?: error("Cannot find task in directory: ${directory.path}")
    val course = task.course
    val configurator = course.configurator ?: error("Cannot find configurator for course: ${course.name}")

    val testDir = course.testDirs.firstOrNull() ?: ""

    val language = course.languageId.lowercase()
    val codeFile = task.getCodeTaskFile(project)?.getVirtualFile(project) ?: error("Cannot find task file for ${task.name}")

    val testFileName = configurator.testFileName
    val testDirFile = directory.findChild(testDir)
      ?: error("Cannot find test directory: ${directory.path}/$testDir")
    val testFile = testDirFile.findChild(testFileName) ?: testDirFile.createChildData(this, testFileName)

    // should be moved to ml-lib
    val promptCustomization = when (language) {
      "kotlin" -> """
        Tests must be in kotlin, inside a class called 'Tests' and must use Junit library and '@Test' annotation for evey test.
        If the code snippet requires the user to implement a function or a class, to you have to import this function or class in tests.
        """.trimIndent()
      "python" -> """
        Tests must be in Python and use unittest library for tests.
        If the code snippet requires the user to implement a function or a class, to you have to import this function or class in tests from the task file.
        """.trimIndent()
      else -> error("Unsupported language: $language")
    }

    return GenerationData(language, codeFile, testFile, promptCustomization)
  }


  data class GenerationData(val language: String, val codeFile: VirtualFile, val testFile: VirtualFile, val promptCustomization: String)
}
