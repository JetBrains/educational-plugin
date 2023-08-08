package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.library.core.NodeCoreLibraryConfigurator
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

class JsCourseProjectGenerator(builder: JsCourseBuilder, course: Course) : CourseProjectGenerator<JsNewProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: JsNewProjectSettings, onConfigurationFinished: () -> Unit) {
    val interpreter = projectSettings.selectedInterpreter
    if (interpreter == null) {
      // It's ok not to have NodeJS interpreter in tests
      if (!isUnitTestMode) {
        LOG.warn("NodeJS interpreter is not selected")
      }
      return
    }
    NodeJsInterpreterManager.getInstance(project).setInterpreterRef(interpreter.toRef())

    val packageJsonFile = project.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    // Don't install dependencies in headless mode (tests or course creation using `EduCourseCreatorAppStarter` on remote)
    // It doesn't make sense for tests since we don't check it.
    // On remote dependencies will be installed during warmup phase
    if (packageJsonFile != null && !isHeadlessEnvironment) {
      installNodeDependencies(project, packageJsonFile)
    }

    val modalityState = ModalityState.current()
    interpreter.provideCachedVersionOrFetch { version ->
      project.invokeLater(modalityState) {
        if (version != null) {
          val configurator = NodeCoreLibraryConfigurator.getInstance(project)
          configurator.configureAndAssociateWithProject(interpreter, version) {
            onConfigurationFinished()
          }
        }
        else {
          LOG.warn("Couldn't retrieve Node interpreter version")
          @Suppress("UnstableApiUsage")
          val requester = ModuleManager.getInstance(project).modules[0].moduleFile
          NodeSettingsConfigurable.showSettingsDialog(project, requester)
          onConfigurationFinished()
        }
      }
    }
    // Pass empty callback here because Core library configuration will be made asynchronously
    // Before this, we can't consider JS course project is fully configured
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished = {})
  }

  @Throws(IOException::class)
  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    val packageJsonFile = holder.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    if (packageJsonFile == null && !holder.course.isStudy) {
      val templateText = getInternalTemplateText(NodeModuleNamesUtil.PACKAGE_JSON)
      createChildFile(holder, holder.courseDir, NodeModuleNamesUtil.PACKAGE_JSON, templateText)
    }
  }

  companion object {
    private val LOG = logger<JsCourseProjectGenerator>()
  }
}