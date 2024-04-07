package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

abstract class SolutionLoadingTestBase : NavigationTestBase() {
  override fun setUp() {
    super.setUp()
    doLoginFakeUser()
  }

  override fun tearDown() {
    try {
      doLogout()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  open fun doLoginFakeUser() {}
  open fun doLogout() {}

  protected fun makeLocalChanges(file: VirtualFile): String {
    val newText = "lalala"
    // hack: timestamps don't change in tests
    runWriteAction { VfsUtil.saveText(file, newText) }
    (file as NewVirtualFile).timeStamp = System.currentTimeMillis()

    //explicitly mark as existing project
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, false)

    return newText
  }

  protected fun checkTaskStatuses(tasks: List<Task>, expectedStatuses: List<CheckStatus>) {
    tasks.zip(expectedStatuses).forEach { (task, status) -> assertEquals(status, task.status) }
  }
}