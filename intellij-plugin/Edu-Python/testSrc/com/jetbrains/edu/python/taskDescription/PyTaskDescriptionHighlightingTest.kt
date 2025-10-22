package com.jetbrains.edu.python.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = PythonLanguage.INSTANCE

  @Test
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
     <head>...</head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p>
       <span class="code-block">
        <pre>  <span style="...">def </span><span style="...">foo():</span>
          <span style="...">print </span><span style="...">"Hello!"</span>
    </pre>
       </span>
       <p>Code block with specific language:</p>
       <span class="code-block">
        <pre>  <span style="...">def </span><span style="...">foo():</span>
          <span style="...">print </span><span style="...">"Hello!"</span>
    </pre>
       </span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">__name__ == </span><span style="...">"__main__"</span></span></p>
      </div>
     </body>
    </html>
  """)

  @Test
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
     <head>...</head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p>
       <span class="code-block">
        <pre>  <span style="...">def </span><span style="...">foo():</span>
          <span style="...">print </span><span style="...">"Hello!"</span>
    </pre>
       </span>
       <p>Code block with specific language:</p>
       <span class="code-block">
        <pre>  <span style="...">def </span><span style="...">foo():</span>
          <span style="...">print </span><span style="...">"Hello!"</span>
    </pre>
       </span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">__name__ == </span><span style="...">"__main__"</span></span></p>
      </div>
     </body>
    </html>
  """)
}
