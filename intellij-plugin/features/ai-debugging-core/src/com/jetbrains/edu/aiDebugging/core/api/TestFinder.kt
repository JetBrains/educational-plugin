package com.jetbrains.edu.aiDebugging.core.api

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiDebugging.core.utils.AIDebugUtils.language
import com.jetbrains.edu.aiDebugging.core.utils.AIDebugUtils.runWithTests
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class TestFinder {

  abstract fun findTestByName(project: Project, testFiles: List<VirtualFile>, testName: String): String?

  companion object {
    private val EP_NAME = LanguageExtension<TestFinder>("aiDebugging.testFinder")

    @RequiresReadLock
    fun findTestByName(project: Project, task: Task, testName: String): String? =
      runWithTests(project, task, {
        EP_NAME.forLanguage(project.language())?.findTestByName(
          project,
          task.taskFiles.values.filter { it.isTestFile }.mapNotNull { it.getVirtualFile(project) },
          testName
        )
      })
  }
}