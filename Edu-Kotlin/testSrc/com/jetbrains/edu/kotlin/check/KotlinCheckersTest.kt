package com.jetbrains.edu.kotlin.check

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.kotlin.KtProjectGenerator
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.kotlin.check.CheckActionListener.expectedMessage
import com.jetbrains.edu.kotlin.check.CheckActionListener.shouldFail
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.checker.TestsOutputParser.CONGRATULATIONS
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class KotlinCheckersTest : CheckersTestBase() {

    fun testKotlinCourse() {
        expectedMessage { task ->
            when(task) {
                is OutputTask, is EduTask -> CONGRATULATIONS
                is TheoryTask -> ""
                else -> null
            }
        }
        doTest()
    }

    fun testErrors() {
        shouldFail()
        expectedMessage { task ->
            when(task.name) {
                "kotlinCompilationError", "javaCompilationError" -> COMPILATION_FAILED_MESSAGE
                "testFail" -> "foo() should return 42"
                else -> null
            }
        }
        doTest()
    }

    fun testBrokenJdk() {
        UIUtil.dispatchAllInvocationEvents()

        val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), myProject.baseDir, JavaSdk.getInstance(), true, null, "Broken JDK")
        ApplicationManager.getApplication().runWriteAction {
            ProjectRootManager.getInstance(myProject).projectSdk = jdk
            ProjectJdkTable.getInstance().addJdk(jdk!!)
        }

        shouldFail()
        expectedMessage { "${CheckAction.FAILED_CHECK_LAUNCH}. See idea.log for more details." }

        doTest()
    }

    override fun getGenerator(course: Course) = KtProjectGenerator(course)
}
