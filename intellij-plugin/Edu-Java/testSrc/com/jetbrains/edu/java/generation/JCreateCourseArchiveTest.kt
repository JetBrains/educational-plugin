package com.jetbrains.edu.java.generation

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.pom.java.LanguageLevel
import com.jetbrains.edu.coursecreator.archive.CourseArchiveTestBase
import com.jetbrains.edu.jvm.JVM_LANGUAGE_LEVEL
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

@Suppress("NonFinalUtilityClass")
class JCreateCourseArchiveTest : CourseArchiveTestBase() {

  /** Checks [com.jetbrains.edu.jvm.gradle.generation.macro.GradleCommandMacroProvider] */
  @Test
  fun `test custom command`() {
    val course = courseWithFiles(
      language = JavaLanguage.INSTANCE,
      courseMode = CourseMode.EDUCATOR,
      description = "my summary"
    ) {
      lesson("lesson1") {
        theoryTask("TheoryWithCustomRunConfiguration") {
          javaTaskFile("src/Main.java", """
            public class Main {
              public static void main(String[] args) {
                System.out.println(System.getenv("EXAMPLE_ENV"));
              }
            }
          """)
          // Need to verify that the plugin doesn't touch non-related run configuration files
          xmlTaskFile("CustomGradleRun.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomCustomGradleRun" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="-PmainClass=Main" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":lesson1-TheoryWithCustomRunConfiguration:run" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <method v="2" />
              </configuration>
            </component>            
          """)
          dir("runConfigurations") {
            xmlTaskFile("CustomGradleRun.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleRun" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="-PmainClass=Main" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":lesson1-TheoryWithCustomRunConfiguration:run" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
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

    withLanguageLevel(LanguageLevel.JDK_17) {
      doTest(course)
    }
  }

  @Test
  fun `test do not override existing jdk level`() {
    val course = courseWithFiles(
      language = JavaLanguage.INSTANCE,
      courseMode = CourseMode.EDUCATOR
    ) {
      lesson("lesson1") {
        theoryTask("task1") {
          javaTaskFile("src/Main.java", """
            public class Main {
              public static void main(String[] args) {
                System.out.println("Hello, World!");
              }
            }
          """)
        }
      }
    }

    course.environmentSettings = mapOf(JVM_LANGUAGE_LEVEL to LanguageLevel.JDK_17.toString())

    withLanguageLevel(LanguageLevel.JDK_19) {
      doTest(course)
    }
  }

  private fun withLanguageLevel(level: LanguageLevel, action: () -> Unit) {
    val languageLevelProjectExtension = LanguageLevelProjectExtension.getInstance(project)
    val initialLevel = languageLevelProjectExtension.languageLevel
    languageLevelProjectExtension.languageLevel = level
    try {
      action()
    }
    finally {
      languageLevelProjectExtension.languageLevel = initialLevel
    }
  }
}
