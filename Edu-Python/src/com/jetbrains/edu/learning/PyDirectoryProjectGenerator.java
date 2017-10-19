package com.jetbrains.edu.learning;

import com.intellij.execution.ExecutionException;
import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.util.BooleanFunction;
import com.intellij.util.PlatformUtils;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson;
import com.jetbrains.edu.coursecreator.actions.CCCreateTask;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import com.jetbrains.edu.learning.stepic.EduAdaptiveStepicConnector;
import com.jetbrains.edu.learning.stepic.EduStepicConnector;
import com.jetbrains.edu.learning.stepic.StepicUser;
import com.jetbrains.edu.learning.ui.StudyNewProjectPanel;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import com.jetbrains.python.newProject.PythonProjectGenerator;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.remote.PyProjectSynchronizer;
import com.jetbrains.python.sdk.PyDetectedSdk;
import com.jetbrains.python.sdk.PySdkExtKt;
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor;
import icons.PythonIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class PyDirectoryProjectGenerator extends PythonProjectGenerator<PyNewProjectSettings>
  implements EduCourseProjectGenerator<PyNewProjectSettings> {

  private static final Logger LOG = Logger.getInstance(PyDirectoryProjectGenerator.class);
  private static final String NO_PYTHON_INTERPRETER = "<html><u>Add</u> python interpreter.</html>";

  private final Course myCourse;
  private final StudyProjectGenerator myGenerator;
  private final boolean isLocal;

  private ValidationResult myValidationResult = new ValidationResult("selected course is not valid");
  private PyNewProjectSettings mySettings = new PyNewProjectSettings();

  // Some python API has been changed while 2017.3 (first version of python plugin with new API is 2017.3.173.3415.6).
  // To prevent exceptions because of it we should check if it is new API or not.
  protected final boolean myHasOldPythonApi;

  public PyDirectoryProjectGenerator(@NotNull Course course, boolean isLocal) {
    myCourse = course;
    this.isLocal = isLocal;
    myGenerator = new StudyProjectGenerator();
    myGenerator.addSettingsStateListener(new StudyProjectGenerator.SettingsListener() {
      @Override
      public void stateChanged(ValidationResult result) {
        setValidationResult(result);
      }
    });
    myHasOldPythonApi = hasOldPythonApi();
  }

  private boolean hasOldPythonApi() {
    try {
      // `com.jetbrains.python.sdk.PySdkExtKt` is part of new python API
      // so we can use it to determine if it is new python API or not.
      // This way looks easier than check version because
      // there are different IDE with python support: PyCharm C/P/EDU and other IDEs with python plugin
      // and we have to use separate way to check API version for each case.
      Class.forName("com.jetbrains.python.sdk.PySdkExtKt");
      return false;
    } catch (ClassNotFoundException e) {
      LOG.warn("Current python API is old");
      return true;
    }
  }

  @Nls
  @NotNull
  @Override
  public String getName() {
    return "Educational";
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return PythonIcons.Python.Python_logo;
  }

  @Override
  public void configureProject(@NotNull final Project project, @NotNull final VirtualFile baseDir,
                               @NotNull PyNewProjectSettings settings,
                               @NotNull Module module,
                               @Nullable PyProjectSynchronizer synchronizer) {
    if (myCourse.isStudy()) {
      myGenerator.setSelectedCourse(myCourse);
      myGenerator.generateProject(project, baseDir);
      ApplicationManager.getApplication().runWriteAction(() -> createTestHelper(project, baseDir));
    } else {
      configureNewCourseProject(project, baseDir);
    }
  }

  private void configureNewCourseProject(@NotNull Project project, @NotNull VirtualFile baseDir) {
    StudyTaskManager.getInstance(project).setCourse(myCourse);
    StudyUtils.registerStudyToolWindow(myCourse, project);

    ApplicationManager.getApplication().runWriteAction(() -> {
      createTestHelper(project, baseDir);
      VirtualFile lessonDir = new CCCreateLesson().createItem(project, baseDir, myCourse, false);
      if (lessonDir == null) {
        LOG.error("Failed to create lesson");
        return;
      }
      new CCCreateTask().createItem(project, lessonDir, myCourse, false);
    });
  }

  private static void createTestHelper(@NotNull Project project, @NotNull VirtualFile baseDir) {
    final String testHelper = EduNames.TEST_HELPER;
    if (baseDir.findChild(testHelper) != null) return;
    final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate("test_helper");
    try {
      StudyGenerator.createChildFile(project.getBaseDir(), testHelper, template.getText());
    }
    catch (IOException exception) {
      LOG.error("Can't copy test_helper.py " + exception.getMessage());
    }
  }

  @NotNull
  @Override
  public ValidationResult validate() {
    final List<Sdk> sdks = getAllSdks();

    ValidationResult validationResult;
    if (sdks.isEmpty()) {
      validationResult = new ValidationResult(NO_PYTHON_INTERPRETER);
    } else {
      validationResult = ValidationResult.OK;
    }

    return validationResult;
  }

  @NotNull
  @Override
  public ValidationResult validate(@NotNull String s) {
    ValidationResult validationResult = validate();
    if (!validationResult.isOk()) {
      myValidationResult = validationResult;
    }

    return myValidationResult;
  }

  @Override
  public boolean beforeProjectGenerated() {
    BooleanFunction<PythonProjectGenerator> function = beforeProjectGenerated(null);
    return function != null && function.fun(this);
  }

  @Override
  public void afterProjectGenerated(@NotNull Project project) {
    Sdk sdk = mySettings.getSdk();

    if (sdk != null && sdk.getSdkType() == FakePythonSdkType.INSTANCE) {
      createAndAddVirtualEnv(project, mySettings);
      sdk = mySettings.getSdk();
    }
    sdk = updateSdkIfNeeded(project, sdk);
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk);
  }

  @Nullable
  @Override
  public LabeledComponent<JComponent> getLanguageSettingsComponent() {
    // It is rather hard to support python interpreter combobox
    // and virtual env creation using old API
    // so we decided to turn off it in this case.
    if (myHasOldPythonApi) {
      LOG.warn("We won't show interpreter combobox because current python API is old");
      return null;
    }

    // by default we create new virtual env in project, we need to add this non-existing sdk to sdk list
    ProjectJdkImpl fakeSdk = createFakeSdk(myCourse);

    ComboboxWithBrowseButton combo = getInterpreterComboBox(fakeSdk);
    if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
      combo.putClientProperty("JButton.buttonType", null);
    }
    combo.setButtonIcon(PythonIcons.Python.InterpreterGear);
    return LabeledComponent.create(combo, "Interpreter", BorderLayout.WEST);
  }

  protected void onSdkSelected(@Nullable Sdk sdk) {
    mySettings.setSdk(sdk);
  }

  @Nullable
  private static ProjectJdkImpl createFakeSdk(@NotNull Course selectedCourse) {
    String fakeSdkPath = getBaseSdk(selectedCourse);
    if (fakeSdkPath == null) {
      return null;
    }
    PythonSdkFlavor flavor = PythonSdkFlavor.getApplicableFlavors(false).get(0);
    String prefix = flavor.getName() + " ";
    String versionString = flavor.getVersionString(fakeSdkPath);
    if (versionString == null || !versionString.contains(prefix)) {
      return null;
    }
    String name = "new virtual env " + versionString.substring(prefix.length());
    return new ProjectJdkImpl(name, FakePythonSdkType.INSTANCE);
  }

  public void setValidationResult(ValidationResult validationResult) {
    myValidationResult = validationResult;
  }

  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {
    StudyNewProjectPanel mySettingsPanel = new StudyNewProjectPanel(myGenerator, isLocal);
    mySettingsPanel.registerValidators(new FacetValidatorsManager() {
      public void registerValidator(FacetEditorValidator validator, JComponent... componentsToWatch) {
        throw new UnsupportedOperationException();
      }

      public void validate() {
        ApplicationManager.getApplication().invokeLater(() -> fireStateChanged());
      }
    });

    addErrorLabelMouseListener(new MouseAdapter() {
      private boolean isCourseAdaptiveAndNotLogged() {
        Course course = myGenerator.getSelectedCourse();
        return course != null && course.isAdaptive() && !myGenerator.isLoggedIn();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (isCourseAdaptiveAndNotLogged()) {
          EduSettings eduSettings = EduSettings.getInstance();
          StepicUser oldUser = eduSettings.getUser();

          EduStepicConnector.doAuthorize(() -> mySettingsPanel.showLoginDialog());

          ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(() -> {
                                                   ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                                                   StepicUser user = StudyUtils.execCancelable(() -> {
                                                     StepicUser newUser = eduSettings.getUser();
                                                     while (newUser == null || newUser.equals(oldUser)) {
                                                       TimeUnit.MILLISECONDS.sleep(500);
                                                       newUser = eduSettings.getUser();
                                                     }
                                                     myGenerator.setEnrolledCoursesIds(EduAdaptiveStepicConnector.getEnrolledCoursesIds(newUser));


                                                     return newUser;
                                                   });

                                                   if (user != null) {
                                                     mySettingsPanel.setOK();
                                                   }
                                                 }, "Authorizing",
                                                 true,
                                                 DefaultProjectFactory.getInstance().getDefaultProject());
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        if (isCourseAdaptiveAndNotLogged()) {
          e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (isCourseAdaptiveAndNotLogged()) {
          e.getComponent().setCursor(Cursor.getDefaultCursor());
        }
      }
    });

    return mySettingsPanel;
  }

  public StudyProjectGenerator getGenerator() {
    return myGenerator;
  }

  @Override
  public boolean hideInterpreter() {
    return true;
  }

  @Nullable
  @Override
  public BooleanFunction<PythonProjectGenerator> beforeProjectGenerated(@Nullable Sdk sdk) {
    return generator -> {
      final List<Integer> enrolledCoursesIds = myGenerator.getEnrolledCoursesIds();
      final Course course = myGenerator.getSelectedCourse();
      if (course == null || !(course instanceof RemoteCourse)) return true;
      if (((RemoteCourse)course).getId() > 0 && !enrolledCoursesIds.contains(((RemoteCourse)course).getId())) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
          return StudyUtils.execCancelable(() -> EduStepicConnector.enrollToCourse(((RemoteCourse)course).getId(),
                                                                                   EduSettings.getInstance().getUser()));
        }, "Creating Course", true, ProjectManager.getInstance().getDefaultProject());
      }
      return true;
    };
  }

  public void createAndAddVirtualEnv(@NotNull Project project, @NotNull PyNewProjectSettings settings) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final String baseSdkPath = getBaseSdk(course);
    if (baseSdkPath != null) {
      final PyDetectedSdk baseSdk = new PyDetectedSdk(baseSdkPath);
      final String virtualEnvPath = project.getBasePath() + "/.idea/VirtualEnvironment";
      final Sdk sdk = PySdkExtKt.createSdkByGenerateTask(new Task.WithResult<String, ExecutionException>(project,
              "Creating Virtual Environment",
              false) {
        @Override
        protected String compute(@NotNull ProgressIndicator indicator) throws ExecutionException {
          indicator.setIndeterminate(true);
          final PyPackageManager packageManager = PyPackageManager.getInstance(baseSdk);
          return packageManager.createVirtualEnv(virtualEnvPath, false);
        }
      }, getAllSdks(), baseSdk, project.getBasePath());
      if (sdk == null) {
        LOG.warn("Failed to create virtual env in " + virtualEnvPath);
        return;
      }
      settings.setSdk(sdk);
      SdkConfigurationUtil.addSdk(sdk);
      PySdkExtKt.associateWithProject(sdk, project, false);
    }
  }

  private static String getBaseSdk(@NotNull final Course course) {
    LanguageLevel baseLevel = LanguageLevel.PYTHON36;
    final String version = course.getLanguageVersion();
    if (PyEduPluginConfigurator.PYTHON_2.equals(version)) {
      baseLevel = LanguageLevel.PYTHON27;
    }
    else if (PyEduPluginConfigurator.PYTHON_3.equals(version)) {
      baseLevel = LanguageLevel.PYTHON36;
    }
    else if (version != null) {
      baseLevel = LanguageLevel.fromPythonVersion(version);
    }
    final PythonSdkFlavor flavor = PythonSdkFlavor.getApplicableFlavors(false).get(0);
    String baseSdk = null;
    final Collection<String> baseSdks = flavor.suggestHomePaths();
    for (String sdk : baseSdks) {
      final String versionString = flavor.getVersionString(sdk);
      final String prefix = flavor.getName() + " ";
      if (versionString != null && versionString.startsWith(prefix)) {
        final LanguageLevel level = LanguageLevel.fromPythonVersion(versionString.substring(prefix.length()));
        if (level.isAtLeast(baseLevel)) {
          baseSdk = sdk;
          break;
        }
      }
    }
    return baseSdk != null ? baseSdk : baseSdks.iterator().next();
  }

  @NotNull
  @Override
  public PyNewProjectSettings getProjectSettings() {
    return mySettings;
  }

  @Nullable
  protected Sdk updateSdkIfNeeded(@NotNull Project project, @Nullable Sdk sdk) {
    return sdk;
  }

  @NotNull
  protected abstract List<Sdk> getAllSdks();
  @NotNull
  protected abstract ComboboxWithBrowseButton getInterpreterComboBox(@Nullable Sdk fakeSdk);

  @NotNull
  public static PyDirectoryProjectGenerator getInstance(@NotNull Course course, boolean isLocal) {
    if (PlatformUtils.isPyCharm() || PlatformUtils.isCLion()) {
      return new PyCharmPyDirectoryProjectGenerator(course, isLocal);
    } else {
      return new IDEAPyDirectoryProjectGenerator(course, isLocal);
    }
  }
}
