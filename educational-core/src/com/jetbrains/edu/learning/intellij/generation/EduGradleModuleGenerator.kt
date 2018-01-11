package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testTextMap
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createDescriptionFiles
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

object EduGradleModuleGenerator {
    private val FAILED_MESSAGE = "Failed to generate gradle wrapper"
    private val requestor = EduGradleModuleGenerator.javaClass

    @JvmStatic
    fun createModule(baseDir: VirtualFile, name: String): VirtualFile = VfsUtil.createDirectoryIfMissing(baseDir, name)

    @JvmStatic
    @Throws(IOException::class)
    fun createTaskModule(lessonDir: VirtualFile, task: Task) {
        val taskDirName = EduNames.TASK + task.index
        val moduleDir = EduGradleModuleGenerator.createModule(lessonDir, taskDirName)
        for (taskFile in task.getTaskFiles().values) {
            GeneratorUtils.createTaskFile(moduleDir, taskFile)
        }
        for ((path, text) in task.testTextMap) {
            GeneratorUtils.createChildFile(moduleDir, path, text)
        }
        if (CCUtils.COURSE_MODE == task.lesson.course.courseMode) {
            createDescriptionFiles(moduleDir, task)
        }
    }

    @Throws(IOException::class)
    private fun createLessonModule(moduleDir: VirtualFile, lesson: Lesson) {
        val lessonDir = VfsUtil.createDirectoryIfMissing(moduleDir, EduNames.LESSON + lesson.index)
        val taskList = lesson.getTaskList()
        for ((i, task) in taskList.withIndex()) {
            task.index = i + 1
            createTaskModule(lessonDir, task)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createUtilModule(course: Course, courseDir: VirtualFile) {
        val additionalMaterials = course.additionalMaterialsTask ?: return
        createUtilModule(additionalMaterials, courseDir)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createUtilModule(additionalMaterials: Task, courseDir: VirtualFile) {
        val sourceDir = additionalMaterials.sourceDir ?: return
        val utilFiles = mutableMapOf<String, String>()
        additionalMaterials.getTaskFiles().mapValuesTo(utilFiles) { (_, v) -> v.text }
        additionalMaterials.testsText.filterTo(utilFiles) { (path, _) -> path.contains(EduNames.UTIL) }
        if (utilFiles.isEmpty()) {
            return
        }

        val utilDir = EduGradleModuleGenerator.createModule(courseDir, EduNames.UTIL)
        for ((key, value) in utilFiles) {
            val path = if (sourceDir.isEmpty()) PathUtil.getFileName(key) else "$sourceDir/${PathUtil.getFileName(key)}"
            GeneratorUtils.createChildFile(utilDir, path, value)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createCourseContent(course: Course, courseDir: VirtualFile) {
        val lessons = course.lessons
        for ((i, lesson) in lessons.withIndex()) {
            lesson.index = i + 1
            createLessonModule(courseDir, lesson)
        }

        createUtilModule(course, courseDir)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createProjectGradleFiles(projectPath: String, projectName: String, buildGradleTemplateName: String) {
        val projectDir = VfsUtil.findFileByIoFile(File(FileUtil.toSystemDependentName(projectPath)), true) ?: return

        val buildTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(buildGradleTemplateName)
        createChildFile(projectDir, GradleConstants.DEFAULT_SCRIPT_NAME, buildTemplate.text)

        val settingsTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(GradleConstants.SETTINGS_FILE_NAME)
        createChildFile(projectDir, GradleConstants.SETTINGS_FILE_NAME, settingsTemplate.text.replace("\$PROJECT_NAME\$", projectName))
    }
}
