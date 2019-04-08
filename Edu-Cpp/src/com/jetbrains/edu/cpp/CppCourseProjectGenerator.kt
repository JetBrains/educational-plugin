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
    if (baseDir.findChild(CMAKELISTS_FILE_NAME) != null) return

    val toolchain = CPPToolchains.getInstance().defaultToolchain
    val version = CMake.readCMakeVersion(toolchain)

    val params = mapOf("CPP_TOOLCHAIN_VERSION" to version,
                       "PROJECT_NAME" to baseDir.name)
    val template = FileTemplateManager.getDefaultInstance().findInternalTemplate(EDU_MAIN_CMAKELISTS)
    GeneratorUtils.createChildFile(baseDir, CMAKELISTS_FILE_NAME, template.getText(params))
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
    val toolchain = CPPToolchains.getInstance().defaultToolchain
    val version = CMake.readCMakeVersion(toolchain)

    lesson.visitTasks { task, _ ->
      val params = mapOf("CPP_PROJECT_NAME" to getCMakeProjectUniqueName(section, lesson, task),
                         "CPP_TOOLCHAIN_VERSION" to version)
      val template = FileTemplateManager.getDefaultInstance().findInternalTemplate(EDU_CMAKELISTS)

      val cMakeFile = TaskFile()
      cMakeFile.name = CMAKELISTS_FILE_NAME
      cMakeFile.setText(StringUtil.notNullize(template.getText(params)))
      task.addTaskFile(cMakeFile)
      true
    }
  }

  private fun getCMakeProjectUniqueName(section: Section?, lesson: Lesson, task: Task): String {
    val projectName = "lesson${lesson.index}-task${task.index}"
    if (section == null) return projectName
    return "section${section.index}-$projectName"
  }

  companion object {
    private const val EDU_MAIN_CMAKELISTS = "EduMainCMakeLists.txt"
    private const val EDU_CMAKELISTS = "EduCMakeLists.txt"
    private const val CMAKELISTS_FILE_NAME = CMakeListsFileType.FILE_NAME
  }
}
