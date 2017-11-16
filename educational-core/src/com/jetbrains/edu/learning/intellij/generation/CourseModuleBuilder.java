package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;

import java.util.Collections;

public class CourseModuleBuilder extends EduBaseIntellijModuleBuilder {

  @Override
  public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
    setSourcePaths(Collections.emptyList());
    super.setupRootModel(rootModel);
  }
}
