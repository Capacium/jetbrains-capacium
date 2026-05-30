package xyz.capacium.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Tools > Capacium > Search Capabilities…
 *
 * Opens the Capacium tool window and focuses the Search tab.
 */
class SearchCapabilitiesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tw = ToolWindowManager.getInstance(project).getToolWindow("Capacium") ?: return
        tw.show {
            // Focus the first content (Search tab)
            tw.contentManager.setSelectedContent(
                tw.contentManager.getContent(0) ?: return@show
            )
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
