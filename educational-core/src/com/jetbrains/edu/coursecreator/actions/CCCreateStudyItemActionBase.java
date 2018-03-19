package com.jetbrains.edu.coursecreator.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialog;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.statistics.FeedbackSenderKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class CCCreateStudyItemActionBase<Item extends StudyItem> extends DumbAwareAction {
  protected static final Logger LOG = Logger.getInstance(CCCreateStudyItemActionBase.class);

  public CCCreateStudyItemActionBase(String text, String description, Icon icon) {
    super(text, description, icon);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    final VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (!isActionApplicable(project, selectedFiles)) return;

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return;

    VirtualFile itemFile = createItem(project, selectedFiles[0], course);
    if (itemFile != null) {
      ProjectView.getInstance(project).select(itemFile, itemFile, true);
    }
    askFeedback(course, project);
  }

  private static void askFeedback(@NotNull final Course course, @NotNull final  Project project) {
    if (FeedbackSenderKt.isFeedbackAsked()) {
      return;
    }
    final Ref<Integer> countTasks = new Ref<>(0);
    course.visitLessons((lesson) -> {
      countTasks.set(countTasks.get() + lesson.getTaskList().size());
      return true;
    });
    if (countTasks.get() == 5) {
      FeedbackSenderKt.showNotification(false, course, project);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    final Presentation presentation = event.getPresentation();
    presentation.setEnabledAndVisible(false);
    final Project project = event.getData(CommonDataKeys.PROJECT);
    final VirtualFile[] selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (!isActionApplicable(project, selectedFiles)) return;

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return;

    VirtualFile sourceDirectory = selectedFiles[0];
    if (!isAddedAsLast(sourceDirectory, project, course) && getThresholdItem(course, sourceDirectory) == null) return;
    if (CommonDataKeys.PSI_FILE.getData(event.getDataContext()) != null) return;
    presentation.setEnabledAndVisible(true);
  }

  private static boolean isActionApplicable(@Nullable Project project, @Nullable VirtualFile[] selectedFiles) {
    if (project == null || selectedFiles == null) return false;
    if (selectedFiles.length == 0 || selectedFiles.length > 1) return false;

    if (!EduUtils.isStudyProject(project) || !CCUtils.isCourseCreator(project)) return false;

    final VirtualFile selectedFile = selectedFiles[0];
    return selectedFile != null;
  }

  @Nullable
  protected VirtualFile getParentDir(@NotNull Project project, @NotNull Course course, @NotNull VirtualFile directory) {
    if (isAddedAsLast(directory, project, course)) {
      return directory;
    }
    return directory.getParent();
  }

  @Nullable
  public VirtualFile createItem(@NotNull final Project project, @NotNull final VirtualFile sourceDirectory,
                                @NotNull final Course course) {
    StudyItem parentItem = getParentItem(course, sourceDirectory);
    final Item item = getItem(sourceDirectory, project, course, parentItem);
    if (item == null) {
      LOG.info("Failed to create study item");
      return null;
    }
    final VirtualFile parentDir = getParentDir(project, course, sourceDirectory);
    if (parentDir == null) {
      LOG.info("Failed to get parent directory");
      return null;
    }
    CCUtils.updateHigherElements(parentDir.getChildren(), getStudyOrderable(item, course), item.getIndex() - 1, 1);
    addItem(course, item);
    sortSiblings(course, parentItem);
    return createItemDir(project, item, parentDir, course);
  }

  protected abstract void addItem(@NotNull final Course course, @NotNull final Item item);

  protected abstract Function<VirtualFile, ? extends StudyItem> getStudyOrderable(@NotNull final StudyItem item,
                                                                                  @NotNull Course course);

  protected abstract VirtualFile createItemDir(@NotNull final Project project, @NotNull final Item item,
                                               @NotNull final VirtualFile parentDirectory, @NotNull final Course course);

  @Nullable
  protected Item getItem(@NotNull final VirtualFile sourceDirectory,
                              @NotNull final Project project,
                              @NotNull final Course course,
                              @Nullable StudyItem parentItem) {

    String itemName;
    int itemIndex;
    if (isAddedAsLast(sourceDirectory, project, course)) {
      itemIndex = getSiblingsSize(course, parentItem) + 1;
      String suggestedName = getItemName() + itemIndex;
      itemName = showInputDialog(sourceDirectory, suggestedName);
    } else {
      StudyItem thresholdItem = getThresholdItem(course, sourceDirectory);
      if (thresholdItem == null) {
        return null;
      }
      final Pair<String, Integer> nameIndex = getItemNameIndex(thresholdItem, project, sourceDirectory);
      if (nameIndex == null) return null;
      itemName = nameIndex.first;
      itemIndex = nameIndex.second;
    }
    if (itemName == null) {
      return null;
    }
    return createAndInitItem(course, parentItem, itemName, itemIndex);
  }

  protected Pair<String, Integer> getItemNameIndex(@NotNull StudyItem thresholdItem, @NotNull Project project,
                                                @NotNull VirtualFile sourceDirectory) {
    final int index = thresholdItem.getIndex();
    CCCreateStudyItemDialog dialog = new CCCreateStudyItemDialog(project, getItemName(), thresholdItem.getName(),
                                                                 index, sourceDirectory.getParent());
    dialog.show();
    if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return null;
    }
    return Pair.create(dialog.getName(), index + dialog.getIndexDelta());
  }

  private String showInputDialog(@NotNull VirtualFile sourceDirectory, @Nullable String suggestedName) {
    return Messages.showInputDialog("Name:", getTitle(), null, suggestedName, new CCUtils.PathInputValidator(sourceDirectory));
  }

  protected abstract int getSiblingsSize(@NotNull final Course course, @Nullable final StudyItem parentItem);

  @NotNull
  protected String getTitle() {
    return "Create New " + StringUtil.toTitleCase(getItemName());
  }

  @Nullable
  protected abstract StudyItem getParentItem(@NotNull final Course course, @NotNull final VirtualFile directory);

  @Nullable
  protected abstract StudyItem getThresholdItem(@NotNull final Course course, @NotNull final VirtualFile sourceDirectory);

  protected abstract boolean isAddedAsLast(@NotNull final VirtualFile sourceDirectory,
                                           @NotNull final Project project,
                                           @NotNull final Course course);

  protected abstract void sortSiblings(@NotNull final Course course, @Nullable final StudyItem parentItem);

  protected abstract String getItemName();

  public abstract Item createAndInitItem(@NotNull final Course course,
                                         @Nullable final StudyItem parentItem,
                                         String name,
                                         int index);
}
