package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.command.undo.UndoManager
import com.jetbrains.edu.coursecreator.FileCheck
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.withTestDialog

abstract class CCChangeFileOwnerTestBase(private val action: AnAction) : EduActionTestCase() {

  protected fun doUnavailableTest(vararg filePaths: String) {
    val includedFiles = filePaths.map { findFile(it) }
    val context = dataContext(includedFiles.toTypedArray())
    val presentation = testAction(context, action, false)
    checkActionEnabled(presentation, false)
  }

  protected fun doAvailableTest(vararg filePaths: String, checksProducer: (Course) -> Pair<List<FileCheck>, List<FileCheck>>) {
    val course = StudyTaskManager.getInstance(project).course!!

    val includedFiles = filePaths.map { findFile(it) }
    val context = dataContext(includedFiles.toTypedArray())
    val presentation = testAction(context, action, true)
    checkActionEnabled(presentation, true)

    val (constantChecks, regularChecks) = checksProducer(course)

    fun check(checks: List<FileCheck>) {
      constantChecks.forEach(FileCheck::check)
      checks.forEach(FileCheck::check)
    }

    check(regularChecks)

    val dialog = EduTestDialog()
    withTestDialog(dialog) {
      UndoManager.getInstance(project).undo(null)
    }
    check(regularChecks.map(FileCheck::invert))

    withTestDialog(dialog) {
      UndoManager.getInstance(project).redo(null)
    }
    check(regularChecks)
  }
}
