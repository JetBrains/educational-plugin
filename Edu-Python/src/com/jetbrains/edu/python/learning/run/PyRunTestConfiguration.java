package com.jetbrains.edu.python.learning.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.courseFormat.ext.StudyItemExtKt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.python.learning.messages.EduPythonBundle;
import com.jetbrains.python.run.PythonRunConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PyRunTestConfiguration extends PythonRunConfiguration {
  public static final String PATH_ATTR = "studyTest";
  private final Project myProject;
  private String myPathToTest;

  public PyRunTestConfiguration(Project project, ConfigurationFactory factory) {
    super(project, factory);
    myProject = project;
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return PyCommandLineState.createInstance(this, environment);
  }

  @Override
  protected SettingsEditor<PythonRunConfiguration> createConfigurationEditor() {
    @SuppressWarnings("unchecked")
    SettingsEditor<PythonRunConfiguration> editor =
      (SettingsEditor<PythonRunConfiguration>)(SettingsEditor<?>)(new PySettingsEditor(myProject));
    return editor;
  }

  public String getPathToTest() {
    return myPathToTest;
  }

  public void setPathToTest(String pathToTest) {
    myPathToTest = pathToTest;
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    myPathToTest = JDOMExternalizerUtil.readField(element, PATH_ATTR);
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, PATH_ATTR, myPathToTest);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();
    VirtualFile testsFile = LocalFileSystem.getInstance().findFileByPath(myPathToTest);
    if (testsFile == null) {
      throw new RuntimeConfigurationException(EduPythonBundle.message("error.invalid.path.to.file"));
    }
    Task task = VirtualFileExt.getContainingTask(testsFile, myProject);
    if (task == null) {
      throw new RuntimeConfigurationException(EduPythonBundle.message("error.invalid.path.to.file"));
    }
    VirtualFile taskDir = StudyItemExtKt.getDir(task, OpenApiExtKt.getCourseDir(myProject));
    if (taskDir == null) {
      throw new RuntimeConfigurationException(EduPythonBundle.message("error.invalid.path.to.file"));
    }
  }
}
