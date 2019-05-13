package com.jetbrains.edu.cpp

import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard

data class CppProjectSettings(val languageStandard: String = CMakeRecognizedCPPLanguageStandard.CPP14.standard)