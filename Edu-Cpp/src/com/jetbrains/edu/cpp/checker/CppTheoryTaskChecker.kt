package com.jetbrains.edu.cpp.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cpp.CppCourseProjectGenerator
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    val lesson: Lesson = task.lesson
    val section: Section? = lesson.section
    val taskProjectName = CppCourseProjectGenerator.getCMakeProjectUniqueName(section, lesson, task)
    val configuration = RunManager.getInstance(project).findConfigurationByName(taskProjectName)
                        ?: return CheckResult(CheckStatus.Unchecked, "No <code>target</code> to run", needEscape = false)

    runInEdt {
      ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
    }
    return CheckResult(CheckStatus.Solved, "")
  }
}