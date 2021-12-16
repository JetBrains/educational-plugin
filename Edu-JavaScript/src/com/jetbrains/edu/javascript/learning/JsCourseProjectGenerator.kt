package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

class JsCourseProjectGenerator(builder: JsCourseBuilder, course: Course) : CourseProjectGenerator<JsNewProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: JsNewProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    val interpreter = projectSettings.selectedInterpreter
    NodeJsInterpreterManager.getInstance(project).setInterpreterRef(interpreter.toRef())
    val modalityState = ModalityState.current()
    interpreter.provideCachedVersionOrFetch { version ->
      invokeLater(modalityState, project.disposed) {
        if (version != null) {
          configureAndAssociateWithProject(project, interpreter, version)
        }
        else {
          LOG.warn("Couldn't retrieve Node interpreter version")
          @Suppress("UnstableApiUsage")
          val requester = ModuleManager.getInstance(project).modules[0].moduleFile
          // BACKCOMPAT: 2021.2
          @Suppress("DEPRECATION")
          ShowSettingsUtil.getInstance().editConfigurable(project, NodeSettingsConfigurable(project, requester, true))
        }
      }
    }
  }

  @Throws(IOException::class)
  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {
    var packageJsonFile = baseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    if (packageJsonFile == null && !myCourse.isStudy) {
      val templateText = getInternalTemplateText(NodeModuleNamesUtil.PACKAGE_JSON)
      packageJsonFile = createChildFile(project, baseDir, NodeModuleNamesUtil.PACKAGE_JSON, templateText)
    }
    if (packageJsonFile != null && !isUnitTestMode) {
      installNodeDependencies(project, packageJsonFile)
    }
  }

  companion object {
    private val LOG = logger<JsCourseProjectGenerator>()
  }
}