package com.jetbrains.edu.learning.eduAssistant.core.prompt

import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

class CompilationErrorPromptBuilder : PromptBuilder {

  override fun buildTextHintPrompt(taskProcessor: TaskProcessor, codeHint: String, codeStr: String, language: String) = """
    Based on the given code and the improved version of the code, provide a concise textual hint that directly guides to improve the given code.
    The code contains a compilation error, and the improved code fixes this error.
    Your objective is to identify compilation errors, and offering clear, educational feedback. 
    
    The code:
    ```$language
    $codeStr
    ```
    
    The improved version of the code:
    ```$language
    $codeHint
    ```
    
    Compilation error details: <${taskProcessor.getErrorDetails()}>
    
    Respond with a BRIEF EXPLICIT explanation in a few words of how the compilation errors appear in the code and a BRIEF textual instruction in IMPERATIVE form of what modifications need to be made to the code to fix the error exhibited in the improved code.
    Take into account in your response ONLY the error in the line indicated in the compilation error details.
    DO NOT write using paragraphs, bullet points, headers, or line breaks, and ensure all text is contained within a single continuous string.
    DO NOT write any code, except names of functions or string literals. 
    DO NOT write in which file and in which line of code the error is made.
    DO NOT INCLUDE introductory or concluding remarks, and MUST NOT include unessential general feedback, or use advanced vocabulary.   
  """.trimIndent()

  override fun buildTextHintPromptIfNoCodeHintIsGenerated(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String
  ) = """
    Based on the given code, determine how to fix it to make the code compile successfully. 
    Fix ONLY the error in the line indicated in the compilation error details.
    
    The code:
    ```$language
    $codeStr
    ```
    
    Compilation error details: <${taskProcessor.getErrorDetails()}>
    
    Respond with a BRIEF EXPLICIT explanation in a few words of how the compilation errors appear in the code and a BRIEF textual instruction in IMPERATIVE form of what modifications need to be made to the code to fix the error exhibited in the improved code. 
    DO NOT write any code, except names of functions or string literals. 
    DO NOT write in which file and in which line of code the error is made.
    DO NOT INCLUDE introductory or concluding remarks, and MUST NOT include unessential general feedback, or use advanced vocabulary.   
  """.trimIndent()

  override fun buildCodeHintPrompt(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String
  ) = formatCodeResponsePrompt("""
    Generate a modified version of the provided student's code that incorporates the compilation error fix. 
    Fix ONLY the error in the line indicated in the compilation error details. Maintain the original structure of the student's code and focus especially on addressing the error.
    The result MUST FOLLOW best practices for the $language programming language.
    Here is the compilation error details and the student's code, all delimited with <>:
    
    Compilation error details: <${taskProcessor.getErrorDetails()}>
  """.trimIndent(), codeStr, language, taskProcessor)

  override fun buildCodeHintPromptFromTextHint(
    taskProcessor: TaskProcessor, textHint: String, codeStr: String, language: String
  ) = formatCodeResponsePrompt("""
    Implement the fix to make the code compile successfully.
    Here is the description of how to fix the code and the code, all delimited with <>:
    
    How to fix the code: <$textHint>
  """.trimIndent(), codeStr, language, taskProcessor)
}
