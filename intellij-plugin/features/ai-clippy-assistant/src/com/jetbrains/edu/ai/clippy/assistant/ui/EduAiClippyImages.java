package com.jetbrains.edu.ai.clippy.assistant.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class EduAiClippyImages {
  public static final Icon Casino = load("/images/casino.png");
  public static final Icon Clippy = load("/images/clippy.svg");
  public static final Icon Frog = load("/images/frog.png");
  public static final Icon JacqueFresco = load("/images/jacque_fresco.png");
  public static final Icon Leha = load("/images/leha.jpg");
  public static final Icon Prigozhin = load("/images/prigozhin.png");
  public static final Icon VseRavno = load("/images/vse_ravno.png");

  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, EduAiClippyImages.class);
  }
}
