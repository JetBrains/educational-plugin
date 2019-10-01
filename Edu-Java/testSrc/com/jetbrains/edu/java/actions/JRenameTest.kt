package com.jetbrains.edu.java.actions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.actions.rename.RenameTestBase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

class JRenameTest : RenameTestBase() {

  fun `test forbid public class renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
          """)
        }
      }
    }
    myFixture.openFileInEditor(findFile("lesson1/task1/Task1.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Task1"), "Task2")
    val task = course.findTask("lesson1", "task1")
    assertNotNull(task.getTaskFile("Task1.java"))
    assertNull(task.getTaskFile("Task2.java"))
    myFixture.checkResult("""
        public class Task1 {}
    """.trimIndent())
  }

  fun `test rename inner class in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
              class Foo {}
          """)
        }
      }
    }
    myFixture.openFileInEditor(findFile("lesson1/task1/Task1.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameActionWithInput(course, javaFile.findClass("Foo"), "Bar", shouldBeShown = false)
    myFixture.checkResult("""
        public class Task1 {}
        class Bar {}
    """.trimIndent())
  }

  fun `test rename student created public class in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
          """)
        }
      }
    }
    val taskDirectory = findFile("lesson1/task1")
    withVirtualFileListener(course) {
      GeneratorUtils.createChildFile(taskDirectory, "Foo.java", """
      public class Foo {}
    """.trimIndent())
    }

    myFixture.openFileInEditor(findFile("lesson1/task1/Foo.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Foo"), "Bar", shouldBeShown = false)

    val task = course.findTask("lesson1", "task1")
    assertNotNull(task.getTaskFile("Bar.java"))
    assertNull(task.getTaskFile("Foo.java"))
    myFixture.checkResult("""
      public class Bar {}
    """.trimIndent())
  }

  fun `test rename public class in CC mode`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
          """)
        }
      }
    }
    myFixture.openFileInEditor(findFile("lesson1/task1/Task1.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Task1"), "Task2", shouldBeShown = false)
    val task = course.findTask("lesson1", "task1")
    assertNotNull(task.getTaskFile("Task2.java"))
    assertNull(task.getTaskFile("Task1.java"))
    myFixture.checkResult("""
        public class Task2 {}
    """.trimIndent())
  }

  private fun PsiJavaFile.findClass(name: String): PsiClass = classes.find { it.name == name }!!
}
