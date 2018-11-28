package com.jetbrains.edu.java.taskDescription

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase

class JTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {

  override val language: Language = JavaLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()

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
     <head></head>
     <body md-src-pos="0..354">
      <p md-src-pos="0..33">Code block with default language:</p>
      <pre>  <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre>
      <p md-src-pos="155..189">Code block with specific language:</p>
      <pre>  <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre>
      <p md-src-pos="315..354">Inline code <span style="...">if </span>(condition) {} <span style="...">else </span>{}</p>
     </body>
    </html>
  """)

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
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <pre>
      <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre>
      <p>Code block with specific language:</p>
      <pre>
      <span style="...">class </span>Main {
          <span style="...">public static void </span>main(String[] args) {
            System.out.println(<span style="...">"Hello!"</span>);
          }
      }
    </pre>
      <p>Inline code <span style="...">if </span>(condition) {} <span style="...">else </span>{}</p>
     </body>
    </html>
  """)
}
