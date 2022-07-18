package com.jetbrains.edu.javascript.learning;

import com.intellij.javascript.nodejs.interpreter.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.UserDataHolder;
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle;
import com.jetbrains.edu.learning.LanguageSettings;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JS;

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
  public List<LabeledComponent<JComponent>> getLanguageSettingsComponents(@NotNull Course course,
                                                                          @NotNull Disposable disposable,
                                                                          @Nullable UserDataHolder context) {
    return Collections.singletonList(
      LabeledComponent.create(myInterpreterField, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST));
  }

  @Nullable
  @Override
  public ValidationMessage validate(@Nullable Course course, @Nullable String courseLocation) {
    NodeJsInterpreter interpreter = myInterpreterField.getInterpreter();
    String message = NodeInterpreterUtil.validateAndGetErrorMessage(interpreter);
    if (message == null) return null;
    return new ValidationMessage(EduJavaScriptBundle.message("configure.js.environment.help", message, ENVIRONMENT_CONFIGURATION_LINK_JS),
                                 ENVIRONMENT_CONFIGURATION_LINK_JS);
  }
}
