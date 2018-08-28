package com.jetbrains.edu.python.learning.checkio.utils;

import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.edu.python.learning.newproject.PyFakeSdkType;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PyCheckiOSdkValidator {
  @Nullable
  public static String validateSdk(@Nullable Sdk sdk) {
    if (sdk == null) {
      return "Specify Python interpreter location";
    }

    final LanguageLevel languageLevel = getLanguageLevel(sdk);
    if (!languageLevel.isPy3K()) {
      return "Python 3 is required to start this course";
    }
    return null;
  }

  @NotNull
  private static LanguageLevel getLanguageLevel(@NotNull Sdk sdk) {
    if (sdk.getSdkType() == PyFakeSdkType.INSTANCE) {
      final String pythonVersion = sdk.getVersionString();
      return pythonVersion == null ? LanguageLevel.getDefault() : LanguageLevel.fromPythonVersion(pythonVersion);
    } else {
      return PythonSdkType.getLanguageLevelForSdk(sdk);
    }
  }
}
