package com.jetbrains.edu.scala.gradle

import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.util.ScalaMainMethodUtil

// This code works only for Scala 2 code
// TODO: support Scala 3
fun ScObject.findMainMethod(): scala.Option<PsiMethod> = ScalaMainMethodUtil.findScala2MainMethod(this)
