package fleet.edu.frontend.actions

import fleet.api.child
import fleet.edu.common.CourseEntity
import fleet.edu.common.generation.CourseProjectGenerator
import fleet.edu.common.marketplace.MarketplaceConnector
import fleet.edu.common.yaml.YamlFormatSynchronizer
import fleet.edu.frontend.EduTriggers
import fleet.edu.frontend.ui.newCourseTreeView
import fleet.edu.frontend.ui.showCourseIdDialog
import fleet.frontend.actions.FleetDataKeys
import fleet.frontend.actions.kernel
import fleet.frontend.actions.sagaAction
import fleet.frontend.actions.windowEntity
import fleet.frontend.fsd.showSelectFolderDialog
import fleet.frontend.layout.ConfirmDialogCommand
import fleet.frontend.layout.ShowOpts
import fleet.frontend.layout.WindowEntity
import fleet.frontend.layout.openTool
import fleet.frontend.navigation.attachToWorkspace
import fleet.kernel.Kernel
import fleet.kernel.change
import fleet.kernel.saga
import noria.model.Action
import noria.model.ActionPresentation

internal fun createImportMarketplaceCourseAction(): Action {
  return Action(
    perform = sagaAction { actionContext ->
      val window = actionContext.windowEntity
      val kernel = actionContext.kernel
      kernel.changeAsync {
        showCourseIdDialog(window) {
          kernel.saga {
            createCourse(it, window, kernel)
          }
        }
      }
    },
    identifier = "import-marketplace-course",
    requirements = setOf(FleetDataKeys.Kernel, FleetDataKeys.Window),
    triggers = setOf(EduTriggers.ImportMarketplace),
    defaultPresentation = ActionPresentation("Import Marketplace Course"))
}

private suspend fun createCourse(id: String?, window: WindowEntity, kernel: Kernel) {
  val courseId = id?.toInt() ?: return
  MarketplaceConnector.loadCourse(courseId) { course ->
    kernel.saga {
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
