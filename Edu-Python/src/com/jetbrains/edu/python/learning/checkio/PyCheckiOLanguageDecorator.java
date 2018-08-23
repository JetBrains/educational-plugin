package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.python.learning.PyLanguageDecorator;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PyCheckiOLanguageDecorator extends PyLanguageDecorator {
  @NotNull
  @Override
  public Icon getLogo() {
    return EducationalCoreIcons.CheckiO;
  }
}
