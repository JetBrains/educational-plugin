package com.jetbrains.edu.python.learning.newproject;

import com.jetbrains.python.sdk.PySdkToInstall;
import com.jetbrains.python.sdk.PySdkToInstallKt;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Workaround to be able to invoke {@link PySdkToInstallKt#getSdksToInstall} from Kotlin code.
 * It's impossible to do it directly from Kotlin since the method has internal visibility.
 * <p>
 * Should be dropped when {@link PySdkToInstallKt#getSdksToInstall} becomes public
 */
public class PySdkToInstallUtils {

  @SuppressWarnings("KotlinInternalInJava")
  @NotNull
  public static List<PySdkToInstall> getSdksToInstall() {
    return PySdkToInstallKt.getSdksToInstall();
  }
}
