package com.jetbrains.edu.cognifire.log

import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.edu.cognifire.utils.toPrompt
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

fun isPromptChanged(current: PromptToCodeContent, previous: PromptToCodeContent?): Boolean =
  if (previous == null) true
  else current.toPrompt().ignoreWhiteSpaces() == previous.toPrompt().ignoreWhiteSpaces()


fun isGeneratedCodeChanged(current: PromptToCodeContent, previous: PromptToCodeContent?): Boolean =
  if (previous == null) true
  else current.toGeneratedCode().ignoreWhiteSpaces() == previous.toGeneratedCode().ignoreWhiteSpaces()

fun PromptExpression?.toPromptData(): PromptData =
  if (this == null) PromptData("", "", "")
  else PromptData(this.prompt, this.code, this.functionSignature.toString())


fun CodeExpression?.toCodeData(): CodeData =
  if (this == null) CodeData("")
  else CodeData(this.code)

fun String.ignoreWhiteSpaces() = this.replace(" ", "")