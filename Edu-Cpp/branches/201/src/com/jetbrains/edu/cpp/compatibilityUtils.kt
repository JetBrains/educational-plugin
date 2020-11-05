package com.jetbrains.edu.cpp

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.execution.CidrTargetRunLineMarkerProvider.isInEntryPointBody

fun isInEntryPointBody(element: PsiElement): Boolean = isInEntryPointBody(element)