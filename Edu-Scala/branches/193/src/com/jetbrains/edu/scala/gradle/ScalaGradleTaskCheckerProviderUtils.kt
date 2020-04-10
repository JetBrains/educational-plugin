package com.jetbrains.edu.scala.gradle

import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.runner.ScalaMainMethodUtil
import scala.Option

val findMainMethod: (ScObject) -> Option<PsiMethod> = { ScalaMainMethodUtil.findMainMethod(it) }