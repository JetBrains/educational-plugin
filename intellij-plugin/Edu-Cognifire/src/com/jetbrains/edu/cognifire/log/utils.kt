package com.jetbrains.edu.cognifire.log

import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression

fun PromptExpression?.toPromptData(): PromptData =
  if (this == null) PromptData("", "", "")
  else PromptData(this.prompt, this.code, this.functionSignature.toString())


fun CodeExpression?.toCodeData(): CodeData =
  if (this == null) CodeData("")
  else CodeData(this.code)