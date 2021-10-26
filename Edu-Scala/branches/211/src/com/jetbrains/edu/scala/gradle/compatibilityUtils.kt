package com.jetbrains.edu.scala.gradle

import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.util.ScalaMainMethodUtil

fun ScObject.findMainMethod(): scala.Option<PsiMethod> = ScalaMainMethodUtil.findMainMethod(this)
