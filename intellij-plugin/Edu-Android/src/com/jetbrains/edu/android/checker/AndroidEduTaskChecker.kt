package com.jetbrains.edu.android.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.checker.NewGradleEduTaskChecker
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class AndroidEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : NewGradleEduTaskChecker(task, envChecker, project) {
  override fun createTestResultCollector(): TestResultCollector = AndroidTestResultCollector()
}
