package com.jetbrains.edu.ai.debugger.core.api

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getInvisibleTestFiles
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.language
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.runWithTests
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface TestFinder {

  fun findTestByName(project: Project, testFiles: List<VirtualFile>, testName: String): String?

  companion object {
    private val EP_NAME = LanguageExtension<TestFinder>("Educational.testFinder")

    fun findTestByName(project: Project, task: Task, testName: String): String =
      runWithTests(project, task, {
        runReadAction {
          EP_NAME.forLanguage(project.language())?.findTestByName(
            project,
            task.taskFiles.values.filter { it.isTestFile }.mapNotNull { it.getVirtualFile(project) },
            testName
          )
        }
      }).also {
        deleteTests(task.getInvisibleTestFiles(), project)
      } ?: error("Can't find test text for $testName")
  }
}