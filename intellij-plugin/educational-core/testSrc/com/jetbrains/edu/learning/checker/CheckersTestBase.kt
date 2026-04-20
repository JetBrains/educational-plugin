package com.jetbrains.edu.learning.checker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.ui.getUICheckLabel
import org.junit.ComparisonFailure

abstract class CheckersTestBase<Settings : EduProjectSettings> : EduHeavyTestCase() {
    protected lateinit var myCourse: Course

    private val checkerFixture: EduCheckerFixture<Settings> by lazy {
        createCheckerFixture()
    }

    override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
        val skipTestReason = checkerFixture.getSkipTestReason()
        if (skipTestReason != null) {
            System.err.println("SKIP `$name`: $skipTestReason")
        }
        else {
            super.runTestRunnable(context)
        }
    }

    protected open fun doTest() {
        refreshProject()
        UIUtil.dispatchAllInvocationEvents()

        val exceptions = arrayListOf<AssertionError>()
        myCourse.visitLessons { lesson ->
            for (task in lesson.taskList) {
                exceptions += checkTask(task)
            }
        }

        if (exceptions.isNotEmpty()) {
            throw MultipleCauseException(exceptions)
        }
    }

    private fun refreshProject() {
        myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    protected open fun checkTask(task: Task): List<AssertionError> {
        UIUtil.dispatchAllInvocationEvents()
        val exceptions = mutableListOf<AssertionError>()
        try {
            // In case of framework lessons, we can't just run checker for the given task
            // because code on file system may be not updated yet.
            // Launch `NextTaskAction` to make all necessary updates on file system in similar was as users do it
            val frameworkLesson = task.lesson as? FrameworkLesson
            val currentFrameworkLessonTask = frameworkLesson?.currentTask()
            if (frameworkLesson != null && currentFrameworkLessonTask != null && currentFrameworkLessonTask != task) {
                val file = currentFrameworkLessonTask.openFirstTaskFileInEditor()
                launchAction(file, getActionById(NextTaskAction.ACTION_ID))
                assertEquals(task, frameworkLesson.currentTask())
            }

            val virtualFile = task.openFirstTaskFileInEditor()
            launchAction(virtualFile, CheckAction(task.getUICheckLabel()))
            UIUtil.dispatchAllInvocationEvents()
        } catch (e: AssertionError) {
            exceptions.add(e)
        } catch (e: ComparisonFailure) {
            exceptions.add(e)
        } catch (e: IllegalStateException) {
            exceptions.add(AssertionError(e))
        }
        return exceptions
    }

    private fun Task.openFirstTaskFileInEditor(): VirtualFile {
        val taskFile = taskFiles.values.first()
        val virtualFile = taskFile.getVirtualFile(myProject)
                          ?: error("Can't find virtual file for `${taskFile.name}` task file in `$name task`")

        FileEditorManager.getInstance(myProject).openFile(virtualFile, true)
        return virtualFile
    }

    private class MultipleCauseException(val causes: List<AssertionError>) : Exception() {
        override fun printStackTrace() {
            for (cause in causes) {
                cause.printStackTrace()
            }
        }

        override val message: String
            get() = "\n" + causes.joinToString("\n") { it.message ?: "" }
    }

    protected abstract fun createCheckerFixture(): EduCheckerFixture<Settings>
    protected abstract fun createCourse(): Course

    private fun projectName() = getTestName(true)

    private fun launchAction(virtualFile: VirtualFile, action: AnAction) {
        val context = createDataEvent(virtualFile)
        testAction(action, context)
    }

    private fun createDataEvent(virtualFile: VirtualFile): DataContext {
        return SimpleDataContext.builder()
          .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
          .add(CommonDataKeys.PROJECT, myProject)
          .build()
    }

    override fun setUpProject() {
        checkerFixture.setUp()
        if (checkerFixture.getSkipTestReason() == null) {
            myCourse = createCourse()
            val settings = checkerFixture.projectSettings

            withTestDialog(TestDialog.NO) {
                val rootDir = tempDir.createVirtualDir()
                val generator = myCourse.configurator?.courseBuilder?.getCourseProjectGenerator(myCourse)
                                ?: error("Failed to get `CourseProjectGenerator`")
                myProject = generator.doCreateCourseProject(rootDir.path, settings)
                            ?: error("Cannot create project with name ${projectName()}")
            }
        }
    }

    override fun setUp() {
        super.setUp()

        if (myProject != null) {
            EduDocumentListener.setGlobalListener(myProject, testRootDisposable)
        }

        CheckActionListener.registerListener(testRootDisposable)
        CheckActionListener.reset()
    }

    override fun tearDown() {
        try {
            checkerFixture.tearDown()
        }
        catch (e: Throwable) {
            addSuppressedException(e)
        }
        finally {
            super.tearDown()
        }
    }
}
