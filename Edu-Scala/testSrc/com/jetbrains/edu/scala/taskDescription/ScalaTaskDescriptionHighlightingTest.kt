package com.jetbrains.edu.scala.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = ScalaLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()

  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    ```

    Code block with specific language:
    ```scala
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    ```

    Inline code `if (condition) {} else {}`
  """, """
    <html>
     <head></head>
     <body md-src-pos="0..267">
      <p md-src-pos="0..33">Code block with default language:</p>
      <pre>  <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p md-src-pos="111..145">Code block with specific language:</p>
      <pre>  <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p md-src-pos="228..267">Inline code <span style="...">if </span>(condition) {} <span style="...">else </span>{}</p>
     </body>
    </html>
  """)

  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      def main(args: Array[String]): Unit = {
        println("Hello!")
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-kotlin">
      def main(args: Array[String]): Unit = {
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
      <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p>Code block with specific language:</p>
      <pre>
      <span style="...">def </span>main(args: Array[String]): Unit = {
        println(<span style="...">"Hello!"</span>)
      }
    </pre>
      <p>Inline code <span style="...">if </span>(condition) {} <span style="...">else </span>{}</p>
     </body>
    </html>
  """)
}
