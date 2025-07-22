package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.gradle.GradleConstants
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

@Suppress("GrUnresolvedAccess")
class KtOneModuleCheckerTest : JdkCheckerTestBase() {
  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        kotlinTaskFile("src/Task.kt", """
          fun foo() = 42
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("EduTaskWithGradleCustomRunConfiguration") {
        kotlinTaskFile("src/CustomTask.kt", """
          fun bar(): String? = System.getenv("EXAMPLE_ENV")
        """)
        kotlinTaskFile("test/CustomTests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class CustomTests {
              @Test
              fun fail() {
                  Assert.fail()
              }

              @Test
              fun test() {
                  Assert.assertEquals("Hello!", bar())
              }
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleCheck.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomTests.test" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="${'$'}PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":test" />
                      <option value="--tests" />
                      <option value="&quot;CustomTests.test&quot;" />
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
      theoryTask("TheoryTask") {
        kotlinTaskFile("src/Task1.kt", """
          fun main(args: Array<String>) {
              val a = 1
              println(a)
          }
        """)
      }
      outputTask("OutputTask") {
        kotlinTaskFile("src/Task2.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
    }

    lesson("Unit Testing") {
      theoryTask {
        kotlinTaskFile("src/NoFramework.kt", """
          // UnitTesting/NoFramework.kt
          package unittesting
          import kotlin.test.assertEquals
          import kotlin.test.assertTrue
          
          fun fortyTwo() = 42
          
          fun testFortyTwo(n: Int = 42) {
            assertEquals(
              expected = n,
              actual = fortyTwo(),
              message = "Incorrect,")
          }
          
          fun main(args: Array<String>){
            testFortyTwo()
            }
    """)
      }
    }

    additionalFile(GradleConstants.SETTINGS_GRADLE, ONE_MODULE_SETTINGS_GRADLE)
    additionalFile(GradleConstants.BUILD_GRADLE, ONE_MODULE_BUILD_GRADLE)
  }


  @Test
  fun `test kotlin course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        is TheoryTask -> ""
        else -> null
      }
    }
    doTest()
  }

  companion object {
    @Language("Groovy")
    private const val ONE_MODULE_SETTINGS_GRADLE = """
      static String sanitizeName(String name) {
            return name.replaceAll("[ /\\\\:<>\"?*|]", "_")
      }
      rootProject.name = sanitizeName('AtomicKotlinCourse')
    """

    @Suppress("DifferentKotlinGradleVersion", "GroovyAssignabilityCheck", "GroovyUnusedAssignment")
    @Language("Groovy")
    private const val ONE_MODULE_BUILD_GRADLE = """
        buildscript {
            ext.kotlin_version = '1.6.20'
            repositories {
                mavenCentral()
            }
            dependencies {
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
            }
        }

        def printOutput(def output) {
            return tasks.create("printOutput") {
                for (line in output.toString().readLines()) {
                    println "#educational_plugin" + line
                }
            }
        }
  
        allprojects {
            apply plugin: 'java'
            apply plugin: 'kotlin'
            repositories {
                jcenter()
            }
            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${'$'}kotlin_version"

                // kotlin.test
                implementation "org.jetbrains.kotlin:kotlin-test"
                implementation "org.jetbrains.kotlin:kotlin-test-junit"
                implementation "junit:junit:4.12"
            }
            compileKotlin {
                kotlinOptions.jvmTarget = "1.8"
            }
            compileTestKotlin {
                kotlinOptions.jvmTarget = "1.8"
            }
        }
        apply plugin: 'application'
        sourceCompatibility = 1.8
        dependencies {
            testImplementation group: 'junit', name: 'junit', version: '4.12'
            testImplementation "org.jetbrains.kotlin:kotlin-test:${'$'}kotlin_version"
            testImplementation "org.jetbrains.kotlin:kotlin-test-junit:${'$'}kotlin_version"
        }
        def srcList = []
        def testList = []
        rootProject.projectDir.eachDirRecurse {
            if (!isTaskDir(it) || it.path.contains(".idea") || "util".equals(it.path)) {
                return
            }
            def srcDir = new File(it, "src")
            if (it.path.contains("Unit Testing")) {
                testList.add(srcDir)
            } else {
                srcList.add(srcDir)
            }
            def testDir = new File(it, "test")
            testList.add(testDir)
        }
        sourceSets {
            main {
                java {
                    srcDirs = srcList
                }
                kotlin {
                    srcDirs = srcList
                }
            }
            test {
                java {
                    srcDirs = testList
                }
                kotlin {
                    srcDirs = testList
                }
            }
        }

        static def isTaskDir(File dir) {
            return new File(dir, "src").exists()
        }

        mainClassName = project.hasProperty("mainClass") ? project.getProperty("mainClass") : ""
        test {
            outputs.upToDateWhen { false }
            afterTest { TestDescriptor test, TestResult result ->
                if (result.resultType == TestResult.ResultType.FAILURE) {
                    def message = result.exception?.message ?: "Wrong answer"
                    def lines = message.readLines()
                    println "#educational_plugin FAILED + " + lines[0]
                    if (lines.size() > 1) {
                        lines[1..-1].forEach { line ->
                            println "#educational_plugin" + line
                        }
                    }
                    // we need this to separate output of different tests
                    println ""
                }
            }
        }
        def runOutput = new ByteArrayOutputStream()
        tasks.run.setStandardOutput(runOutput)
        tasks.run.doLast { printOutput(runOutput) }
    """
  }
}