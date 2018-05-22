package com.jetbrains.edu.java.taskDescription

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionPsiLinksTestBase

class JTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = JavaFileType.INSTANCE

  fun `test navigate to class`() = doTest("Bar", """
    public class <caret>Bar {
      public void bar() {}
    }
  """) {
    java("Foo.java", """
      public class Foo {
        public void foo() {}
      }
    """)
    java("Bar.java", """
      public class Bar {
        public void bar() {}
      }
    """)
  }

  fun `test navigate to method`() = doTest("Foo#foo", """
    public class Foo {
      public void <caret>foo() {}
    }
  """) {
    java("Foo.java", """
      public class Foo {
        public void foo() {}
      }
    """)
    java("Bar.java", """
      public class Bar {
        public void bar() {}
      }
    """)
  }

  fun `test navigate to inner class`() = doTest("Foo.Baz", """
    public class Foo {
      public void foo() {}

      public static class <caret>Baz {}
    }
  """) {
    java("Foo.java", """
      public class Foo {
        public void foo() {}

        public static class Baz {}
      }
    """)
    java("Bar.java", """
      public class Bar {
        public void bar() {}
      }
    """)
  }
}
