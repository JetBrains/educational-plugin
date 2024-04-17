package com.jetbrains.edu.kotlin.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = KotlinLanguage.INSTANCE

  @Test
  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      fun main(args: Array<String>) {
        println("Hello!")
      }
    ```

    Code block with specific language:
    ```kotlin
      fun main(args: Array<String>) {
        println("Hello!")
      }
    ```

    Inline code `if (condition) {} else {}`
  """, """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block"><pre>  <span style="...">fun </span><span style="...">main(args: Array&lt;String&gt;) {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre> </span>
       <p>Code block with specific language:</p><span class="code-block"><pre>  <span style="...">fun </span><span style="...">main(args: Array&lt;String&gt;) {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre> </span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">(condition) {} </span><span style="...">else </span><span style="...">{}</span></span></p>
      </div>
     </body>
    </html>
  """)

  @Test
  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      fun main(args: Array&lt;String&gt;) {
        println("Hello!")
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-kotlin">
      fun main(args: Array&lt;String&gt;) {
        println("Hello!")
      }
    </code></pre>
    <p>Inline code <code>if (condition) {} else {}</code></p>
    </html>
  """, """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p> <span class="code-block"><pre>  <span style="...">fun </span><span style="...">main(args: Array&lt;String&gt;) {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre> </span>
       <p>Code block with specific language:</p> <span class="code-block"><pre>  <span style="...">fun </span><span style="...">main(args: Array&lt;String&gt;) {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre> </span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">(condition) {} </span><span style="...">else </span><span style="...">{}</span></span></p>
      </div>
     </body>
    </html>
  """)
}
