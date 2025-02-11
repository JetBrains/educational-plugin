package com.jetbrains.edu.cognifiredecompose.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

public final class DecomposeIcons {
  public static final Icon Function = load("/icons.cognifiredecompose/function.svg");

  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@SuppressWarnings("SameParameterValue") @NotNull String path) {
    return IconLoader.getIcon(path, DecomposeIcons.class);
  }
}
