package com.jetbrains.edu.python.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.newProject.PythonProjectGenerator

class PyTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = PythonLanguage.INSTANCE
  override val settings: Any get() = PythonProjectGenerator.NO_SETTINGS

  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      def foo():
          print "Hello!"
    ```

    Code block with specific language:
    ```python
      def foo():
          print "Hello!"
    ```

    Inline code `if __name__ == "__main__"`
  """, """
    <html>
     <head></head>
     <body md-src-pos="0..200">
      <p md-src-pos="0..33">Code block with default language:</p>
      <span class="code-block"><pre>  <span style="...">def </span>foo():
          print <span style="...">"Hello!"</span>
    </pre> </span>
      <p md-src-pos="77..111">Code block with specific language:</p>
      <span class="code-block"><pre>  <span style="...">def </span>foo():
          print <span style="...">"Hello!"</span>
    </pre> </span>
      <p md-src-pos="161..200">Inline code <span class="code"><span style="...">if </span>__name__ == <span style="...">"__main__"</span></span></p>
     </body>
    </html>
  """)

  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      def foo():
          print "Hello!"
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="python">
      def foo():
          print "Hello!"
    </code></pre>
    <p>Inline code <code>if __name__ == "__main__"</code></p>
    </html>
  """, """
    <html>
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <span class="code-block"><pre>
      <span style="...">def </span>foo():
          print <span style="...">"Hello!"</span>
    </pre> </span>
      <p>Code block with specific language:</p>
      <span class="code-block"><pre>
      <span style="...">def </span>foo():
          print <span style="...">"Hello!"</span>
    </pre> </span>
      <p>Inline code <span class="code"><span style="...">if </span>__name__ == <span style="...">"__main__"</span></span></p>
     </body>
    </html>
  """)
}
