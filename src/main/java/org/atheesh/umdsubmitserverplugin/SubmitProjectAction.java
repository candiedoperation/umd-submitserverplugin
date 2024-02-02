package org.atheesh.umdsubmitserverplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SubmitProjectAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        /* Get the Current Project */
        Project currentProject = e.getProject();
        if (currentProject == null) e.getPresentation().setEnabled(false);
        else {
            /* Check if the .submit file Exists */
            boolean submitExists =
                Files.exists(Path.of(currentProject.getBasePath() + "/.submit"));

            /* Set Visibility Status */
            e.getPresentation().setEnabled(submitExists);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ProgressManager.getInstance().run(
            new  Task.Modal(e.getProject(), "Submitting Project", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    /* Set Indeterminate Progress Indicator */
                    progressIndicator.setIndeterminate(true);

                    try {
                        /* Initiate Submission Middleware */
                        new SubmitServerMiddleware(e.getProject(), progressIndicator)
                                .initiateSubmission();
                    } catch (SubmitProjectException ex) {
                        /*
                            Since we're using the ProgressManager,
                            invokeLater is required for EDT thread
                        */

                        SwingUtilities.invokeLater(() -> {
                            /* Show Error Dialog */
                            Messages.showErrorDialog(
                                    ex.getMessage(),
                                    "Project Submission Error"
                            );
                        });
                    }
                }
            }
        );
    }
}
