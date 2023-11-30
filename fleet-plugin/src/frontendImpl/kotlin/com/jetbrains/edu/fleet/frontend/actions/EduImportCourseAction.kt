package com.jetbrains.edu.fleet.frontend.actions

import com.jetbrains.edu.fleet.common.CourseEntity
import com.jetbrains.edu.fleet.common.generation.CourseProjectGenerator
import com.jetbrains.edu.fleet.common.marketplace.MarketplaceConnector
import com.jetbrains.edu.fleet.common.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.fleet.frontend.EduTriggers
import com.jetbrains.edu.fleet.frontend.ui.newCourseTreeView
import com.jetbrains.edu.fleet.frontend.ui.showCourseIdDialog
import fleet.api.child
import fleet.frontend.actions.FleetDataKeys
import fleet.frontend.actions.sagaAction
import fleet.frontend.actions.windowEntity
import fleet.frontend.fsd.showSelectFolderDialog
import fleet.frontend.layout.ConfirmDialogCommand
import fleet.frontend.layout.ShowOpts
import fleet.frontend.layout.WindowEntity
import fleet.frontend.layout.openTool
import fleet.frontend.navigation.attachToWorkspace
import fleet.kernel.change
import fleet.kernel.plugins.PluginScope
import fleet.kernel.saga
import noria.model.Action
import noria.model.ActionPresentation

internal fun createImportMarketplaceCourseAction(pluginScope: PluginScope): Action {
  return Action(
    perform = sagaAction { actionContext ->
      val window = actionContext.windowEntity
      pluginScope.changeAsync {
        showCourseIdDialog(window) {
          pluginScope.saga {
            createCourse(it, window, pluginScope)
          }
        }
      }
    },
    identifier = "import-marketplace-course",
    requirements = setOf(FleetDataKeys.Window),
    triggers = setOf(EduTriggers.ImportMarketplace),
    defaultPresentation = ActionPresentation("Import Marketplace Course"))
}

private suspend fun createCourse(id: String?, window: WindowEntity, pluginScope: PluginScope) {
  val courseId = id?.toInt() ?: return
  MarketplaceConnector.loadCourse(courseId) { course ->
    pluginScope.saga {
      showSelectFolderDialog(window) { item, showValidationError ->
        pluginScope.saga {
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
