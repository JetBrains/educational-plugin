package com.jetbrains.edu.cpp.actions.move

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.learning.actions.move.MoveHandlerTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.junit.Test

class CppMoveHandlerTest : MoveHandlerTestBase(OCLanguage.getInstance(), environment = "Catch") {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.cpp") }
    doTest(findTarget) {
      cppTaskFile("foo.cpp", """
        void foo<caret>() {}
      """)
      cppTaskFile("bar.cpp")
    }
  }

  @Test
  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.cpp") }
    doTest(findTarget) {
      cppTaskFile("foo.cpp", """
        class Foo<caret> {};
      """)
      cppTaskFile("bar.cpp")
    }
  }
}
