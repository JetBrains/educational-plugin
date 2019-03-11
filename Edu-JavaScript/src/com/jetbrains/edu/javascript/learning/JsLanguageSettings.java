package com.jetbrains.edu.javascript.learning;

import com.intellij.javascript.nodejs.interpreter.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.jetbrains.edu.learning.LanguageSettings;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.ui.ErrorMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class JsLanguageSettings extends LanguageSettings<JsNewProjectSettings> {
  private final JsNewProjectSettings mySettings = new JsNewProjectSettings();
  private final NodeJsInterpreterField myInterpreterField;

  public JsLanguageSettings() {
    Project defaultProject = ProjectManager.getInstance().getDefaultProject();
    myInterpreterField = new NodeJsInterpreterField(defaultProject, false) {
      @Override
      public boolean isDefaultProjectInterpreterField() {
        return true;
      }
    };
    myInterpreterField.addChangeListener(new NodeJsInterpreterChangeListener() {
      @Override
      public void interpreterChanged(@Nullable NodeJsInterpreter interpreter) {
        mySettings.setSelectedInterpreter(interpreter);
      }
    });
    myInterpreterField.setInterpreterRef(NodeJsInterpreterManager.getInstance(defaultProject).getInterpreterRef());
  }

  @NotNull
  @Override
  public JsNewProjectSettings getSettings() {
    return mySettings;
  }

  @NotNull
  @Override
  public List<LabeledComponent<JComponent>> getLanguageSettingsComponents(@NotNull Course course) {
    return Collections.singletonList(LabeledComponent.create(myInterpreterField, "Interpreter", BorderLayout.WEST));
  }

  @Nullable
  @Override
  public ErrorMessage validate(@Nullable Course course) {
    NodeJsInterpreter interpreter = myInterpreterField.getInterpreter();
    String message = NodeInterpreterUtil.validateAndGetErrorMessage(interpreter);
    return message != null ? new ErrorMessage(message) : null;
  }
}
