package com.jetbrains.edu.scala.actions.move

import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.actions.move.MoveHandlerTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaMoveHandlerTest : MoveHandlerTestBase(ScalaLanguage.INSTANCE, "sbt") {

  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.scala") }
    doTest(findTarget) {
      scalaTaskFile("Foo.scala", """
        def foo<caret>() {}
      """)
      scalaTaskFile("Bar.scala")
    }
  }

  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.scala") }
    doTest(findTarget) {
      scalaTaskFile("Foo.scala", """
        class Foo<caret> {}
      """)
      scalaTaskFile("Bar.scala")
    }
  }
}
