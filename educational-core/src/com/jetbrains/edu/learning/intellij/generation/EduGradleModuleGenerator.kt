package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createDescriptionFiles
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

object EduGradleModuleGenerator {
    private val LOG = Logger.getInstance(EduModuleBuilderUtils::class.java)
    private val FAILED_MESSAGE = "Failed to generate gradle wrapper"
    private val requestor = EduGradleModuleGenerator.javaClass

    @JvmStatic
    fun createModule(baseDir: VirtualFile, name: String): EduGradleModule {
        val moduleDir = VfsUtil.createDirectoryIfMissing(baseDir, name)
        val srcDir = VfsUtil.createDirectoryIfMissing(moduleDir, EduNames.SRC)
        val testDir = VfsUtil.createDirectoryIfMissing(moduleDir, EduNames.TEST)
        return EduGradleModule(srcDir, testDir)
    }

    @Throws(IOException::class)
    private fun createTests(task: Task, testDir: VirtualFile) {
        for ((path, text) in getTestTexts(task)) {
            GeneratorUtils.createChildFile(testDir, PathUtil.getFileName(path), text)
        }
    }


    private fun getTestTexts(task: Task): Map<String, String> {
        val additionalMaterials = task.lesson.course.additionalMaterialsTask
        if (task.testsText.isEmpty() && additionalMaterials != null) {
            val lessonDirName = EduNames.LESSON + task.lesson.index
            val taskDirName = EduNames.TASK + task.index
            return additionalMaterials.testsText.filterKeys { key -> key.contains("$lessonDirName/$taskDirName/") }
        }
        return task.testsText
    }


    @JvmStatic
    @Throws(IOException::class)
    fun createTaskModule(lessonDir: VirtualFile, task: Task) {
        val taskDirName = EduNames.TASK + task.index
        val (src, test) = EduGradleModuleGenerator.createModule(lessonDir, taskDirName)
        for (taskFile in task.getTaskFiles().values) {
            GeneratorUtils.createTaskFile(src, taskFile)
        }
        createTests(task, test)
        if (CCUtils.COURSE_MODE == task.lesson.course.courseMode) {
            createDescriptionFiles(src, task)
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
    fun createUtilModule(course: Course, moduleDir: VirtualFile) {
        val additionalMaterials = course.additionalMaterialsTask ?: return
        createUtilModule(additionalMaterials, moduleDir)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createUtilModule(additionalMaterials: Task, moduleDir: VirtualFile) {
        val utilFiles = mutableMapOf<String, String>()
        additionalMaterials.getTaskFiles().mapValuesTo(utilFiles) { (_, v) -> v.text }
        additionalMaterials.testsText.filterTo(utilFiles) { (path, _) -> path.contains(EduNames.UTIL) }
        if (utilFiles.isEmpty()) {
            return
        }
        val (src, _) = EduGradleModuleGenerator.createModule(moduleDir, EduNames.UTIL)
        for ((key, value) in utilFiles) {
            GeneratorUtils.createChildFile(src, PathUtil.getFileName(key), value)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createCourseContent(course: Course, moduleDir: VirtualFile) {
        val lessons = course.lessons
        for ((i, lesson) in lessons.withIndex()) {
            lesson.index = i + 1
            createLessonModule(moduleDir, lesson)
        }

        createUtilModule(course, moduleDir)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createProjectGradleFiles(projectPath: String, projectName: String) {
        val projectDir = VfsUtil.findFileByIoFile(File(FileUtil.toSystemDependentName(projectPath)), true) ?: return

        val buildTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(GradleConstants.DEFAULT_SCRIPT_NAME)
        createChildFile(projectDir, GradleConstants.DEFAULT_SCRIPT_NAME, buildTemplate.text)

        val settingsTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(GradleConstants.SETTINGS_FILE_NAME)
        createChildFile(projectDir, GradleConstants.SETTINGS_FILE_NAME, settingsTemplate.text.replace("\$PROJECT_NAME\$", projectName))
    }
}




