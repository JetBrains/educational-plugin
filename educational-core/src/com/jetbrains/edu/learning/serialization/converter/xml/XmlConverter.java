package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public interface XmlConverter {
  @NotNull
  Element convert(@NotNull Project project, @NotNull Element element) throws StudyUnrecognizedFormatException;
}
