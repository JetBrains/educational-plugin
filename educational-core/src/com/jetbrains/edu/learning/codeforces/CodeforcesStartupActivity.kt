package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.isUnitTestMode

class CodeforcesStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project) || isUnitTestMode) return
    CodeforcesCourseUpdateChecker.getInstance(project).check()
  }
}