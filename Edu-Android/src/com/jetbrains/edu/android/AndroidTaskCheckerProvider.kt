package com.jetbrains.edu.android

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.gradle.GradleEduTaskChecker
import com.jetbrains.edu.learning.checker.gradle.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class AndroidTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun getEduTaskChecker(task: EduTask, project: Project): GradleEduTaskChecker = AndroidChecker(task, project)

  override fun mainClassForFile(project: Project, file: VirtualFile): String? = null
}
