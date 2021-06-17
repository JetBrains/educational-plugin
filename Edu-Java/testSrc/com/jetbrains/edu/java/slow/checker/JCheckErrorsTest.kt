package com.jetbrains.edu.java.slow.checker

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.edu.learning.xmlEscaped
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat

class JCheckErrorsTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = JavaLanguage.INSTANCE) {
    lesson {
      eduTask("javaCompilationError") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static final String STRING;
          }
        """)
        javaTaskFile("test/Test.java", """
            class Test {}
        """)
      }
      eduTask("testFail") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static int foo() {
              return 0;
            }
          }
        """)
        javaTaskFile("test/Test.java", """
          import org.junit.Assert;

          public class Test {
            @org.junit.Test
            public void test() {
              Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
            }
          }
        """)
      }
      eduTask("comparisonTestFail") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static int foo() {
              return 0;
            }
          }
        """)
        javaTaskFile("test/Test.java", """
          import org.junit.Assert;

          public class Test {
            @org.junit.Test
            public void test() {
              Assert.assertEquals(42, Task.foo());
            }
          }
        """)
      }
      eduTask("escapeMessageInFailedTest") {
        javaTaskFile("src/Task.java")
        javaTaskFile("test/Test.java", """
          import org.junit.Assert;

          public class Test {
            @org.junit.Test
            public void test() {
              Assert.assertTrue("<br>", false);
            }
          }
        """)
      }
    }
  }

  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "javaCompilationError" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "testFail" -> equalTo("Task.foo() should return 42") to nullValue()
        "comparisonTestFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "42", actual = "0", title = "Comparison Failure (test)"))
        "escapeMessageInFailedTest" ->
          equalTo("<br>".xmlEscaped) to nullValue()
        else -> error("Unexpected task `${task.name}`")
      }
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }

  fun `test broken jdk`() {
    UIUtil.dispatchAllInvocationEvents()

    @Suppress("DEPRECATION")
    val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), myProject.baseDir, JavaSdk.getInstance(), true, null, "Broken JDK")!!
    runWriteAction {
      ProjectRootManager.getInstance(myProject).projectSdk = jdk
      ProjectJdkTable.getInstance().addJdk(jdk)
    }

    CheckActionListener.shouldSkip()
    CheckActionListener.setCheckResultVerifier { _, checkResult ->
      assertThat(checkResult.message, containsString(EduCoreBundle.message("error.failed.to.launch.checking")))
    }

    try {
      doTest()
    }
    finally {
      SdkConfigurationUtil.removeSdk(jdk)
    }
  }
}
