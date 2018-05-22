package com.jetbrains.edu.kotlin.taskDescription

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionPsiLinksTestBase
import org.jetbrains.kotlin.idea.KotlinFileType

class KtTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {
  override val fileType: FileType = KotlinFileType.INSTANCE

  fun `test navigate to class`() = doTest("Bar", """
    class <caret>Bar {
      fun bar() {}
    }
  """) {
    kotlin("Foo.kt", """
      class Foo {
        fun foo() {}
      }
    """)
    kotlin("Bar.kt", """
      class Bar {
        fun bar() {}
      }
    """)
  }

  fun `test navigate to method`() = doTest("Foo#foo", """
    class Foo {
      fun <caret>foo() {}
    }
  """) {
    kotlin("Foo.kt", """
      class Foo {
        fun foo() {}
      }
    """)
    kotlin("Bar.kt", """
      class Bar {
        fun bar() {}
      }
    """)
  }

  fun `test navigate to inner class`() = doTest("Foo.FooBar", """
    class Foo {
      fun foo() {}

      data class <caret>FooBar(val x: Int)
    }
  """) {
    kotlin("Foo.kt", """
      class Foo {
        fun foo() {}

        data class FooBar(val x: Int)
      }
    """)
    kotlin("Bar.kt", """
      class Bar {
        fun bar() {}
      }
    """)
  }

  fun `test navigate to property`() = doTest("Foo#property", """
      class Foo {
        val <caret>property: Int = 1
        fun foo() {}
      }
  """) {
    kotlin("Foo.kt", """
      class Foo {
        val property: Int = 1
        fun foo() {}
      }
    """)
    kotlin("Bar.kt", """
      class Bar {
        fun bar() {}
      }
    """)
  }
}
