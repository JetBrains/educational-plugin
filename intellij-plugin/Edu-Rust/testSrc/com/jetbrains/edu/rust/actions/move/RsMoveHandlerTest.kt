package com.jetbrains.edu.rust.actions.move

import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.actions.move.MoveHandlerTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.junit.Test
import org.rust.lang.RsLanguage

class RsMoveHandlerTest : MoveHandlerTestBase(RsLanguage) {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/src/bar.rs") }
    doTest(findTarget) {
      rustTaskFile("src/main.rs", """
        fn foo<caret>() {}
      """)
      rustTaskFile("src/bar.rs")
    }
  }

  @Test
  fun `test do not forbid move refactoring for structs`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/src/bar.rs") }
    doTest(findTarget) {
      rustTaskFile("src/main.rs", """
        struct S<caret>;
      """)
      rustTaskFile("src/bar.rs")
    }
  }
}
