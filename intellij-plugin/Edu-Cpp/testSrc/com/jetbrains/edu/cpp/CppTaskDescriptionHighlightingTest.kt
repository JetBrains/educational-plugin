package com.jetbrains.edu.cpp

import com.intellij.lang.Language
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.junit.Test

class CppTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  override val language: Language = OCLanguage.getInstance()
  override val environment = "GoogleTest"

  @Test
  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      int main(void) {
          double x = 1e-6 + 5.0;
          cout << "Hello world!";
          return 0;
       }
    ```

    Code block with specific language:
    ```cpp
       int main(void) {
          double x = 1e-6 + 5.0;
          cout << "Hello world!";
          return 0;
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
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">int </span><span style="...">main(</span><span style="...">void</span><span style="...">) {</span>
          <span style="...">double </span><span style="...">x = </span><span style="...">1e-6 </span><span style="...">+ </span><span style="...">5.0</span><span style="...">;</span>
          <span style="...">cout &lt;&lt; </span><span style="...">"Hello world!"</span><span style="...">;</span>
          <span style="...">return </span><span style="...">0</span><span style="...">;</span>
       <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>   <span style="...">int </span><span style="...">main(</span><span style="...">void</span><span style="...">) {</span>
          <span style="...">double </span><span style="...">x = </span><span style="...">1e-6 </span><span style="...">+ </span><span style="...">5.0</span><span style="...">;</span>
          <span style="...">cout &lt;&lt; </span><span style="...">"Hello world!"</span><span style="...">;</span>
          <span style="...">return </span><span style="...">0</span><span style="...">;</span>
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
      int main(void) {
          double x = 1e-6 + 5.0;
          cout << "Hello world!";
          return 0;
       }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-cpp">
      int main(void) {
          double x = 1e-6 + 5.0;
          cout << "Hello world!";
          return 0;
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
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">int </span><span style="...">main(</span><span style="...">void</span><span style="...">) {</span>
          <span style="...">double </span><span style="...">x = </span><span style="...">1e-6 </span><span style="...">+ </span><span style="...">5.0</span><span style="...">;</span>
          <span style="...">cout &lt;&lt; </span><span style="...">"Hello world!"</span><span style="...">;</span>
          <span style="...">return </span><span style="...">0</span><span style="...">;</span>
       <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">int </span><span style="...">main(</span><span style="...">void</span><span style="...">) {</span>
          <span style="...">double </span><span style="...">x = </span><span style="...">1e-6 </span><span style="...">+ </span><span style="...">5.0</span><span style="...">;</span>
          <span style="...">cout &lt;&lt; </span><span style="...">"Hello world!"</span><span style="...">;</span>
          <span style="...">return </span><span style="...">0</span><span style="...">;</span>
       <span style="...">}</span>
    </pre></span>
       <p>Inline code <span class="code"><span style="...">const </span><span style="...">a = </span><span style="...">"hello"</span></span></p>
      </div>
     </body>
    </html>
  """)
}
