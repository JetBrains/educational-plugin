package com.jetbrains.edu.kotlin

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.kotlin.KtConfigurator.Companion.SUBTASK_TESTS_KT
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.SubtaskUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks
import com.jetbrains.edu.learning.intellij.EduCourseBuilderBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.IOException
import java.util.*

open class KtCourseBuilder : EduCourseBuilderBase() {

    override fun createTaskContent(project: Project, task: Task,
                                   parentDirectory: VirtualFile, course: Course): VirtualFile? {
        KtCourseBuilder.initTask(task)
        ApplicationManager.getApplication().runWriteAction {
            try {
                EduGradleModuleGenerator.createTaskModule(parentDirectory, task)
            } catch (e: IOException) {
                LOG.error("Failed to create task")
            }
        }

        ExternalSystemUtil.refreshProjects(project, GradleConstants.SYSTEM_ID, true, ProgressExecutionMode.MODAL_SYNC)
        return parentDirectory.findChild(EduNames.TASK + task.index)
    }


    override fun createTestsForNewSubtask(project: Project, task: TaskWithSubtasks) {
        val taskDir = task.getTaskDir(project) ?: return
        val prevSubtaskIndex = task.lastSubtaskIndex
        val taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir) ?: return
        val nextSubtaskIndex = prevSubtaskIndex + 1
        val nextSubtaskFileName = SubtaskUtils.getTestFileName(project, nextSubtaskIndex)

        ApplicationManager.getApplication().runWriteAction {
            try {
                val testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(SUBTASK_TESTS_KT)
                if (testsTemplate == null) {
                    return@runWriteAction
                }
                val properties = Properties()
                properties.setProperty("TEST_CLASS_NAME", "Test" + EduNames.SUBTASK_MARKER + nextSubtaskIndex)
                FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskFileName, properties, taskPsiDir)
            } catch (e: Exception) {
                LOG.error(e)
            }
        }
    }

    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? =
            KtProjectGenerator(course)

    companion object {
        private val LOG = Logger.getInstance(KtCourseBuilder::class.java)

        @JvmStatic
        fun initTask(task: Task) {
            val taskFile = TaskFile()
            taskFile.task = task
            taskFile.name = KtConfigurator.TASK_KT
            taskFile.text = EduUtils.getTextFromInternalTemplate(KtConfigurator.TASK_KT)
            task.addTaskFile(taskFile)
            task.testsText.put(KtConfigurator.TESTS_KT, EduUtils.getTextFromInternalTemplate(KtConfigurator.TESTS_KT))
        }
    }
}
