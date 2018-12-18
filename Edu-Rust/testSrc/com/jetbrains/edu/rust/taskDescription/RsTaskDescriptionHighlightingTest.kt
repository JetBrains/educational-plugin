package com.jetbrains.edu.rust.taskDescription

import com.intellij.lang.Language
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionHighlightingTestBase
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.lang.RsLanguage

class RsTaskDescriptionHighlightingTest : TaskDescriptionHighlightingTestBase() {
  override val language: Language = RsLanguage
  override val settings: Any = RsProjectSettings(null)

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
     <head></head>
     <body md-src-pos="0..221">
      <p md-src-pos="0..33">Code block with default language:</p>
      <span class="code-block"><pre>  <span style="...">pub struct </span>Foo&lt;<span style="...">'a</span>&gt; {
          s: &amp;<span style="...">'a </span>str
      }
    </pre> </span>
      <p md-src-pos="87..121">Code block with specific language:</p>
      <span class="code-block"><pre>  <span style="...">fn </span>main() {
          println!(<span style="...">"Hello!"</span>)
      }
    </pre> </span>
      <p md-src-pos="178..221">Inline code <span class="code"><span style="...">if </span>condition { <span style="...">1 </span>} <span style="...">else </span>{ <span style="...">2 </span>}</span></p>
     </body>
    </html>
  """)

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
     <head></head>
     <body>
      <p>Code block with default language:</p>
      <span class="code-block"><pre>
      <span style="...">pub struct </span>Foo&lt;<span style="...">'a</span>&gt; {
          s: &amp;<span style="...">'a </span>str
      }
    </pre> </span>
      <p>Code block with specific language:</p>
      <span class="code-block"><pre>
      <span style="...">fn </span>main() {
          println!(<span style="...">"Hello!"</span>)
      }
    </pre> </span>
      <p>Inline code <span class="code"><span style="...">if </span>condition { <span style="...">1 </span>} <span style="...">else </span>{ <span style="...">2 </span>}</span></p>
     </body>
    </html>
  """)
}
