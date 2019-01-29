package com.jetbrains.edu.javascript

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase


class JsTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  override val language: Language = JavascriptLanguage.INSTANCE
  override val settings = JsNewProjectSettings()

  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      function sum(a, b) {
        return a + b;
      }
    ```

    Code block with specific language:
    ```javascript
       function sum(a, b) {
          return a + b;
       }
    ```

    Inline code `const a = "hello"`
  """, """
  <html>
   <head></head>
   <body md-src-pos="0..222">
    <p md-src-pos="0..33">Code block with default language:</p>
    <span class="code-block"><pre>  <span style="...">function </span>sum(a, b) {
      <span style="...">return </span>a + b;
    }
  </pre> </span>
    <p md-src-pos="88..122">Code block with specific language:</p>
    <span class="code-block"><pre>   <span style="...">function </span>sum(a, b) {
        <span style="...">return </span>a + b;
     }
  </pre> </span>
    <p md-src-pos="191..222">Inline code <span class="code"><span style="...">const </span>a = <span style="...">"hello"</span></span></p>
   </body>
  </html>
  """)

  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      function sum(a, b) {
        return a + b;
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-JavaScript">
      function sum(a, b) {
          return a + b;
       }
    </code></pre>
    <p>Inline code <code>const a = "hello"</code></p>
    </html>
  """, """
  <html>
   <head></head>
   <body>
    <p>Code block with default language:</p>
    <span class="code-block"><pre>
    <span style="...">function </span>sum(a, b) {
      <span style="...">return </span>a + b;
    }
  </pre> </span>
    <p>Code block with specific language:</p>
    <span class="code-block"><pre>
    <span style="...">function </span>sum(a, b) {
        <span style="...">return </span>a + b;
     }
  </pre> </span>
    <p>Inline code <span class="code"><span style="...">const </span>a = <span style="...">"hello"</span></span></p>
   </body>
  </html>
  """)
}
