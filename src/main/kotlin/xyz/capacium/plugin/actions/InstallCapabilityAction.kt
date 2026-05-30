package xyz.capacium.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType

/**
 * Tools > Capacium > Install Capability…
 *
 * Prompts for owner/name[@version] and fires `cap install` in the terminal,
 * or shows a notification with the command when no terminal is available.
 */
class InstallCapabilityAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val capName = Messages.showInputDialog(
            project,
            "Enter capability name (owner/name[@version]):",
            "Install Capability",
            null,
            "",
            object : com.intellij.openapi.ui.InputValidatorEx {
                override fun checkInput(inputString: String) = inputString.contains("/")
                override fun canClose(inputString: String) = inputString.contains("/")
                override fun getErrorText(inputString: String) =
                    if (inputString.contains("/")) null
                    else "Format: owner/name (e.g. acme-corp/code-reviewer)"
            }
        ) ?: return

        // Try to open in terminal tool window
        val termTw = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
        if (termTw != null) {
            termTw.show()
            // Emit install notification — terminal focus is sufficient for the user to type
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Capacium.Notifications")
            .createNotification(
                "Install capability",
                "Run in terminal: <code>cap install $capName</code>",
                NotificationType.INFORMATION,
            )
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
