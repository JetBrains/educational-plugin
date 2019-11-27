package com.jetbrains.edu.learning;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.coursecreator.yaml.YamlInfoTaskDescriptionTabKt;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import com.jetbrains.edu.learning.yaml.YamlDeepLoader;
import com.jetbrains.edu.learning.yaml.YamlFormatSettings;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.COURSE;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;
import static com.jetbrains.edu.learning.yaml.YamlFormatSettings.disableYaml;

/**
 * Implementation of class which contains all the information
 * about study in context of current project
 */

@State(name = "StudySettings", storages = @Storage(value = "study_project.xml", roamingType = RoamingType.DISABLED))
public class StudyTaskManager implements PersistentStateComponent<Element>, DumbAware {
  public static final Topic<CourseSetListener> COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener.class);
  private static final Logger LOG = Logger.getInstance(StudyTaskManager.class);

  @Transient
  private Course myCourse;
  public int VERSION = EduVersions.XML_FORMAT_VERSION;

  @Transient @Nullable private final Project myProject;

  public StudyTaskManager(@Nullable Project project) {
    myProject = project;
  }

  public StudyTaskManager() {
    this(null);
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
    if (myProject != null) {
      myProject.getMessageBus().syncPublisher(COURSE_SET).courseSet(course);
    }
  }

  @Nullable
  @Transient
  public Course getCourse() {
    return myCourse;
  }

  public boolean hasFailedAnswerPlaceholders(@NotNull final TaskFile taskFile) {
    return taskFile.getAnswerPlaceholders().size() > 0 && taskFile.hasFailedPlaceholders();
  }

  @Nullable
  @Override
  public Element getState() {
    if (disableYaml(myCourse)) {
      return serialize();
    }
    return null;
  }

  @VisibleForTesting
  @NotNull
  public Element serialize() {
    Element el = new Element("taskManager");
    Element taskManagerElement = new Element(MAIN_ELEMENT);
    XmlSerializer.serializeInto(this, taskManagerElement);
    Element courseElement = serializeCourse();
    addChildWithName(taskManagerElement, COURSE, courseElement);
    el.addContent(taskManagerElement);
    return el;
  }

  private Element serializeCourse() {
    Class<? extends Course> courseClass = myCourse.getClass();
    String serializedName = courseClass.getSimpleName();
    final Element courseElement = new Element(serializedName);
    XmlSerializer.serializeInto(courseClass.cast(myCourse), courseElement);
    return courseElement;
  }

  @Override
  public void loadState(@NotNull Element state) {
    if (myProject != null && YamlFormatSettings.isEduYamlProject(myProject)) {
      return;
    }
    try {
      int version = getVersion(state);
      if (version == -1) {
        LOG.error("StudyTaskManager doesn't contain any version:\n" + state.getValue());
        return;
      }
      if (myProject != null) {
        switch (version) {
          case 1:
            state = convertToSecondVersion(myProject, state);
          case 2:
            state = convertToThirdVersion(myProject, state);
          case 3:
            state = convertToFourthVersion(myProject, state);
          case 4:
            state = convertToFifthVersion(myProject, state);
            updateTestHelper();
          case 5:
            state = convertToSixthVersion(myProject, state);
          case 6:
            state = convertToSeventhVersion(myProject, state);
          case 7:
            state = convertToEighthVersion(myProject, state);
          case 8:
            state = convertToNinthVersion(myProject, state);
          case 9:
            state = convertToTenthVersion(myProject, state);
          case 10:
            state = convertToEleventhVersion(myProject, state);
          case 11:
            state = convertTo12Version(myProject, state);
          case 12:
            state = convertTo13Version(myProject, state);
          case 13:
            state = convertTo14Version(myProject, state);
          case 14:
            state = convertTo15Version(myProject, state);
          case 15:
            state = convertTo16Version(myProject, state);
          // uncomment for future versions
          //case 16:
          //  state = convertTo17Version(myProject, state);
        }
      }
      myCourse = deserialize(state);
      VERSION = EduVersions.XML_FORMAT_VERSION;
      if (myCourse != null) {
        myCourse.init(null, null, true);
        if (!disableYaml(myCourse)) {
          createConfigFilesIfMissing();
        }
      }
    }
    catch (StudyUnrecognizedFormatException e) {
      LOG.error("Unexpected course format:\n", new XMLOutputter().outputString(state));
    }
  }

  private void createConfigFilesIfMissing() {
    if (myProject == null) {
      return;
    }
    VirtualFile courseDir = OpenApiExtKt.getCourseDir(myProject);
    VirtualFile courseConfig = courseDir.findChild(YamlFormatSettings.COURSE_CONFIG);
    if (courseConfig == null) {
      StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
        YamlFormatSynchronizer.saveAll(myProject);
        FileDocumentManager.getInstance().saveAllDocuments();
        YamlFormatSynchronizer.startSynchronization(myProject);
        if (myCourse.isStudy()) {
          createDescriptionFiles(myProject);
        }
        else {
          Notification notification = new Notification("Education: yaml info",
                                                       "New YAML Format for Educators",
                                                       "Modify course by editing <i>*.yaml</i> files",
                                                       NotificationType.INFORMATION);
          notification.addAction(new AnAction("Learn More") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
              YamlInfoTaskDescriptionTabKt.showYamlTab(myProject);
              notification.hideBalloon();
            }
          });

          notification.notify(myProject);
        }
      });
    }
  }

  private void createDescriptionFiles(@NotNull Project project) {
    List<Task> tasks = CourseExt.getAllTasks(myCourse);
    for (Task task : tasks) {
      VirtualFile taskDir = task.getTaskDir(project);
      if (taskDir == null) {
        LOG.warn("Cannot find directory for a task: " + task.getName());
        continue;
      }

      try {
        GeneratorUtils.createDescriptionFile(taskDir, task);
      }
      catch (IOException e) {
        LOG.warn("Cannot create task description file: " + e.getMessage());
      }
    }
  }

  private void updateTestHelper() {
    if (myProject == null) return;
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      final VirtualFile baseDir = OpenApiExtKt.getCourseDir(myProject);
      final VirtualFile testHelper = baseDir.findChild(EduNames.TEST_HELPER);
      if (testHelper != null) {
        EduUtils.deleteFile(testHelper);
      }
      final FileTemplate template =
        FileTemplateManager.getInstance(myProject).getInternalTemplate(FileUtil.getNameWithoutExtension(EduNames.TEST_HELPER));
      try {
        final PsiDirectory projectDir = PsiManager.getInstance(myProject).findDirectory(baseDir);
        if (projectDir != null) {
          FileTemplateUtil.createFromTemplate(template, EduNames.TEST_HELPER, null, projectDir);
        }
      }
      catch (Exception e) {
        LOG.warn("Failed to create new test helper");
      }
    }));
  }

  @VisibleForTesting
  public Course deserialize(Element state) throws StudyUnrecognizedFormatException {
    final Element taskManagerElement = state.getChild(MAIN_ELEMENT);
    if (taskManagerElement == null) {
      throw new StudyUnrecognizedFormatException();
    }
    XmlSerializer.deserializeInto(this, taskManagerElement);
    final Element xmlCourse = getChildWithName(taskManagerElement, COURSE);
    return deserializeCourse(xmlCourse);
  }

  private static Course deserializeCourse(Element xmlCourse) {
    for (Class<? extends Course> courseClass : COURSE_ELEMENT_TYPES) {
      final Element courseElement = xmlCourse.getChild(courseClass.getSimpleName());
      if (courseElement == null) {
        continue;
      }
      try {
        Course courseBean = courseClass.newInstance();
        XmlSerializer.deserializeInto(courseBean, courseElement);
        return courseBean;
      }
      catch (InstantiationException e) {
        LOG.error("Failed to deserialize course");
      }
      catch (IllegalAccessException e) {
        LOG.error("Failed to deserialize course");
      }
    }
    return null;
  }

  public static StudyTaskManager getInstance(@NotNull final Project project) {
    StudyTaskManager manager = ServiceManager.getService(project, StudyTaskManager.class);
    if (!project.isDefault() && manager != null && manager.getCourse() == null && YamlFormatSettings.isEduYamlProject(project)) {
      Course course = ApplicationManager.getApplication().runReadAction((Computable<Course>)() -> YamlDeepLoader.loadCourse(project));
      manager.setCourse(course);
      YamlFormatSynchronizer.startSynchronization(project);
    }
    return manager;
  }
}
