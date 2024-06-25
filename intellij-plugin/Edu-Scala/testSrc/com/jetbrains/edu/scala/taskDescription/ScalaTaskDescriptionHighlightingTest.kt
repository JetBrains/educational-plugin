package com.jetbrains.edu.scala.taskDescription

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Test

class ScalaTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = ScalaLanguage.INSTANCE
  override val environment: String = "Gradle"

  @Test
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
  """, if (ApplicationInfo.getInstance().build < BUILD_242) """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">(condition) {} </span><span style="...">else </span><span style="...">{}</span></span></p>
      </div>
     </body>
    </html>
  """ else """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main</span><span style="...">(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main</span><span style="...">(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
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
  """, if (ApplicationInfo.getInstance().build < BUILD_242) """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">(condition) {} </span><span style="...">else </span><span style="...">{}</span></span></p>
      </div>
     </body>
    </html>
  """ else """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main</span><span style="...">(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">def </span><span style="...">main</span><span style="...">(args: Array[String]): Unit = {</span>
        <span style="...">println(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">(condition) {} </span><span style="...">else </span><span style="...">{}</span></span></p>
      </div>
     </body>
    </html>
  """)

  companion object {
    // BACKCOMPAT: 2024.1. Drop it
    private val BUILD_242 = BuildNumber.fromString("242")!!
  }
}
