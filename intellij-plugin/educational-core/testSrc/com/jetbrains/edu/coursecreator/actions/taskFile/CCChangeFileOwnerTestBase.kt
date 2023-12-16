package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.command.undo.UndoManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course

abstract class CCChangeFileOwnerTestBase(private val actionId: String) : EduActionTestCase() {

  protected fun doUnavailableTest(vararg filePaths: String) {
    val includedFiles = filePaths.map { findFile(it) }
    val context = dataContext(includedFiles.toTypedArray())
    testAction(actionId, context, shouldBeEnabled = false)
  }

  protected fun doAvailableTest(vararg filePaths: String, checksProducer: (Course) -> Pair<List<FileCheck>, List<FileCheck>>) {
    val course = StudyTaskManager.getInstance(project).course!!

    val includedFiles = filePaths.map { findFile(it) }
    val context = dataContext(includedFiles.toTypedArray())
    testAction(actionId, context)

    val (constantChecks, regularChecks) = checksProducer(course)

    fun check(checks: List<FileCheck>) {
      constantChecks.forEach(FileCheck::check)
      checks.forEach(FileCheck::check)
    }

    check(regularChecks)

    val dialog = EduTestDialog()
    withEduTestDialog(dialog) {
      UndoManager.getInstance(project).undo(null)
    }
    check(regularChecks.map(FileCheck::invert))

    withEduTestDialog(dialog) {
      UndoManager.getInstance(project).redo(null)
    }
    check(regularChecks)
  }
}
