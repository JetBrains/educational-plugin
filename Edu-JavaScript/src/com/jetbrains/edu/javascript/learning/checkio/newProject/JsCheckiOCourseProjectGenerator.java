package com.jetbrains.edu.javascript.learning.checkio.newProject;

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.library.core.NodeCoreLibraryConfigurator;
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.JsCheckiOCourseBuilder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOCourseProjectGenerator extends CourseProjectGenerator<JsNewProjectSettings> {
  public static final Logger LOG = Logger.getInstance(JsCheckiOCourseProjectGenerator.class);

  public JsCheckiOCourseProjectGenerator(@NotNull JsCheckiOCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    return true;
  }

  @Override
  protected void afterProjectGenerated(@NotNull Project project, @NotNull JsNewProjectSettings projectSettings) {
    NodeJsInterpreter interpreter = projectSettings.getSelectedInterpreter();
    NodeJsInterpreterManager.getInstance(project).setInterpreterRef(interpreter.toRef());
    ModalityState modalityState = ModalityState.current();
    interpreter.provideCachedVersionOrFetch(version -> ApplicationManager.getApplication().invokeLater(() -> {
        if (version != null) {
          NodeCoreLibraryConfigurator configurator = NodeCoreLibraryConfigurator.getInstance(project);
          configurator.configureAndAssociateWithProject(interpreter, version, null);
        }
        else {
          LOG.warn("Couldn't retrieve Node interpreter version");
          VirtualFile requestor = ModuleManager.getInstance(project).getModules()[0].getModuleFile();
          ShowSettingsUtil.getInstance().editConfigurable(project, new NodeSettingsConfigurable(project, requestor, true));
        }
      }, modalityState, project.getDisposed()));
    }
}
