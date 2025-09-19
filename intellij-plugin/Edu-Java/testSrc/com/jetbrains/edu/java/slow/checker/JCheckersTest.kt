package com.jetbrains.edu.java.slow.checker

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.junit.Test

@Suppress("NonFinalUtilityClass", "NewClassNamingConvention")
class JCheckersTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = JavaLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static int foo() {
              return 42;
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
      eduTask("EduTaskWithIgnoredTest") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static int foo() {
              return 42;
            }
          }
        """)
        javaTaskFile("test/Tests.java", """
          import org.junit.Assert;
          import org.junit.Ignore;
          import org.junit.Test;
  
          public class Tests {
            @Test
            public void test() {
              Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
            }
            @Test
            @Ignore
            public void ignoredTest() {
              Assert.assertTrue("Task.foo() should return 42", Task.foo() == 43);
            }
          }
        """)
      }
      eduTask("EduTaskWithGradleCustomRunConfiguration") {
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
              Assert.assertEquals("Hello!", Task.foo());
            }
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleCheck.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleCheck" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":$TASK_GRADLE_PROJECT$:test" />
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
      outputTask("OutputTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println("OK");
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTaskWithWindowsLineSeparators") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println("OK");
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\r\n")
        }
      }
      outputTask("OutputTaskWithSeveralFiles") {
        javaTaskFile("src/Utils.java", """
          public class Utils {
            public static String ok() {
              return "OK";
            }
          }
        """)
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println(Utils.ok());
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("CheckEncoding") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.print('\u25A1');
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("□")
        }
      }
    }
  }

  @Test
  fun `test java course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
