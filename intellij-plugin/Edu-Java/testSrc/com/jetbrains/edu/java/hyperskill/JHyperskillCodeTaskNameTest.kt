package com.jetbrains.edu.java.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import org.junit.Test
import java.io.File

class JHyperskillCodeTaskNameTest : EduTestCase() {
  @Test
  fun `test find taskFile for uploading`() {
    val course = courseWithFiles(
      language = JavaLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.java", """
            public class Task {
            }
          """.trimIndent())
          taskFile("src/CoolTaskName.java", """
            public class CoolTaskName {
              public static void main(String[] args) {
              }
            }
          """.trimIndent())
          taskFile("test/Tests1.java")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val task = findTask(0, 0)
    val configurator = course.configurator as HyperskillConfigurator
    val codeTaskFile = configurator.getCodeTaskFile(project, task)

    assertEquals("src/CoolTaskName.java", codeTaskFile!!.name)
  }

  @Test
  fun `test create name for taskfile`() {
    val course = courseWithFiles(
      language = JavaLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse
    ) {} as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val fileName = course.configurator?.getMockFileName(course, """
      public class CoolTaskName {
        public static void main(String[] args) {
        }
      }
      """.trimIndent())

    assertEquals("CoolTaskName.java", fileName)
  }

  override fun getProjectDescriptor(): LightProjectDescriptor {
    return object : LightProjectDescriptor() {
      override fun getSdk(): Sdk? {
        val myJdkHome = IdeaTestUtil.requireRealJdkHome()
        VfsRootAccess.allowRootAccess(testRootDisposable, myJdkHome)
        val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(myJdkHome))!!
        return SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, "Test JDK")
      }
    }
  }
}
