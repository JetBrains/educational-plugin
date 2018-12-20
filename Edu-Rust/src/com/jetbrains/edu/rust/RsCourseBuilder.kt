package com.jetbrains.edu.rust

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.CargoProject
import org.rust.cargo.project.model.cargoProjects
import org.rust.openapiext.pathAsPath
import java.nio.file.Path

class RsCourseBuilder : EduCourseBuilder<RsProjectSettings> {

    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<RsProjectSettings>? =
        RsCourseProjectGenerator(this, course)

    override fun getLanguageSettings(): LanguageSettings<RsProjectSettings> = RsLanguageSettings()

    override fun refreshProject(project: Project) {
        val course = StudyTaskManager.getInstance(project).course ?: return
        val cargoProjects = project.cargoProjects

        val cargoProjectMap = HashMap<VirtualFile, CargoProject>()
        val toAttach = mutableListOf<Path>()
        val toDetach = mutableListOf<CargoProject>()
        for (cargoProject in cargoProjects.allProjects) {
            val rootDir = cargoProject.rootDir
            // we should check existence of manifest file because after study item rename
            // manifest path will be outdated
            if (rootDir == null || !cargoProject.manifest.exists()) {
                toDetach += cargoProject
            } else {
                cargoProjectMap[rootDir] = cargoProject
            }
        }

        course.visitLessons {
            for (task in it.taskList) {
                val taskDir = task.getTaskDir(project) ?: continue
                val cargoProject = cargoProjectMap[taskDir]
                if (cargoProject == null) {
                    val manifestFile = taskDir.findChild(CargoConstants.MANIFEST_FILE) ?: continue
                    toAttach.add(manifestFile.pathAsPath)
                }
            }
            true
        }


        toDetach.forEach { cargoProjects.detachCargoProject(it) }
        // TODO: find out way not to refresh all projects on each `CargoProjectsService.attachCargoProject` call.
        // Now it leads to O(n^2) cargo invocations
        toAttach.forEach { cargoProjects.attachCargoProject(it) }

        cargoProjects.refreshAllProjects()
    }

    override fun initNewTask(lesson: Lesson, task: Task, info: NewStudyItemInfo) {
        if (task.taskFiles.isNotEmpty()) return
        val templateManager = FileTemplateManager.getDefaultInstance()
        val taskFile = TaskFile("src/$LIB_RS", templateManager.getInternalTemplate(LIB_RS).text)
        task.addTaskFile(taskFile)
        val testText = templateManager.getInternalTemplate(TESTS_RS).text
        task.addTestsTexts("tests/$TESTS_RS", testText)
        val packageName = lesson.course.name.toPackageName()
        val additionalText = templateManager.getInternalTemplate(CargoConstants.MANIFEST_FILE)
            .getText(mapOf("PACKAGE_NAME" to packageName))
        task.addAdditionalFile(CargoConstants.MANIFEST_FILE, additionalText)
    }

    companion object {
        private const val LIB_RS = "lib.rs"
        private const val TESTS_RS = "tests.rs"
    }
}
