package com.jetbrains.edu.java.taskDescription

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.junit.Test

class JTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = JavaLanguage.INSTANCE

  @Test
  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    ```

    Code block with specific language:
    ```java
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    ```

    Inline code `if (condition) {} else {}`
  """, """
    <html>
     <head>...</head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p>
       <span class="code-block">
        <pre>  <span style="...">class </span><span style="...">Main {</span>
          <span style="...">public static void </span><span style="...">main(String[] args) {</span>
            <span style="...">System.out.println(</span><span style="...">"Hello!"</span><span style="...">);</span>
          <span style="...">}</span>
      <span style="...">}</span>
    </pre>
       </span>
       <p>Code block with specific language:</p>
       <span class="code-block">
        <pre>  <span style="...">class </span><span style="...">Main {</span>
          <span style="...">public static void </span><span style="...">main(String[] args) {</span>
            <span style="...">System.out.println(</span><span style="...">"Hello!"</span><span style="...">);</span>
          <span style="...">}</span>
      <span style="...">}</span>
    </pre>
       </span>
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
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-java">
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </code></pre>
    <p>Inline code <code>if (condition) {} else {}</code></p>
    </html>
  """, """
    <html>
     <head>...</head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p>
       <span class="code-block">
        <pre>  <span style="...">class </span><span style="...">Main {</span>
          <span style="...">public static void </span><span style="...">main(String[] args) {</span>
            <span style="...">System.out.println(</span><span style="...">"Hello!"</span><span style="...">);</span>
          <span style="...">}</span>
      <span style="...">}</span>
    </pre>
       </span>
       <p>Code block with specific language:</p>
       <span class="code-block">
        <pre>  <span style="...">class </span><span style="...">Main {</span>
          <span style="...">public static void </span><span style="...">main(String[] args) {</span>
            <span style="...">System.out.println(</span><span style="...">"Hello!"</span><span style="...">);</span>
          <span style="...">}</span>
      <span style="...">}</span>
    </pre>
       </span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">(condition) {} </span><span style="...">else </span><span style="...">{}</span></span></p>
      </div>
     </body>
    </html>
  """)

  @Test
  fun `test html description no highlighting class`() = doHtmlTest("""
    <html>
    <pre><code class="no-highlight">
      class Main {
          public static void main(String[] args) {
            System.out.println("Hello!");
          }
      }
    </code></pre>
    </html>
  """, """
    <html>
     <head>...</head>
     <body>
      <div class="wrapper">
       <span class="code-block">
        <pre>  <span style="...">class Main {</span>
          <span style="...">public static void main(String[] args) {</span>
            <span style="...">System.out.println("Hello!");</span>
          <span style="...">}</span>
      <span style="...">}</span>
    </pre>
       </span>
      </div>
     </body>
    </html>
  """)
}
