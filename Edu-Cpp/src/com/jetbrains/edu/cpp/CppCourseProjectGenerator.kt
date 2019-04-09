package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    val isDone = super.beforeProjectGenerated()

    if (isDone) {
      addCMakeListsToEachTaskInCourse()
    }
    return isDone
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return
    GeneratorUtils.createChildFile(baseDir, CMakeListsFileType.FILE_NAME, getFileTemplateTextWithParams(EDU_MAIN_CMAKELISTS, baseDir.name))
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)

    CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
  }

  private fun addCMakeListsToEachTaskInCourse() {
    for (item in myCourse.items) {
      if (item is Lesson) {
        addCMakeListsForEachTaskInLesson(null, item)
      }
      else if (item is Section) {
        item.visitLessons { lesson ->
          addCMakeListsForEachTaskInLesson(item, lesson)
          true
        }
      }
    }
  }

  private fun addCMakeListsForEachTaskInLesson(section: Section?, lesson: Lesson) {
    lesson.visitTasks { task, _ ->
      val cMakeFile = TaskFile()
      cMakeFile.name = CMakeListsFileType.FILE_NAME
      cMakeFile.setText(StringUtil.notNullize(
        getFileTemplateTextWithParams(EDU_CMAKELISTS, getCMakeProjectUniqueName(section, lesson, task))))
      task.addTaskFile(cMakeFile)
      true
    }
  }

  private fun getCMakeProjectUniqueName(section: Section?, lesson: Lesson, task: Task): String {
    val projectName = "${EduNames.LESSON}${lesson.index}-${EduNames.TASK}${task.index}"
    if (section == null) return projectName
    return "${EduNames.SECTION}${section.index}-$projectName"
  }

  private fun getFileTemplateTextWithParams(templateName: String, cppProjectName: String): String {
    val toolchain = CPPToolchains.getInstance().defaultToolchain
    val version = CMake.readCMakeVersion(toolchain)

    val params = mapOf(PROJECT_NAME to cppProjectName,
                       CPP_TOOLCHAIN_VERSION to version)
    return FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName).getText(params)
  }

  companion object {
    private const val EDU_MAIN_CMAKELISTS = "EduMainCMakeLists.txt"
    private const val EDU_CMAKELISTS = "EduCMakeLists.txt"
    private const val PROJECT_NAME = "PROJECT_NAME"
    private const val CPP_TOOLCHAIN_VERSION = "CPP_TOOLCHAIN_VERSION"
  }
}
