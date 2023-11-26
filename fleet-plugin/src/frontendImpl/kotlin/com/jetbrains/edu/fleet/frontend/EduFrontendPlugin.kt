package com.jetbrains.edu.fleet.frontend

import com.jetbrains.edu.fleet.common.CourseEntity
import com.jetbrains.edu.fleet.common.generation.CourseProjectGenerator
import com.jetbrains.edu.fleet.common.marketplace.MarketplaceConnector
import com.jetbrains.edu.fleet.common.yaml.YamlDeepLoader
import com.jetbrains.edu.fleet.common.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.fleet.frontend.actions.createImportCourseAction
import com.jetbrains.edu.fleet.frontend.actions.createImportMarketplaceCourseAction
import com.jetbrains.edu.fleet.frontend.ui.*
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import fleet.api.FileAddress
import fleet.api.child
import fleet.api.exists
import fleet.common.fs.SharedWorkspaceRootEntity
import fleet.common.fs.fsService
import fleet.frontend.actions.actions
import fleet.frontend.entityRenderer
import fleet.frontend.fsd.showSelectFolderDialog
import fleet.frontend.layout.*
import fleet.frontend.navigation.attachToWorkspace
import fleet.frontend.toolEntityRenderer
import fleet.kernel.*
import fleet.kernel.plugins.ContributionScope
import fleet.kernel.plugins.Plugin
import fleet.kernel.plugins.PluginScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import noria.NoriaContext
import noria.model.Trigger


class EduFrontendPlugin : Plugin<Unit> {
  companion object : Plugin.Key<Unit>

  override val key: Plugin.Key<Unit> = EduFrontendPlugin

  override fun ContributionScope.load(pluginScope: PluginScope) {
    actions(
      createImportCourseAction(),
      createImportMarketplaceCourseAction(),
      courseViewAction
    )
    entityRenderer(CourseIdDialogEntity::class, NoriaContext::courseIdDialog)
    toolEntityRenderer(CourseViewEntity::class, ToolPosition.RightPanel, NoriaContext::renderCourseTreeEntity)

    worker("[edu plugin] load course structure from yaml") {
      supervisorScope {
        launch { loadCourseStructure() }
      }
    }

//    worker("[edu plugin] open course from marketplace") {
//      supervisorScope {
//        launch {
//          launchOnEachEntity<FrontendEntity> {
//            val shipParams = it.shipParams as? OpenLocalWorkspace ?: return@launchOnEachEntity
//            val courseId = shipParams.extra["courseId"] ?: return@launchOnEachEntity
//            createCourse(courseId, kernel)
//          }
//        }
//      }
//    }
  }
}

private suspend fun createCourse(id: String?, kernel: Kernel) {
  val courseId = id?.toInt() ?: return
  MarketplaceConnector.loadCourse(courseId) { course ->
    kernel.saga {
      val window = lastFocusedEntity(WindowEntity::class) ?: return@saga
      showSelectFolderDialog(window) { item, showValidationError ->
        kernel.saga {
          val fileAddress = item.child(course.name)
          val success = CourseProjectGenerator().createCourse(fileAddress, course)
          if (success) {
            attachToWorkspace(window, fileAddress)
            change {
              new(CourseEntity::class) {
                this.course = course
              }
              openTool(newCourseTreeView(), ShowOpts(window = window))
            }
            YamlFormatSynchronizer.saveAll(course, fileAddress)
          }
        }
        ConfirmDialogCommand.CLOSE
      }
    }
  }
}

private suspend fun loadCourseStructure() {
  launchOnEachEntity<SharedWorkspaceRootEntity> { root ->
    if (!root.dirAddress.isEduYamlProject()) return@launchOnEachEntity
    val course = YamlDeepLoader.loadCourse(root.dirAddress) ?: return@launchOnEachEntity
    change {
      new(CourseEntity::class) {
        this.course = course
      }
      val window = lastFocusedWindow()
      openTool(newCourseTreeView(), ShowOpts(window = window))
    }
  }
}

private suspend fun FileAddress.isEduYamlProject(): Boolean {
  val fsApi = requireNotNull(fsService(this)) { "There must be fs service for file $this" }
  return fsApi.exists(child(YamlConfigSettings.COURSE_CONFIG).path)
}

object EduTriggers {
  val Import = Trigger("edu-import")
  val ImportMarketplace = Trigger("edu-import-marketplace")
  val CourseView = Trigger("edu-course-view")
}
