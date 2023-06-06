package com.jetbrains.edu.learning.configuration;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.jetbrains.edu.learning.courseFormat.EduFormatNames;
import org.jetbrains.annotations.Nullable;

public class EducationalExtensionPoint<T> extends BaseKeyedLazyInstance<T> {
  public static final ExtensionPointName<EducationalExtensionPoint<EduConfigurator<?>>> EP_NAME =
    ExtensionPointName.create("Educational.configurator");

  @Attribute("implementationClass")
  @RequiredElement
  public String implementationClass;

  @Attribute("language")
  @RequiredElement
  public String language = "";

  @Attribute("courseType")
  public String courseType = EduFormatNames.PYCHARM;

  @Attribute("environment")
  public String environment = "";

  @Attribute("displayName")
  public String displayName = null;

  @Override
  protected @Nullable String getImplementationClassName() {
    return implementationClass;
  }
}
