package com.jetbrains.edu.learning.eduAssistant.core.prompt

import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

fun formatCodeResponsePrompt(description: String, codeStr: String, language: String, taskProcessor: TaskProcessor) = """
    $description
    
    The student's code:
    ```$language
    $codeStr
    ```
    
    ${buildTaskErrorInformation(taskProcessor)}
    
    Write the response in the following format:
    Response: <non-code text>
    Code: ```$language
    <code>
    ```
    
    The code response should include the entire function or code block with the new changes incorporated into it.
  """.trimIndent()

fun buildTaskErrorInformation(taskProcessor: TaskProcessor) = if (taskProcessor.taskHasErrors()) {
  """
      The present code does not pass test <${taskProcessor.getFailedTestName()}>, the error message is <${taskProcessor.getFailureMessage()}>.
      ${
    if (taskProcessor.getExpectedValue() != null && taskProcessor.getActualValue() != null) {
      """
          The expected test output was:
          ${taskProcessor.getExpectedValue()}
          But the actual test output was:
          ${taskProcessor.getActualValue()}
        """.trimIndent()
    }
    else ""
  }
      If the task appears to be completed, provide a step-by-step guide that explains what changes need to be made to the code to fix the failing test.
    """.trimIndent()
}
else {
  ""
}
