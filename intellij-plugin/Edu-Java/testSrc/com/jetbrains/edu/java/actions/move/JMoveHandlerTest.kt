package com.jetbrains.edu.java.actions.move

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.actions.move.MoveHandlerTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.junit.Test

class JMoveHandlerTest : MoveHandlerTestBase(JavaLanguage.INSTANCE) {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiClassInFile("lesson1/task1/Bar.java") }
    doTest(findTarget) {
      javaTaskFile("Foo.java", """
        public class Foo {
          public void foo<caret>() {}
        }
      """)
      javaTaskFile("Bar.java", "public class Bar {}")
    }
  }

  @Test
  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.java") }
    doTest(findTarget) {
      javaTaskFile("Foo.java", """
        public class Foo {}
        private class FooInner<caret> {}
      """)
      javaTaskFile("Bar.java", "public class Bar {}")
    }
  }

  private fun findPsiClassInFile(path: String): PsiClass {
    val psiFile = findPsiFile(path)
    return PsiTreeUtil.findChildOfType(psiFile, PsiClass::class.java) ?: error("")
  }
}
