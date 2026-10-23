package com.jetbrains.edu.python.learning

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.jetbrains.edu.learning.EnvironmentAwareCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.python.PythonModuleTypeBase

class PyCourseProjectGenerator(
  builder: EnvironmentAwareCourseBuilder<PyLanguageEnvironment>,
  course: Course
) : CourseProjectGenerator<PyLanguageEnvironment>(builder, course) {

  override suspend fun prepareToOpen(project: Project, module: Module) {
    super.prepareToOpen(project, module)
    // Python interpreter assignment requires a Python-aware module since 263
    WorkspaceModel.getInstance(project).update("Set Python course module type") { storage ->
      val entity = storage.resolve(ModuleId(module.name))
      if (entity == null) {
        LOG.error("Can't change the course module type to Python module: ${module.name}")
        return@update
      }
      storage.modifyModuleEntity(entity) {
        type = ModuleTypeId(PythonModuleTypeBase.getInstance().id)
      }
    }
  }

  companion object {
    val LOG = logger<PyCourseProjectGenerator>()
  }
}