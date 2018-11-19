package com.jetbrains.edu.javascript.learning.checkio.newProject;

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.library.core.NodeCoreLibraryConfigurator;
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable;
import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.JsCheckiOCourseBuilder;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsCheckiOCourseProjectGenerator extends CourseProjectGenerator<JsNewProjectSettings> {
  private final CheckiOCourseContentGenerator myContentGenerator =
    new CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector.getInstance());

  public JsCheckiOCourseProjectGenerator(@NotNull JsCheckiOCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    return CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress(myContentGenerator, (CheckiOCourse) myCourse);
  }

  @Override
  protected void afterProjectGenerated(@NotNull Project project, @NotNull JsNewProjectSettings projectSettings) {
    VirtualFile requestor = project.getBaseDir();
    NodeJsInterpreter interpreter = NodeJsInterpreterManager.getInstance(project).getInterpreter();
    if (interpreter == null) {
      showSettings(project, requestor);
    }
    else {
      ModalityState modalityState = ModalityState.current();
      interpreter.provideCachedVersionOrFetch(version -> ApplicationManager.getApplication().invokeLater(() -> {
        if (version != null) {
          NodeCoreLibraryConfigurator configurator = NodeCoreLibraryConfigurator.getInstance(project);
          configurator.configureAndAssociateWithProject(interpreter, version, null);
        }
        else {
          showSettings(project, requestor);
        }
      }, modalityState, project.getDisposed()));
    }
  }

  private static void showSettings(@NotNull Project project, @Nullable VirtualFile requestor) {
    ShowSettingsUtil.getInstance().editConfigurable(project, new NodeSettingsConfigurable(project, requestor, true));
  }
}
