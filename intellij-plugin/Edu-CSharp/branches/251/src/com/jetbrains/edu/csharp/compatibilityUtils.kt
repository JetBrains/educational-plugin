package com.jetbrains.edu.csharp

import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rider.model.RdUnitTestResultData
import com.jetbrains.rider.model.RdUnitTestSession

val RdUnitTestSession.testResultData: IProperty<RdUnitTestResultData?>?
  get() = resultData