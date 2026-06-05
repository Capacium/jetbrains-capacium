package xyz.capacium;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class SearchAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String query = Messages.showInputDialog(
            e.getProject(),
            "Enter search query:",
            "Capacium Marketplace Search",
            Messages.getQuestionIcon()
        );
        if (query != null && !query.isBlank()) {
            Messages.showInfoMessage(
                "Use `cap install` in terminal to install capabilities.\n\n" +
                "Search: " + query,
                "Capacium"
            );
        }
    }
}
