package com.jetbrains.edu.learning.taskToolWindow

import org.junit.Test

class TaskDescriptionCssTest : TaskDescriptionTestBase() {

  @Test
  fun `test remote style tag using swing`() = doHtmlTest("""
    Some <b>simple</b> text.
    
    <style>
    img {
      display: block;
      margin-left: auto;
      margin-right: auto;
    }
    </style>
    Some <i>other</i> text.     
  """, """
    <html>
     <head>...</head>
     <body>
      <div class="wrapper">
       Some <b>simple</b> text.
       <style>
    img {
      display: block;
      margin-left: auto;
      margin-right: auto;
    }
    </style>
       Some <i>other</i> text.
      </div>
     </body>
    </html>
  """, """
    <html>
     <head>...</head>
     <body>
      <div class="wrapper">
       Some <b>simple</b> text. Some <i>other</i> text.
      </div>
     </body>
    </html>
  """)
}
