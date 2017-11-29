package com.jetbrains.edu.kotlin.check

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.kotlin.KtProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.test.CheckActionListener

class KotlinCheckersTest : CheckersTestBase() {

    fun testKotlinCourse() {
        doTest()
    }

    fun testErrors() {
        CheckActionListener.afterCheck = CheckActionListener.SHOULD_FAIL
        doTest()
    }

    fun testBrokenJdk() {
        UIUtil.dispatchAllInvocationEvents()

        val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), myProject.baseDir, JavaSdk.getInstance(), true, null, "Broken JDK")
        ApplicationManager.getApplication().runWriteAction {
            ProjectRootManager.getInstance(myProject).projectSdk = jdk
            ProjectJdkTable.getInstance().addJdk(jdk!!)
        }

        CheckActionListener.afterCheck = CheckActionListener.SHOULD_FAIL

        doTest()
    }

    override fun getGenerator(course: Course) = KtProjectGenerator(course)
}
