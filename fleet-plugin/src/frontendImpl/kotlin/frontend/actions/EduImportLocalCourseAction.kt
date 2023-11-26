package fleet.edu.frontend.actions

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.json.readCourseJson
import fleet.api.child
import fleet.edu.common.CourseEntity
import fleet.edu.common.generation.CourseProjectGenerator
import fleet.edu.common.yaml.YamlFormatSynchronizer
import fleet.edu.frontend.EduTriggers
import fleet.edu.frontend.ui.newCourseTreeView
import fleet.frontend.actions.FleetDataKeys
import fleet.frontend.actions.sagaAction
import fleet.frontend.actions.windowEntity
import fleet.frontend.fsd.showNativeOpenDialog
import fleet.frontend.fsd.showSelectFolderDialog
import fleet.frontend.layout.ConfirmDialogCommand
import fleet.frontend.layout.ShowOpts
import fleet.frontend.layout.openTool
import fleet.frontend.navigation.attachToWorkspace
import fleet.kernel.change
import fleet.kernel.kernel
import fleet.kernel.saga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import noria.model.Action
import noria.model.ActionPresentation
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

internal fun createImportCourseAction(): Action {
  return Action(
    perform = sagaAction { actionContext ->
      val window = actionContext.windowEntity
      val kernel = kernel()

      showNativeOpenDialog(window) { item ->
        withContext(Dispatchers.IO) {
          ZipFile(File(item.path.joinToString()))
        }.use {
          val entry = it.getEntry(COURSE_META_FILE)
          val reader = { it.getInputStream(entry).reader(StandardCharsets.UTF_8) }
          val course = readCourseJson(reader) ?: return@showNativeOpenDialog ConfirmDialogCommand.CLOSE

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
          ConfirmDialogCommand.CLOSE
        }
      }
    },
    identifier = "import-local-course",
    requirements = setOf(FleetDataKeys.Kernel, FleetDataKeys.Window),
    triggers = setOf(EduTriggers.Import),
    defaultPresentation = ActionPresentation("Import Course"))
}