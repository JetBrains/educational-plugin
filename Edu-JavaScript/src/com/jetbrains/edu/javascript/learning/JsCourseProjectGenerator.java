package com.jetbrains.edu.javascript.learning;

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable;
import com.intellij.lang.javascript.modules.InstallNodeLocalDependenciesAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText;

public class JsCourseProjectGenerator extends CourseProjectGenerator<JsNewProjectSettings> {
  public static final Logger LOG = Logger.getInstance(JsCourseProjectGenerator.class);

  public JsCourseProjectGenerator(@NotNull JsCourseBuilder builder, @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void afterProjectGenerated(@NotNull Project project, @NotNull JsNewProjectSettings projectSettings) {
    super.afterProjectGenerated(project, projectSettings);
    NodeJsInterpreter interpreter = projectSettings.getSelectedInterpreter();
    NodeJsInterpreterManager.getInstance(project).setInterpreterRef(interpreter.toRef());
    ModalityState modalityState = ModalityState.current();
    interpreter.provideCachedVersionOrFetch(version -> ApplicationManager.getApplication().invokeLater(() -> {
        if (version != null) {
          EduNodeJsCoreLibraryConfigurator.configureAndAssociateWithProject(project, interpreter, version);
        }
        else {
          LOG.warn("Couldn't retrieve Node interpreter version");
          VirtualFile requester = ModuleManager.getInstance(project).getModules()[0].getModuleFile();
          ShowSettingsUtil.getInstance().editConfigurable(project, new NodeSettingsConfigurable(project, requester, true));
        }
      }, modalityState, project.getDisposed()));
    }


  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) throws IOException {
    String packageJson = "package.json";
    VirtualFile packageJsonFile = baseDir.findChild(packageJson);
    if (packageJsonFile == null && !myCourse.isStudy()) {
      final String templateText = getInternalTemplateText(packageJson);
      packageJsonFile = GeneratorUtils.createChildFile(baseDir, packageJson, templateText);
    }

    if (packageJsonFile != null && !OpenApiExtKt.isUnitTestMode()) {
      InstallNodeLocalDependenciesAction.runAndShowConsole(project, packageJsonFile);
    }
  }
}
