package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.File

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

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

    val toolchain = CPPToolchains.getInstance().defaultToolchain
    val version = CMake.readCMakeVersion(toolchain)

    myCourse.visitLessons { lesson ->
      for (task in lesson.taskList) {
        val params = mapOf("CPP_PROJECT_NAME" to getCMakeProjectUniqueName(lesson, task),
                           "CPP_TOOLCHAIN_VERSION" to version)
        val template = FileTemplateManager.getDefaultInstance().findInternalTemplate(EDU_CMAKELISTS)
        val taskDir = task.getDir(project)
        GeneratorUtils.createChildFile(taskDir!!, CMAKELISTS_FILE_NAME, template.getText(params))
      }
      true
    }

    val fileCMake = File(project.basePath)
    CMakeWorkspace.getInstance(project).selectProjectDir(fileCMake)
  }


  private fun getCMakeProjectUniqueName(lesson: Lesson, task: Task): String {
    val taskName = task.name.replace("[^A-Za-z0-9]".toRegex(), "")
    val projectName = "${lesson.id}-${taskName}"

    if (lesson.section == null) {
      return projectName
    }
    return "${lesson.parent.id}-$projectName"
  }

  companion object {
    private const val EDU_MAIN_CMAKELISTS = "EduMainCMakeLists.txt"
    private const val EDU_CMAKELISTS = "EduCMakeLists.txt"
    private const val CMAKELISTS_FILE_NAME = CMakeListsFileType.FILE_NAME
  }
}
