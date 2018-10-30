package com.jetbrains.edu.coursecreator.actions;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.io.ZipUtil;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.mixins.*;
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import kotlin.collections.ArraysKt;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static com.jetbrains.edu.learning.EduNames.COURSE_META_FILE;

@SuppressWarnings("ComponentNotRegistered") //educational-core.xml
public class CCCreateCourseArchive extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(CCCreateCourseArchive.class.getName());

  public static final String GENERATE_COURSE_ARCHIVE = "&Generate Course Archive";
  public static final String LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation";

  private String myZipName;
  private String myLocationDir;

  public void setZipName(String zipName) {
    myZipName = zipName;
  }

  public void setLocationDir(String locationDir) {
    myLocationDir = locationDir;
  }

  public CCCreateCourseArchive() {
    super(GENERATE_COURSE_ARCHIVE, GENERATE_COURSE_ARCHIVE, null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(project != null && CCUtils.isCourseCreator(project));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) return;
    Module module = ModuleUtilCore.findModuleForFile(baseDir, project);
    if (module == null) return;

    CCCreateCourseArchiveDialog dlg = new CCCreateCourseArchiveDialog(project, this);
    dlg.show();
    if (dlg.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return;
    }
    boolean isSuccessful = createCourseArchive(project, module, myZipName, myLocationDir, true);
    if (isSuccessful) {
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, myLocationDir);
      EduUsagesCollector.createdCourseArchive();
    } else {
      Messages.showErrorDialog("Can not create archive for current course", "Failed to Create Course Archive");
    }
  }

  /**
   * @return true if course archive was created successfully, false otherwise
   */
  public static boolean createCourseArchive(final Project project, Module module, String zipName, String locationDir, boolean showMessage) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return false;
    VirtualFile archiveFolder = CCUtils.generateFolder(project, module, zipName);
    if (archiveFolder == null) {
      return false;
    }
    FileDocumentManager.getInstance().saveAllDocuments();

    final Ref<Boolean> isCreationSuccessful = Ref.create();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        archiveFolder.refresh(false, true);
        Course courseCopy = course.copy();
        loadActualTexts(courseCopy);
        courseCopy.sortItems();
        createAdditionalFiles(courseCopy);
        try {
          generateJson(archiveFolder, courseCopy);
          VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
          packCourse(archiveFolder, locationDir, zipName, showMessage);
          synchronize(project);
          isCreationSuccessful.set(true);
        } catch (IOException e) {
          LOG.error("Failed to create course archive", e);
          isCreationSuccessful.set(false);
        }
      }

      private void createAdditionalFiles(Course course) {
        final Lesson lesson = CCUtils.createAdditionalLesson(course, project, EduNames.ADDITIONAL_MATERIALS);
        if (lesson != null) {
          course.addLesson(lesson);
        }
      }

      private void loadActualTexts(@NotNull Course courseCopy) {
        courseCopy.visitLessons((lesson) -> {
          final VirtualFile lessonDir = lesson.getLessonDir(project);
          if (lessonDir == null) return true;
          for (Task task : lesson.getTaskList()) {
            final VirtualFile taskDir = task.getTaskDir(project);
            if (taskDir == null) continue;
            convertToStudentTaskFiles(task, taskDir);
            CCUtils.loadTestTextsToTask(task, taskDir);
            CCUtils.loadAdditionalFileTextsToTask(task, taskDir);
            addDescriptions(task);
          }
          return true;
        });
      }

      private void convertToStudentTaskFiles(Task task, VirtualFile taskDir) {
        final LinkedHashMap<String, TaskFile> studentTaskFiles = new LinkedHashMap<>();
        for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
          VirtualFile answerFile = EduUtils.findTaskFileInDir(entry.getValue(), taskDir);
          if (answerFile == null) {
            continue;
          }
          final TaskFile studentFile = EduUtils.createStudentFile(project, answerFile, task);
          if (studentFile != null) {
            studentTaskFiles.put(entry.getKey(), studentFile);
          }
        }
        task.setTaskFiles(studentTaskFiles);
      }

      private void addDescriptions(@NotNull final Task task) {
        VirtualFile descriptionFile = TaskExt.getDescriptionFile(task, project);

        if (descriptionFile != null) {
          try {
            task.setDescriptionText(VfsUtilCore.loadText(descriptionFile));
            String extension = descriptionFile.getExtension();
            DescriptionFormat descriptionFormat =
              ArraysKt.firstOrNull(DescriptionFormat.values(), format -> format.getFileExtension().equals(extension));
            if (descriptionFormat != null) {
              task.setDescriptionFormat(descriptionFormat);
            }
          } catch (IOException e) {
            LOG.warn("Failed to load text " + descriptionFile.getName());
          }
        } else {
          LOG.warn(String.format("Can't find description file for task `%s`", task.getName()));
        }
      }
    });

    return isCreationSuccessful.get();
  }

  private static void synchronize(@NotNull final Project project) {
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
    ProjectView.getInstance(project).refresh();
  }

  private static void packCourse(@NotNull final VirtualFile baseDir, String locationDir,
                                    String zipName, boolean showMessage) throws IOException {
    final File zipFile = new File(locationDir, zipName + ".zip");
    VirtualFile[] courseFiles = baseDir.getChildren();
    try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
      for (VirtualFile file : courseFiles) {
        ZipUtil.addFileOrDirRecursively(zos, null, new File(file.getPath()), file.getName(), null, null);
      }
      if (showMessage) {
        ApplicationManager.getApplication().invokeLater(
          () -> Messages.showInfoMessage("Course archive was saved to " + zipFile.getPath(),
                                         "Course Archive Was Created Successfully"));

      }
    }
  }

  public static void generateJson(VirtualFile parentDir, Course course) throws IOException {
    ObjectMapper mapper = course.getId() == 0 ? getLocalCourseMapper() : getRemoteCourseMapper();
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

    mapper.writer(prettyPrinter).writeValue(new File(new File(parentDir.getPath()), COURSE_META_FILE), course);
  }

  @NotNull
  private static ObjectMapper getLocalCourseMapper() {
    JsonFactory factory = new JsonFactory();
    ObjectMapper mapper = new ObjectMapper(factory);
    mapper.addMixIn(EduCourse.class, LocalCourseMixin.class);
    mapper.addMixIn(Section.class, LocalSectionMixin.class);
    mapper.addMixIn(Lesson.class, LocalLessonMixin.class);
    mapper.addMixIn(Task.class, LocalTaskMixin.class);
    mapper.addMixIn(TaskFile.class, LocalTaskFileMixin.class);
    mapper.addMixIn(AdditionalFile.class, AdditionalFileMixin.class);
    mapper.addMixIn(FeedbackLink.class, FeedbackLinkMixin.class);
    mapper.enable(WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(READ_ENUMS_USING_TO_STRING);
    return mapper;
  }

  @NotNull
  private static ObjectMapper getRemoteCourseMapper() {
    JsonFactory factory = new JsonFactory();
    ObjectMapper mapper = new ObjectMapper(factory);
    mapper.addMixIn(EduCourse.class, LocalCourseMixin.class);
    mapper.addMixIn(Section.class, LocalSectionMixin.class);
    mapper.addMixIn(Lesson.class, LocalLessonMixin.class);
    mapper.addMixIn(Task.class, LocalTaskMixin.class);
    mapper.addMixIn(TaskFile.class, LocalTaskFileMixin.class);
    mapper.addMixIn(AdditionalFile.class, AdditionalFileMixin.class);
    mapper.addMixIn(FeedbackLink.class, FeedbackLinkMixin.class);
    mapper.enable(WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(READ_ENUMS_USING_TO_STRING);
    return mapper;
  }
}
