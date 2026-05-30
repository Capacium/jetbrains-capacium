package xyz.capacium.plugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import xyz.capacium.plugin.validator.validateYamlText

/**
 * Validate the active capability.yaml and show results as a notification.
 *
 * Registered in:
 *  - Tools > Capacium > Validate capability.yaml
 *  - Editor right-click context menu (capability.yaml only)
 */
class ValidateCapabilityYamlAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)

        val text = editor?.document?.text
            ?: file?.text
            ?: run {
                notify(project, "Open a capability.yaml file first.", NotificationType.WARNING)
                return
            }

        val issues = validateYamlText(text)
        val errors   = issues.filter {
            it.severity == com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
        }
        val warnings = issues.filter {
            it.severity == com.intellij.codeInspection.ProblemHighlightType.WARNING
        }

        val fileName = file?.name ?: "capability.yaml"

        if (errors.isEmpty() && warnings.isEmpty()) {
            notify(project, "✅ $fileName is valid (capability.yaml v1.0)", NotificationType.INFORMATION)
            return
        }

        val sb = StringBuilder()
        if (errors.isNotEmpty()) {
            sb.appendLine("<b>${errors.size} error(s):</b>")
            errors.forEach { sb.appendLine("• ${it.message}") }
        }
        if (warnings.isNotEmpty()) {
            sb.appendLine("<b>${warnings.size} warning(s):</b>")
            warnings.forEach { sb.appendLine("• ${it.message}") }
        }

        val type = if (errors.isNotEmpty()) NotificationType.ERROR else NotificationType.WARNING
        notify(project, sb.toString().trim(), type)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val isCapYaml = file?.name?.let {
            it == "capability.yaml" || it == "capability.yml"
        } ?: false
        e.presentation.isEnabled = e.project != null && (isCapYaml || e.getData(CommonDataKeys.EDITOR) != null)
    }

    private fun notify(project: com.intellij.openapi.project.Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Capacium.Notifications")
            .createNotification("Capacium: capability.yaml", message, type)
            .notify(project)
    }
}
