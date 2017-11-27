package com.jetbrains.edu.coursecreator.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.generation.CourseModuleBuilder

class CCModuleBuilder(private val myCourse: Course) : CourseModuleBuilder() {

  override val course: Course? get() = myCourse

  override fun createModule(moduleModel: ModifiableModuleModel): Module {
    val module = super.createModule(moduleModel)
    val project = module.project

    val configurator = configurator(myCourse) ?: return module

    StudyTaskManager.getInstance(project).course = myCourse
    configurator.courseBuilder.createCourseModuleContent(moduleModel, project, myCourse, moduleFileDirectory)

    val createInitialStructure = Runnable {
      ApplicationManager.getApplication().runWriteAction {
        val lessonDir = CCCreateLesson().createItem(project, project.baseDir, myCourse, false)
        if (lessonDir == null) {
          LOG.error("Failed to create lesson")
          return@runWriteAction
        }
        CCCreateTask().createItem(project, lessonDir, myCourse, false)
      }
    }
    if (ApplicationManager.getApplication().isUnitTestMode) {
      createInitialStructure.run()
    } else {
      // If we drop `registerPostStartupActivity` modules will not be created
      StartupManager.getInstance(project).registerPostStartupActivity(createInitialStructure)
    }

    return module
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCModuleBuilder::class.java)
  }
}
