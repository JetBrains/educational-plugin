package com.jetbrains.edu.java.taskDescription

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionPsiLinksTestBase
import org.junit.Test

class JTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = JavaFileType.INSTANCE

  @Test
  fun `test navigate to class`() = doTest("psi_element://Bar", """
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

  @Test
  fun `test navigate to method`() = doTest("psi_element://Foo#foo", """
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

  @Test
  fun `test navigate to method with encoded url`() = doTest("psi_element://Foo%23foo%28int%2C%20int%29", """
      public class Foo {
        public void foo() {}
        public void <caret>foo(int bar, int baz) {}
      }
  """) {
    java("Foo.java", """
      public class Foo {
        public void foo() {}
        public void foo(int bar, int baz) {}
      }
    """)
    java("Bar.java", """
      public class Bar {
        public void bar() {}
      }
    """)
  }

  @Test
  fun `test navigate to inner class`() = doTest("psi_element://Foo.Baz", """
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
