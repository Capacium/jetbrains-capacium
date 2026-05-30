package xyz.capacium.plugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Tool window factory — registers the Capacium sidebar.
 *
 * Creates two tabs:
 *   1. Search    — live search of Capacium Exchange
 *   2. Installed — list of installed capabilities (from ~/.capacium/registry.json)
 */
class CapaciumToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // ── Tab 1: Search ──────────────────────────────────────────────────
        val searchPanel = CapaciumSearchPanel(project)
        val searchContent = contentFactory.createContent(
            searchPanel.component,
            "Search",
            false,
        )
        toolWindow.contentManager.addContent(searchContent)

        // ── Tab 2: Installed ───────────────────────────────────────────────
        val installedPanel = CapaciumInstalledPanel(project)
        val installedContent = contentFactory.createContent(
            installedPanel.component,
            "Installed",
            false,
        )
        toolWindow.contentManager.addContent(installedContent)
    }

    override fun shouldBeAvailable(project: Project) = true
}
