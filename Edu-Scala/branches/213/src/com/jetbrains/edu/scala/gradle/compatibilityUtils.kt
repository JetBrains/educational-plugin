package com.jetbrains.edu.scala.gradle

import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.runner.MyScalaMainMethodUtil

// This code works only for Scala 2 code
// TODO: support Scala 3
//  ideally, without using `MyScalaMainMethodUtil` at all because it's too low-level and it's subject to change
fun ScObject.findMainMethod(): scala.Option<PsiMethod> = MyScalaMainMethodUtil.findScala2MainMethod(this)
