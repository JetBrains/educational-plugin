package com.jetbrains.edu.php

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.invokeLater
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

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> =
    listOfNotNull(createComposerFile(holder))

  override fun afterProjectGenerated(project: Project, projectSettings: PhpProjectSettings, onConfigurationFinished: () -> Unit) {
    if (!isUnitTestMode) {
      downloadPhar(project, projectSettings)
      installComposer(project)
    }
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
  }

  private fun createComposerFile(holder: CourseInfoHolder<Course>): EduFile? {
    val composerFile = holder.courseDir.findChild(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    if (composerFile != null) return null

    return EduFile(ComposerUtils.CONFIG_DEFAULT_FILENAME, InMemoryTextualContents(
      getInternalTemplateText(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    ))
  }

  private fun downloadPhar(project: Project, projectSettings: PhpProjectSettings) {
    val interpreterId = projectSettings.phpInterpreter?.id
    val file = ComposerUtils.downloadPhar(project, null, project.basePath)
    val pharComposerExecution = if (file != null) {
      PharComposerExecution(interpreterId, file.path, false)
    }
    else {
      PharComposerExecution(interpreterId, null, true)
    }
    ComposerDataService.getInstance(project).composerExecution = pharComposerExecution
  }

  private fun installComposer(project: Project) {
    val courseDir = project.courseDir
    val composerFile = courseDir.findChild(ComposerUtils.CONFIG_DEFAULT_FILENAME) ?: return
    project.invokeLater {
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
