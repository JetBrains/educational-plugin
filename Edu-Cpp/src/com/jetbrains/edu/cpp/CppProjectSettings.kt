package com.jetbrains.edu.cpp

import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard
import com.jetbrains.edu.learning.newproject.EduProjectSettings

data class CppProjectSettings(val languageStandard: String = CMakeRecognizedCPPLanguageStandard.CPP14.standard) : EduProjectSettings