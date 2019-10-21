package com.jetbrains.edu.cpp

import com.intellij.lang.Language
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase

class CppTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  override val language: Language = OCLanguage.getInstance()
  override val settings = CppProjectSettings()
  override val environment = "GoogleTest"

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
  """, """<html>
 <head></head>
 <body md-src-pos="0..328">
  <p md-src-pos="0..33">Code block with default language:</p>
  <span class="code-block"><pre>  <span style="...">int </span>main(<span style="...">void</span>) {
      <span style="...">double </span>x = <span style="...">1e-6 </span>+ <span style="...">5.0</span>;
      cout &lt;&lt; <span style="...">"Hello world!"</span>;
      <span style="...">return </span><span style="...">0</span>;
   }
</pre> </span>
  <p md-src-pos="142..176">Code block with specific language:</p>
  <span class="code-block"><pre>   <span style="...">int </span>main(<span style="...">void</span>) {
      <span style="...">double </span>x = <span style="...">1e-6 </span>+ <span style="...">5.0</span>;
      cout &lt;&lt; <span style="...">"Hello world!"</span>;
      <span style="...">return </span><span style="...">0</span>;
   }
</pre> </span>
  <p md-src-pos="289..328">Inline code <span class="code"><span style="...">if </span>(condition) {} <span style="...">else </span>{}</span></p>
 </body>
</html>
  """)


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
  """, """<html>
 <head></head>
 <body>
  <p>Code block with default language:</p>
  <span class="code-block"><pre>
  <span style="...">int </span>main(<span style="...">void</span>) {
      <span style="...">double </span>x = <span style="...">1e-6 </span>+ <span style="...">5.0</span>;
      cout &lt;&lt; <span style="...">"Hello world!"</span>;
      <span style="...">return </span><span style="...">0</span>;
   }
</pre> </span>
  <p>Code block with specific language:</p>
  <span class="code-block"><pre>
  <span style="...">int </span>main(<span style="...">void</span>) {
      <span style="...">double </span>x = <span style="...">1e-6 </span>+ <span style="...">5.0</span>;
      cout &lt;&lt; <span style="...">"Hello world!"</span>;
      <span style="...">return </span><span style="...">0</span>;
   }
</pre> </span>
  <p>Inline code <span class="code"><span style="...">const </span>a = <span style="...">"hello"</span></span></p>
 </body>
</html>
  """)
}

