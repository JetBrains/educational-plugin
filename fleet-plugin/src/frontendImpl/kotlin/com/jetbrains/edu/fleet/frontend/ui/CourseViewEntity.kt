package com.jetbrains.edu.fleet.frontend.ui

import com.jetbrains.rhizomedb.byEntityType
import com.jetbrains.edu.fleet.common.CourseEntity
import com.jetbrains.edu.fleet.frontend.EduTriggers
import fleet.frontend.actions.FleetDataKeys
import fleet.frontend.actions.sagaAction
import fleet.frontend.actions.windowEntity
import fleet.frontend.icons.IconKeys
import fleet.frontend.layout.*
import fleet.frontend.ui.db.dbCell
import fleet.kernel.ChangeScope
import fleet.kernel.change
import fleet.util.UID
import noria.NoriaContext
import noria.model.Action
import noria.model.ActionPresentation
import noria.model.components.TreeState
import noria.ui.components.list.SpeedSearchOptions
import noria.ui.components.tree.TreeViewOptions
import noria.ui.components.tree.treeModel
import noria.ui.components.tree.treeView
import noria.ui.core.focusable

interface CourseViewEntity : ToolEntity {
  var treeState: TreeState

  override fun sharedUID(): UID = UID(CourseViewEntity::class.toString())
}

fun ChangeScope.newCourseTreeView(): CourseViewEntity {
  return new(CourseViewEntity::class) {
    displayName = "Course"
    icon = IconKeys.Folder
    treeState = TreeState()
    closingPolicy = toolClosingPolicy()
  }
}

fun NoriaContext.renderCourseTreeEntity(courseViewEntity: CourseViewEntity) {
  val course = byEntityType(CourseEntity::class).firstOrNull()?.course ?: return
  val rootItem = courseNode(course)

  val state = dbCell(read = { courseViewEntity.treeState },
                     write = { courseViewEntity.treeState = it })
  val treeViewOptions = TreeViewOptions(indentLeaves = false, speedSearchOptions = SpeedSearchOptions.Default())
  val treeModel = treeModel(rootItem, options = treeViewOptions, state = state)
  focusable {
    treeView(treeModel)
  }
}

val courseViewAction = Action(
  defaultPresentation = ActionPresentation(name = "Course",
                                           icon = IconKeys.Folder,
                                           tags = listOf(NEW_TOOL_ACTION_TAG)),
  perform = sagaAction { context ->
    change {
      openTool(newCourseTreeView(), ShowOpts(window = context.windowEntity))
    }
  },
  identifier = "open-course-tree",
  requirements = setOf(FleetDataKeys.Window),
  triggers = setOf(EduTriggers.CourseView)
)
