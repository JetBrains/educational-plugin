package com.jetbrains.edu.kotlin;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.twitter.StudyTwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.io.InputStream;
import java.net.URL;

@SuppressWarnings("WeakerAccess")
public class KotlinTwitterDialogPanel extends StudyTwitterUtils.TwitterDialogPanel {
    private final JTextField myTwitterTextField;
    private final JLabel myRemainSymbolsLabel;
    private URL myImageUrl;
    private String imageName = "";

    public KotlinTwitterDialogPanel(@NotNull Task solvedTask) {
        setLayout(new VerticalFlowLayout());
        myRemainSymbolsLabel = new JLabel();
        myTwitterTextField = new JTextField();
        create(solvedTask);
    }

    public void create(@NotNull Task solvedTask) {
        myImageUrl = getMediaSourceForTask(solvedTask);
        addImageLabel();

        String messageForTask = getMessageForTask(solvedTask);
        myTwitterTextField.setText(messageForTask);
        add(myTwitterTextField);

        myRemainSymbolsLabel.setText(String.valueOf(140 - messageForTask.length()));
        add(myRemainSymbolsLabel);
    }

    private void addImageLabel() {
        if (myImageUrl != null) {
            Icon icon = new ImageIcon(myImageUrl);
            add(new JLabel(icon));
        }
    }

    @NotNull
    @Override
    public String getMessage() {
        return myTwitterTextField.getText();
    }

    private String getMessageForTask(@NotNull final Task task) {
        int solvedTaskNumber = KotlinUtils.calculateTaskNumber(task);
        return "Hey, I just completed level " + solvedTaskNumber / 8 
                +" of Kotlin Koans. https://kotlinlang.org/docs/tutorials/koans.html #kotlinkoans";
    }

    @Nullable
    @Override
    public InputStream getMediaSource() {
        return getClass().getResourceAsStream(imageName);
    }

    @Nullable
    private URL getMediaSourceForTask(@NotNull final Task task) {
        imageName = "/twitter/kotlin_koans/images/" + getImageName(task);
        return getClass().getResource(imageName);
    }
    
    private String getImageName(@NotNull final Task task) {
        int solvedTaskNumber = KotlinUtils.calculateTaskNumber(task);
        int level = solvedTaskNumber / 8;
        return level + "level.gif";
    }


    @Override
    public void addTextFieldVerifier(@NotNull DocumentListener documentListener) {
        myTwitterTextField.getDocument().addDocumentListener(documentListener);
    }

    @NotNull
    @Override
    public JLabel getRemainSymbolsLabel() {
        return myRemainSymbolsLabel;
    }
}
