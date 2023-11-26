package fleet.edu.frontend

import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.rhizomedb.Entrypoint
import fleet.api.FileAddress
import fleet.api.child
import fleet.api.exists
import fleet.common.fs.SharedWorkspaceRootEntity
import fleet.common.fs.fsService
import fleet.dock.connectors.OpenLocalWorkspace
import fleet.edu.common.CourseEntity
import fleet.edu.common.generation.CourseProjectGenerator
import fleet.edu.common.marketplace.MarketplaceConnector
import fleet.edu.common.yaml.YamlDeepLoader
import fleet.edu.common.yaml.YamlFormatSynchronizer
import fleet.edu.frontend.actions.createImportCourseAction
import fleet.edu.frontend.actions.createImportMarketplaceCourseAction
import fleet.edu.frontend.ui.*
import fleet.frontend.FrontendEntity
import fleet.frontend.actions.actions
import fleet.frontend.entityRenderer
import fleet.frontend.fsd.showSelectFolderDialog
import fleet.frontend.layout.*
import fleet.frontend.navigation.attachToWorkspace
import fleet.frontend.toolEntityRenderer
import fleet.kernel.*
import fleet.kernel.plugins.register
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import noria.NoriaContext
import noria.model.Trigger


@Entrypoint
fun ChangeScope.edu() {
  register {
    actions (
      createImportCourseAction(),
      createImportMarketplaceCourseAction(),
      courseViewAction
    )
    entityRenderer(CourseIdDialogEntity::class, NoriaContext::courseIdDialog)
    toolEntityRenderer(CourseViewEntity::class, ToolPosition.RightPanel, NoriaContext::renderCourseTreeEntity)
  }

  worker("[edu plugin] load course structure from yaml") {
    supervisorScope {
      launch { loadCourseStructure() }
    }
  }

  worker("[edu plugin] open course from marketplace") {
    supervisorScope {
      launch {
        launchOnEachEntity<FrontendEntity> {
          val shipParams = it.shipParams as? OpenLocalWorkspace ?: return@launchOnEachEntity
          val courseId = shipParams.extra["courseId"] ?: return@launchOnEachEntity
          createCourse(courseId, kernel)
        }
      }
    }
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
