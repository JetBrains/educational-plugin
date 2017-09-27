/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.newproject;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface EduCourseProjectGenerator {
  @NotNull
  DirectoryProjectGenerator getDirectoryProjectGenerator();

  @NotNull
  Object getProjectSettings();

  void setCourse(@NotNull Course course);

  ValidationResult validate();

  boolean beforeProjectGenerated();

  void afterProjectGenerated(@NotNull Project project);

  @Nullable
  default LabeledComponent<JComponent> getLanguageSettingsComponent(@NotNull Course selectedCourse) {
    return null;
  }

  default void createProject(@NotNull Course course, @NotNull String location) {
    setCourse(course);
    if (!beforeProjectGenerated()) {
      return;
    }
    DirectoryProjectGenerator directoryProjectGenerator = getDirectoryProjectGenerator();
    Project createdProject = AbstractNewProjectStep.doGenerateProject(null, location, directoryProjectGenerator, getProjectSettings());
    if (createdProject == null) {
      return;
    }
    afterProjectGenerated(createdProject);
  }
}
