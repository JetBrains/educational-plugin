package com.jetbrains.edu.learning.projectView


import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.panels.Wrapper
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateStudyItemActionBase.Companion.ITEM_INDEX
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateStudyItemActionBase.Companion.UPDATE_PARENT_CONFIG
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.projectView.CCCourseNode
import com.jetbrains.edu.coursecreator.projectView.CCLessonNode
import com.jetbrains.edu.coursecreator.projectView.CCSectionNode
import com.jetbrains.edu.coursecreator.projectView.CCTaskNode
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import javax.swing.tree.DefaultMutableTreeNode

class EducatorActionsPanel : Wrapper() {

  init {
    val addItemsGroup = DefaultActionGroup()
    addItemsGroup.addAction(NewLessonToolbarAction())
    addItemsGroup.addAction(NewTaskToolbarAction())
    val actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.PROJECT_VIEW_TOOLBAR, addItemsGroup, true)
    setContent(actionToolbar.component)
  }
}

private class NewTaskToolbarAction : CCCreateTask(EducationalCoreIcons.CourseCreator.NewTask) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedObject = getSelectedObject(project) ?: return

    val newLessonPlace = getNewTaskPlace(selectedObject, project.courseDir)
    val newEvent = createNewEvent(e, newLessonPlace)

    super.actionPerformed(newEvent)
  }

  private fun getNewTaskPlace(selectedObject: EduNode<*>, courseDir: VirtualFile): NewItemPlace {

    return when (val item = selectedObject.item) {
      is Lesson -> {
        val itemDir = item.getDir(courseDir) ?: error("Cannot get get directory for $item")
        NewItemPlace(itemDir, item.taskList.size)
      }
      is Task -> {
        val parentLesson = item.parent
        val itemDir = parentLesson.getDir(courseDir) ?: error("Cannot get get directory for $parentLesson")
        NewItemPlace(itemDir, item.index)
      }
      else -> error("Unexpected userObject $selectedObject")
    }
  }

  override fun update(event: AnActionEvent) {
    event.presentation.isVisible = true
    event.presentation.isEnabled = false
    val project = event.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (course.lessons.isEmpty()) {
      return
    }

    val selectedNode = getSelectedObject(project) ?: return
    if (selectedNode !is CCLessonNode && selectedNode !is CCTaskNode) {
      return
    }

    event.presentation.isEnabled = true
  }

}

private class NewLessonToolbarAction : CCCreateLesson(EducationalCoreIcons.CourseCreator.NewLesson) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedObject = getSelectedObject(project)
    val course = project.course ?: return

    val newLessonPlace = getNewLessonPlace(course, selectedObject, project.courseDir)
    val newEvent = createNewEvent(e, newLessonPlace)
    super.actionPerformed(newEvent)
  }

  private fun getNewLessonPlace(course: Course, selectedObject: Any?, courseDir: VirtualFile): NewItemPlace {
    return when (selectedObject) {
      null -> {
        NewItemPlace(courseDir, course.items.size)
      }
      is CCCourseNode -> {
        val lastIndex = course.items.lastOrNull()?.index ?: 0
        NewItemPlace(courseDir, lastIndex)
      }
      is CCSectionNode -> {
        val section = selectedObject.item
        val lastIndex = section.items.lastOrNull()?.index ?: 0
        val parentDir = section.getDir(courseDir) ?: error("Cannot get directory for $section")
        NewItemPlace(parentDir, lastIndex)
      }
      is CCLessonNode -> {
        val selectedLesson = selectedObject.item
        val itemDir = selectedLesson.getDir(courseDir) ?: error("Cannot get get directory for $selectedLesson")
        NewItemPlace(itemDir, selectedLesson.index)
      }
      is CCTaskNode -> {
        val parentLesson = selectedObject.item.parent
        val itemDir = parentLesson.getDir(courseDir) ?: error("Cannot get get directory for $parentLesson")
        NewItemPlace(itemDir, parentLesson.index)
      }
      else -> error("Unexpected userObject $selectedObject")
    }
  }

  override fun update(event: AnActionEvent) {
    event.presentation.isEnabled = false
    event.presentation.isVisible = true
    val project = event.getData(CommonDataKeys.PROJECT) ?: return
    StudyTaskManager.getInstance(project).course ?: return

    val selectedNode = getSelectedObject(project) ?: return
    if (selectedNode !is CCCourseNode
        && selectedNode !is CCSectionNode
        && selectedNode !is CCLessonNode
        && selectedNode !is CCTaskNode
    ) {
      return
    }

    event.presentation.isEnabled = true
  }
}

private class NewItemPlace(val directory: VirtualFile, val index: Int)

private fun getSelectedObject(project: Project): EduNode<*>? {
  val projectView = ProjectView.getInstance(project)
  val selectionPath = projectView.getProjectViewPaneById(CourseViewPane.ID).tree.selectionPath
  return (selectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? EduNode<*>
}


private fun createNewEvent(e: AnActionEvent, newItemPlace: NewItemPlace): AnActionEvent {
  val context = DataContext { dataId ->
    when {
      CommonDataKeys.PROJECT.`is`(dataId) -> e.project
      CommonDataKeys.VIRTUAL_FILE_ARRAY.`is`(dataId) -> arrayOf(newItemPlace.directory)
      ITEM_INDEX.`is`(dataId) -> newItemPlace.index
      UPDATE_PARENT_CONFIG.`is`(dataId) -> true
      else -> null
    }
  }
  return AnActionEvent(e.inputEvent, context, ActionPlaces.PROJECT_VIEW_TOOLBAR, e.presentation, e.actionManager, e.modifiers)
}