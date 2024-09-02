package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.CCUtils.updateHigherElements
import com.jetbrains.edu.coursecreator.handlers.StudyItemRefactoringHandler
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isConfigFile
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CCRemoveSection : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (project == null || selectedFiles == null || selectedFiles.size != 1) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course ?: return
    val file = selectedFiles[0]
    val section = course.getSection(file.name) ?: return
    val courseDir = project.courseDir
    for (child in VfsUtil.getChildren(file)) {
      if (courseDir.findChild(child.name) != null) {
        Messages.showInfoMessage(
          message("error.failed.to.unwrap.section.message", child.name),
          message("error.failed.to.unwrap.section")
        )
        return
      }
    }
    val lessonsFromSection = section.lessons
    lessonsFromSection.forEach { lesson ->
      StudyItemRefactoringHandler.processBeforeLessonMovement(project, lesson, courseDir)
    }
    if (removeSectionDir(file, courseDir)) {
      val sectionIndex = section.index
      lessonsFromSection.forEach { lesson ->
        lesson.index += sectionIndex - 1
        lesson.parent = lesson.course
      }
      updateHigherElements(
        courseDir.children, { it: VirtualFile -> course.getItem(it.name) },
        sectionIndex - 1, lessonsFromSection.size - 1
      )
      course.addLessons(lessonsFromSection)
      course.sortItems()
      saveItem(course)
    }
    ProjectView.getInstance(project).refresh()
  }

  private fun removeSectionDir(file: VirtualFile, courseDir: VirtualFile): Boolean {
    return ApplicationManager.getApplication().runWriteAction(object : Computable<Boolean> {
      override fun compute(): Boolean {
        val children = VfsUtil.getChildren(file)
        for (child in children) {
          try {
            if (isConfigFile(child)) {
              child.delete(this)
            }
            else {
              child.move(this, courseDir)
            }
          }
          catch (e: IOException) {
            LOG.error("Failed to move lesson " + child.name + " out of section")
            return false
          }
        }
        try {
          file.delete(this)
        }
        catch (e: IOException) {
          LOG.error("Failed to delete section " + file.name)
          return false
        }
        return true
      }
    })
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    if (project == null) return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!isCourseCreator(project)) return
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (selectedFiles != null && selectedFiles.size == 1) {
      val section = course.getSection(selectedFiles[0].name)
      if (section != null) {
        presentation.isEnabledAndVisible = true
      }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    private val LOG = Logger.getInstance(CCRemoveSection::class.java)
    const val ACTION_ID: @NonNls String = "Educational.Educator.CCRemoveSection"
  }
}
