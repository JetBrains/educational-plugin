package com.jetbrains.edu.php

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.php.composer.ComposerDataService
import com.jetbrains.php.composer.ComposerUtils
import com.jetbrains.php.composer.actions.ComposerInstallAction
import com.jetbrains.php.composer.actions.ComposerOptionsManager
import com.jetbrains.php.composer.execution.phar.PharComposerExecution

class PhpCourseProjectGenerator(
  builder: PhpCourseBuilder,
  course: Course
) : CourseProjectGenerator<PhpProjectSettings>(builder, course) {

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {
    super.createAdditionalFiles(project, baseDir, isNewCourse)
    downloadPhar(project)
    createComposerFile(project, baseDir)
  }

  private fun createComposerFile(project: Project, baseDir: VirtualFile) {
    val composerFile =
      baseDir.findChild(ComposerUtils.CONFIG_DEFAULT_FILENAME)
      ?: createChildFile(project,
                         baseDir,
                         ComposerUtils.CONFIG_DEFAULT_FILENAME,
                         getInternalTemplateText(ComposerUtils.CONFIG_DEFAULT_FILENAME))

    if (composerFile != null) {
      installComposer(project, composerFile)
    }
  }

  private fun downloadPhar(project: Project) {
    if (isUnitTestMode) {
      return
    }

    val interpreterId = myCourseBuilder.getLanguageSettings().getSettings().phpInterpreter?.id
    val file = ComposerUtils.downloadPhar(project, null, project.basePath)
    val pharComposerExecution = if (file != null) {
      PharComposerExecution(interpreterId, file.path, false)
    }
    else {
      PharComposerExecution(interpreterId, null, true)
    }
    project.getService(ComposerDataService::class.java).apply {
      composerExecution = pharComposerExecution
    }
  }

  private fun installComposer(project: Project, composerFile: VirtualFile) {
    ApplicationManager.getApplication().invokeLater {
      val executor = ComposerInstallAction.createExecutor(
        project,
        ComposerDataService.getInstance(project).composerExecution,
        composerFile,
        ComposerOptionsManager.DEFAULT_COMMAND_LINE_OPTIONS,
        null,
        true
      )
      executor.execute()
    }
  }
}