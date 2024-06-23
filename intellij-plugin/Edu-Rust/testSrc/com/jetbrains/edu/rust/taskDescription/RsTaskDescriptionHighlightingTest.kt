package com.jetbrains.edu.rust.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.taskToolWindow.TaskDescriptionHighlightingTestBase
import org.junit.Test
import org.rust.lang.RsLanguage

class RsTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  override val language: Language = RsLanguage

  @Test
  fun `test markdown description highlighting`() = doMarkdownTest("""
    Code block with default language:
    ```
      pub struct Foo<'a> {
          s: &'a str
      }
    ```

    Code block with specific language:
    ```rust
      fn main() {
          println!("Hello!")
      }
    ```

    Inline code `if condition { 1 } else { 2 }`
  """, """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">pub struct </span><span style="...">Foo&lt;</span><span style="...">'a</span><span style="...">&gt; {</span>
          <span style="...">s: &amp;</span><span style="...">'a </span><span style="...">str</span>
      <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">fn </span><span style="...">main() {</span>
          <span style="...">println!(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">condition { </span><span style="...">1 </span><span style="...">} </span><span style="...">else </span><span style="...">{ </span><span style="...">2 </span><span style="...">}</span></span></p>
      </div>
     </body>
    </html>
  """)

  @Test
  fun `test html description highlighting`() = doHtmlTest("""
    <html>
    <p>Code block with default language:</p>
    <pre><code>
      pub struct Foo<'a> {
          s: &'a str
      }
    </code></pre>
    <p>Code block with specific language:</p>
    <pre><code data-lang="text/x-kotlin">
      fn main() {
          println!("Hello!")
      }
    </code></pre>
    <p>Inline code <code>if condition { 1 } else { 2 }</code></p>
    </html>
  """, """
    <html>
     <head>
      ...
     </head>
     <body>
      <div class="wrapper">
       <p>Code block with default language:</p><span class="code-block">
        <pre>  <span style="...">pub struct </span><span style="...">Foo&lt;</span><span style="...">'a</span><span style="...">&gt; {</span>
          <span style="...">s: &amp;</span><span style="...">'a </span><span style="...">str</span>
      <span style="...">}</span>
    </pre></span>
       <p>Code block with specific language:</p><span class="code-block">
        <pre>  <span style="...">fn </span><span style="...">main() {</span>
          <span style="...">println!(</span><span style="...">"Hello!"</span><span style="...">)</span>
      <span style="...">}</span>
    </pre></span>
       <p>Inline code <span class="code"><span style="...">if </span><span style="...">condition { </span><span style="...">1 </span><span style="...">} </span><span style="...">else </span><span style="...">{ </span><span style="...">2 </span><span style="...">}</span></span></p>
      </div>
     </body>
    </html>
  """)
}
