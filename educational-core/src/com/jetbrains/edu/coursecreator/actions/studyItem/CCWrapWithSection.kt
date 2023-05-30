package com.jetbrains.edu.coursecreator.actions.studyItem

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.CCUtils.wrapIntoSection
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.presentableTitleName
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SECTION
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle.lazyMessage
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import org.jetbrains.annotations.NonNls
import java.util.*
import java.util.stream.Collectors

class CCWrapWithSection : DumbAwareAction(
  lazyMessage("action.wrap.with.section.text"),
  lazyMessage("action.wrap.with.section.description"),
  null
) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
    if (project == null || virtualFiles == null) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course ?: return
    val lessonsToWrap = getLessonsToWrap(virtualFiles, course)
    wrapLessonsIntoSection(project, course, lessonsToWrap)
    val configurator = course.configurator ?: return
    configurator.courseBuilder.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    if (project == null || !isCourseCreator(project)) {
      return
    }
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
    if (virtualFiles.isNullOrEmpty()) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course ?: return
    val lessonsToWrap = getLessonsToWrap(virtualFiles, course)
    if (lessonsToWrap.isNotEmpty()) {
      presentation.isEnabledAndVisible = true
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.Educator.CCWrapWithSection"
    private fun getLessonsToWrap(virtualFiles: Array<VirtualFile>, course: Course): ArrayList<Lesson> {
      val lessonsToWrap = ArrayList<Lesson>()
      for (file in virtualFiles) {
        val lesson = course.getLesson(file.name)
        if (lesson != null) {
          lessonsToWrap.add(lesson)
        }
      }
      return if (!isConsecutive(lessonsToWrap)) ArrayList() else lessonsToWrap
    }

    private fun isConsecutive(lessonsToWrap: ArrayList<Lesson>): Boolean {
      val indexes = lessonsToWrap.stream().map { it: Lesson -> it.index }.collect(Collectors.toList())
      if (indexes.isEmpty()) return false
      if (indexes.stream().distinct().count() != indexes.size.toLong()) {
        return false
      }
      val max = Collections.max(indexes)
      val min = Collections.min(indexes)
      return max - min == indexes.size - 1
    }

    fun wrapLessonsIntoSection(project: Project, course: Course, lessonsToWrap: List<Lesson>) {
      if (lessonsToWrap.isEmpty()) {
        return
      }
      val sectionIndex = course.sections.size + 1
      val validator: InputValidator = CCStudyItemPathInputValidator(project, course, StudyItemType.SECTION_TYPE, project.courseDir)
      val sectionName = Messages.showInputDialog(
        message("action.wrap.with.section.enter.name"),
        StudyItemType.SECTION_TYPE.presentableTitleName, null, SECTION + sectionIndex, validator
      ) ?: return
      wrapIntoSection(project, course, lessonsToWrap, sectionName)
    }
  }
}
