package com.jetbrains.edu.kotlin.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = KotlinLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()

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
     <head></head>
     <body md-src-pos="0..252">
      <p md-src-pos="0..33">Code block with default language:</p>
      <pre>  <span style="...">fun </span>main(args: Array&lt;String&gt;) {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p md-src-pos="103..137">Code block with specific language:</p>
      <pre>  <span style="...">fun </span>main(args: Array&lt;String&gt;) {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p md-src-pos="213..252">Inline code <span style="...">if </span>(condition) {} <span style="...">else </span>{}</p>
     </body>
    </html>
  """)

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
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <pre>
      <span style="...">fun </span>main(args: Array&lt;String&gt;) {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p>Code block with specific language:</p>
      <pre>
      <span style="...">fun </span>main(args: Array&lt;String&gt;) {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p>Inline code <span style="...">if </span>(condition) {} <span style="...">else </span>{}</p>
     </body>
    </html>
  """)
}
