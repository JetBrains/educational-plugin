package com.jetbrains.edu.learning.eduAssistant.core.prompt

import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

class CompilationErrorPromptBuilder : PromptBuilder {

  override fun buildTextHintPrompt(codeHint: String, codeStr: String, language: String) = """
    Based on the given code and the improved version of the code, provide a concise textual hint that directly guides to improve the given code.
    The code contains a compilation error, and the improved code fixes this error.
    
    The code:
    ```$language
    <$codeStr>
    ```
    
    The improved version of the code:
    ```$language
    <$codeHint>
    ```
    
    Respond with a brief textual instruction in imperative form of what modifications need to be made to the code to achieve the improvements exhibited in the improved code. 
    Do not write any code, except names of functions or string literals. 
  """.trimIndent()

  override fun buildTextHintPromptIfNoCodeHintIsGenerated(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String
  ) = """
    Based on the given code, determine how to fix it to make the code compile successfully.
    
    The code:
    ```$language
    <$codeStr>
    ```
    
    Respond with a brief textual instruction in imperative form of how to fix it.
    Do not write any code, except names of functions that can be used in the solution.
  """.trimIndent()

  override fun buildCodeHintPrompt(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String
  ) = formatCodeResponsePrompt("""
    Generate a modified version of the provided student's code that incorporates the compilation error fix. 
    Try to maintain the original structure of the student's code and focus especially on addressing the error.
    Here is the student's code, delimited with <>:
  """.trimIndent(), codeStr, language, taskProcessor)

  override fun buildCodeHintPromptFromTextHint(
    taskProcessor: TaskProcessor, textHint: String, codeStr: String, language: String
  ) = formatCodeResponsePrompt("""
    Implement the fix to make the code compile successfully.
    Here is the description of how to fix the code and the code, all delimited with <>:
    
    How to fix the code: <$textHint>
  """.trimIndent(), codeStr, language, taskProcessor)
}
