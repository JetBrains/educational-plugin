package com.jetbrains.edu.java.generation

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.actions.CourseArchiveTestBase
import com.jetbrains.edu.coursecreator.actions.EduCourseArchiveCreator
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.encrypt.getAesKey

class JCreateCourseArchiveTest : CourseArchiveTestBase() {

  override fun getTestDataPath(): String = super.getTestDataPath() + "/actions/createCourseArchive"

  /** Checks [com.jetbrains.edu.jvm.gradle.generation.macro.GradleCommandMacroProvider] */
  fun `test custom command`() {
    courseWithFiles(
      language = JavaLanguage.INSTANCE,
      settings = JdkProjectSettings.emptySettings(),
      courseMode = CCUtils.COURSE_MODE,
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
          xmlTaskFile("CustomGradleRun.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomCustomGradleRun" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
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
            xmlTaskFile("CustomGradleRun.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleRun" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
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
    doTest()
  }

  override fun getArchiveCreator(): CourseArchiveCreator = EduCourseArchiveCreator(
    myFixture.project,
    "${myFixture.project.basePath}/${CCUtils.GENERATED_FILES_FOLDER}/course.zip",
    getAesKey()
  )
}
