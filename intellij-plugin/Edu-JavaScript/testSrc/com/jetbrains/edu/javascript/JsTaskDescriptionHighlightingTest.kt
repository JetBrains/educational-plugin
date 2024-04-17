package com.jetbrains.edu.javascript

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.junit.Test


class JsTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  override val language: Language = JavascriptLanguage.INSTANCE

  @Test
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
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block"><pre>  <span style="...">function </span><span style="...">sum(a, b) {</span>
        <span style="...">return </span><span style="...">a + b;</span>
      <span style="...">}</span>
    </pre> </span>
       <p>Code block with specific language:</p><span class="code-block"><pre>   <span style="...">function </span><span style="...">sum(a, b) {</span>
          <span style="...">return </span><span style="...">a + b;</span>
       <span style="...">}</span>
    </pre> </span>
       <p>Inline code <span class="code"><span style="...">const </span><span style="...">a = </span><span style="...">"hello"</span></span></p>
      </div>
     </body>
    </html>
  """)

  @Test
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
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p> <span class="code-block"><pre>  <span style="...">function </span><span style="...">sum(a, b) {</span>
        <span style="...">return </span><span style="...">a + b;</span>
      <span style="...">}</span>
    </pre> </span>
       <p>Code block with specific language:</p> <span class="code-block"><pre>  <span style="...">function </span><span style="...">sum(a, b) {</span>
          <span style="...">return </span><span style="...">a + b;</span>
       <span style="...">}</span>
    </pre> </span>
       <p>Inline code <span class="code"><span style="...">const </span><span style="...">a = </span><span style="...">"hello"</span></span></p>
      </div>
     </body>
    </html>
  """)
}
