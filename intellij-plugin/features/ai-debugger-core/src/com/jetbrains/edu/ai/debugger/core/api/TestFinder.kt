package com.jetbrains.edu.ai.debugger.core.api

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
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

    @RequiresReadLock
    fun findTestByName(project: Project, task: Task, testName: String): String? =
      runWithTests(project, task, {
        EP_NAME.forLanguage(project.language())?.findTestByName(
          project,
          task.taskFiles.values.filter { it.isTestFile }.mapNotNull { it.getVirtualFile(project) },
          testName
        )
      }).also {
        deleteTests(task.getInvisibleTestFiles(), project)
      }
  }
}