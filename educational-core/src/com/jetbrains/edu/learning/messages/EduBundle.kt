package com.jetbrains.edu.learning.messages

import com.intellij.DynamicBundle
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.*

abstract class EduBundle(pathToBundle: String) : DynamicBundle(pathToBundle)