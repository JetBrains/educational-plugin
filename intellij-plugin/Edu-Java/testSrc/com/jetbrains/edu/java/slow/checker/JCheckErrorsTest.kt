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
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus.FAILED
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.edu.learning.xmlEscaped
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

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
      eduTask("gradleCustomRunConfiguration") {
        javaTaskFile("src/Task.java", """
          public class Task {
              public static String foo() {
                  return System.getenv("EXAMPLE_ENV");
              }
          }
        """)
        javaTaskFile("test/Tests.java", """
          import org.junit.Assert;
          import org.junit.Test;

          public class Tests {

            @Test
            public void fail() {
              Assert.fail();
            }

            @Test
            public void test() {
              Assert.assertEquals("Hello", Task.foo());
            }
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleCheck.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleCheck" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":${'$'}TASK_GRADLE_PROJECT${'$'}:test" />
                      <option value="--tests " />
                      <option value="&quot;Tests.test&quot;" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>false</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <method v="2" />
              </configuration>
            </component>
          """)
        }
      }
    }
  }

  @Test
  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val testComparisonData = when (task.name) {
        "javaCompilationError" -> TestComparisonData(equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE), nullValue())
        "testFail" -> TestComparisonData(
          equalTo("Task.foo() should return 42"), nullValue(), listOf(EduTestInfo("Test class Test:test", FAILED))
        )

        "comparisonTestFail" ->
          TestComparisonData(
            equalTo(EduCoreBundle.message("check.incorrect")),
            diff(CheckResultDiff(expected = "42", actual = "0", title = "Comparison Failure (test)")),
            listOf(EduTestInfo("Test class Test:test", FAILED))
          )

        "escapeMessageInFailedTest" ->
          TestComparisonData(equalTo("<br>".xmlEscaped), nullValue(), listOf(EduTestInfo("Test class Test:test", FAILED)))

        "gradleCustomRunConfiguration" ->
          TestComparisonData(
            equalTo(EduCoreBundle.message("check.incorrect")),
            diff(CheckResultDiff(expected = "Hello", actual = "Hello!", title = "Comparison Failure (test)")),
            listOf(EduTestInfo("Test class Tests:test", FAILED))
          )

        else -> error("Unexpected task `${task.name}`")
      }
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, testComparisonData.messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, testComparisonData.diffMatcher)
      assertEquals(
        "Number of executed tests for ${task.name} is wrong",
        testComparisonData.executedTestsInfo.size,
        checkResult.executedTestsInfo.size
      )
      testComparisonData.executedTestsInfo.forEach { testInfo ->
        val actualTestInfo = checkResult.executedTestsInfo.find { it.name == testInfo.name }
                             ?: error(
                               "Expected test ${testInfo.name} of ${task.name} task wasn't found " +
                               "in test results: ${checkResult.executedTestsInfo}"
                             )
        assertEquals(
          "Status of test from ${task.name} task is wrong",
          testInfo.toString(),
          actualTestInfo.toString()
        )
      }
    }
    doTest()
  }

  @Test
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
      assertThat(checkResult.message, containsString(EduFormatBundle.message("error.failed.to.launch.checking")))
    }

    try {
      doTest()
    }
    finally {
      SdkConfigurationUtil.removeSdk(jdk)
    }
  }
}
