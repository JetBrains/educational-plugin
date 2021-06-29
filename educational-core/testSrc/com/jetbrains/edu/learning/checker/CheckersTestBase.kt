package com.jetbrains.edu.learning.checker

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.*
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.codeforces.AnsiAwareCapturingProcessAdapter
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationProducer
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.withTestDialog
import org.junit.Assert

abstract class CheckersTestBase<Settings> : HeavyPlatformTestCase() {
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

    protected fun doCodeforcesTest(expectedOutput: String) {
        val course = project.course ?: error("Course was not found")
        val task = course.getLesson(0)?.getTask(0) as? CodeforcesTask ?: error("Codeforces task was not found")
        val inputTaskFile = task.taskFiles["${CodeforcesNames.TEST_DATA_FOLDER}/1/${task.inputFileName}"]
                            ?: error("Unable to find input file")
        val virtualFile = inputTaskFile.getVirtualFile(project) ?: error("Unable to find virtual file for input file")
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: error("Unable to find PSI for input file")

        val configuration = CodeforcesRunConfigurationProducer().createConfigurationFromContext(ConfigurationContext(psiFile))
                            ?: error("Could not create run configuration")
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, configuration.configuration).build()
        val result = environment.state!!.execute(executor, environment.runner)!!

        val adapter = AnsiAwareCapturingProcessAdapter()
        with(result.processHandler) {
            addProcessListener(adapter)
            startNotify()
            waitFor()
        }
        Disposer.dispose(result.executionConsole)
        assertTrue(expectedOutput in adapter.output.stdout)
    }

    private fun refreshProject() {
        myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    protected open fun checkTask(task: Task): List<AssertionError> {
        val errorProcessor = CollectingLoggedErrorProcessor()
        LoggedErrorProcessor.setNewInstance(errorProcessor)

        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        try {
            // In case of framework lessons, we can't just run checker for the given task
            // because code on file system may be not updated yet.
            // Launch `NextTaskAction` to make all necessary updates on file system in similar was as users do it
            val frameworkLesson = task.lesson as? FrameworkLesson
            val currentFrameworkLessonTask = frameworkLesson?.currentTask()
            if (frameworkLesson != null && currentFrameworkLessonTask != null && currentFrameworkLessonTask != task) {
                val file = currentFrameworkLessonTask.openFirstTaskFileInEditor()
                launchAction(file, NextTaskAction())
                assertEquals(task, frameworkLesson.currentTask())
            }

            val virtualFile = task.openFirstTaskFileInEditor()
            launchAction(virtualFile, CheckAction())
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        }
        finally {
            LoggedErrorProcessor.restoreDefaultProcessor()
        }
        return errorProcessor.exceptions
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
        val e = getActionEvent(virtualFile, action)
        action.beforeActionPerformedUpdate(e)
        Assert.assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
        action.actionPerformed(e)
    }

    private fun getActionEvent(virtualFile: VirtualFile, action: AnAction): TestActionEvent {
        val context = MapDataContext()
        context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
        context.put(CommonDataKeys.PROJECT, myProject)
        return TestActionEvent(context, action)
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
                myProject = generator.doCreateCourseProject(rootDir.path, settings as Any)
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
        } finally {
            super.tearDown()
        }
    }
}
