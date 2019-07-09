package com.jetbrains.edu.cpp.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    val taskProjectName = generateCMakeProjectUniqueName(task)
    val configuration = RunManager.getInstance(project).allSettings.firstOrNull { it.name == taskProjectName }
                        ?: return CheckResult(CheckStatus.Unchecked, "No <code>target</code> to run", needEscape = false)

    runInEdt {
      ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
    }
    return CheckResult(CheckStatus.Solved, "")
  }

  private fun generateCMakeProjectUniqueName(task: Task): String {
    val lesson: Lesson = task.lesson
    val section: Section? = lesson.section

    val taskPart = "${EduNames.TASK}${task.index}"
    val lessonPart = "${EduNames.LESSON}${lesson.index}"
    if (section == null) {
      return "$lessonPart-$taskPart"
    }

    val sectionPart = "${EduNames.SECTION}${section.index}"
    return "$sectionPart-$lessonPart-$taskPart"
  }
}