package com.jetbrains.edu.rust

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.cargoProjects

class RsCourseBuilder : EduCourseBuilder<RsProjectSettings> {

    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<RsProjectSettings>? =
        RsCourseProjectGenerator(this, course)

    override fun getLanguageSettings(): LanguageSettings<RsProjectSettings> = RsLanguageSettings()

    override fun refreshProject(project: Project) {
        project.cargoProjects.refreshAllProjects()
    }

    override fun initNewTask(lesson: Lesson, task: Task, info: NewStudyItemInfo) {
        if (task.taskFiles.isNotEmpty()) return
        val templateManager = FileTemplateManager.getDefaultInstance()
        val taskFile = TaskFile("src/${LIB_RS}", templateManager.getInternalTemplate(LIB_RS).text)
        task.addTaskFile(taskFile)
        val testText = templateManager.getInternalTemplate(TESTS_RS).text
        task.addTestsTexts("tests/$TESTS_RS", testText)
        val additionalText = templateManager.getInternalTemplate(CargoConstants.MANIFEST_FILE)
            // TODO: sanitize name
            .getText(mapOf("PROJECT_NAME" to lesson.course.name))
        task.addAdditionalFile(CargoConstants.MANIFEST_FILE, additionalText)
    }

    companion object {
        private const val LIB_RS = "lib.rs"
        private const val TESTS_RS = "tests.rs"
    }
}
