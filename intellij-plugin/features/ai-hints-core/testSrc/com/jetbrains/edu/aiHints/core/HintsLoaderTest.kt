package com.jetbrains.edu.aiHints.core

import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.executeSomeCoroutineTasksAndDispatchAllInvocationEvents
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import org.junit.Test
import kotlin.random.Random

class HintsLoaderTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    courseWithFiles {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt", "original text")
        }
      }
    }
    /**
     * Replace [com.intellij.openapi.fileEditor.impl.TestEditorManagerImpl] with the production one,
     * because [com.intellij.diff.DiffManager] fails due to cast error.
     */
    setProductionFileEditorManager()
  }

  /** TODO: add tests for the [HintsLoader.getHint] */

  /** Tests for [HintsLoader.showInCodeAction] */
  @Test
  fun `test show in code action`() {
    // when
    val launchId = Random.nextInt()
    val taskVirtualFile = findFile("lesson/task/task.txt")
    val taskFileText = taskVirtualFile.readText()
    val codeHint = "modified text"

    // then
    executeShowInCodeAction(launchId, taskVirtualFile, taskFileText, codeHint)

    // verify
    val openFiles = FileEditorManager.getInstance(project).openFiles
    assertEquals(1, openFiles.size)
    val diffVirtualFile = openFiles.filterIsInstance<ChainDiffVirtualFile>().firstOrNull() ?: error("Diff file is not opened")
    verifyGetHintDiffVirtualFile(diffVirtualFile, launchId, taskVirtualFile)
  }

  @Test
  fun `test the same diff is not opened twice within the same launch`() {
    val launchId = Random.nextInt()
    val taskVirtualFile = findFile("lesson/task/task.txt")
    val taskFileText = taskVirtualFile.readText()
    val codeHint = "something else"

    // then
    executeShowInCodeAction(launchId, taskVirtualFile, taskFileText, codeHint)

    // verify one diff is opened
    assertEquals(1, FileEditorManager.getInstance(project).openFiles.size)

    // and again
    executeShowInCodeAction(launchId, taskVirtualFile, taskFileText, codeHint)

    // verify still only one is opened
    val openFiles = FileEditorManager.getInstance(project).openFiles
    assertEquals(1, openFiles.size)
    val diffVirtualFile = openFiles.filterIsInstance<ChainDiffVirtualFile>().firstOrNull() ?: error("Diff file is not opened")
    verifyGetHintDiffVirtualFile(diffVirtualFile, launchId, taskVirtualFile)
  }

  private fun executeShowInCodeAction(launchId: Int, taskVirtualFile: VirtualFile, taskFileText: String, codeHint: String) {
    HintsLoader.getInstance(project).showInCodeAction(project, launchId, taskVirtualFile, taskFileText, codeHint)
    // Since we use async production implementation of `FileEditorManager` instead of sync test implementation,
    // we need to wait when all events are processed
    executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)
  }

  private fun verifyGetHintDiffVirtualFile(diffVirtualFile: ChainDiffVirtualFile, launchId: Int, taskVirtualFile: VirtualFile) {
    val diffRequestChain = diffVirtualFile.chain
    assertEquals(true, diffRequestChain.getUserData(ApplyCodeAction.GET_HINT_DIFF))
    assertEquals(listOf(taskVirtualFile.path), diffRequestChain.getUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST))
    assertEquals(launchId, diffRequestChain.getUserData(HintsLoader.LAUNCH_ID))
    assertEquals(1, diffRequestChain.requests.size)
    val diffRequest = diffRequestChain.requests.first()
    assertEquals(taskVirtualFile.fileType, diffRequest.contentType)
  }
}