package com.jetbrains.edu.coursecreator.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.StudyUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder

class CCModuleBuilder(private val myCourse: Course) : EduCourseModuleBuilder() {

  override fun createModule(moduleModel: ModifiableModuleModel): Module {
    val module = super.createModule(moduleModel)
    val project = module.project

    val language = myCourse.languageById
    if (language == null) {
      LOG.error("Can't find language by ${myCourse.languageID}")
      return module
    }
    val configurator = EduPluginConfigurator.INSTANCE.forLanguage(language)
    if (configurator == null) {
      LOG.error("EduPluginConfigurator for language ${language.displayName} not found")
      return module
    }

    StudyTaskManager.getInstance(project).course = myCourse
    StudyUtils.registerStudyToolWindow(myCourse, project)
    configurator.createCourseModuleContent(moduleModel, project, myCourse, moduleFileDirectory)

    // If we drop `registerPostStartupActivity` modules will not be created
    StartupManager.getInstance(project).registerPostStartupActivity {
      ApplicationManager.getApplication().runWriteAction {
        val lessonDir = CCCreateLesson().createItem(project, project.baseDir, myCourse, false)
        if (lessonDir == null) {
          LOG.error("Failed to create lesson")
          return@runWriteAction
        }
        CCCreateTask().createItem(project, lessonDir, myCourse, false)
      }
    }
    return module
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCModuleBuilder::class.java)
  }
}
