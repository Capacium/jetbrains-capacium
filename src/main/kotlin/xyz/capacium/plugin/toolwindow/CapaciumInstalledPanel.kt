package xyz.capacium.plugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.SimpleTree
import java.awt.BorderLayout
import java.awt.Font
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Installed capabilities panel — reads ~/.capacium/registry.json
 * and displays capabilities grouped by kind in a tree.
 */
class CapaciumInstalledPanel(private val project: Project) {

    val component: JComponent = buildPanel()

    private fun buildPanel(): JPanel {
        val panel = JPanel(BorderLayout(4, 4))

        val root = DefaultMutableTreeNode("Installed Capabilities")
        val treeModel = DefaultTreeModel(root)
        val tree = SimpleTree(treeModel)
        tree.isRootVisible = true

        val refreshBtn = JButton("⟳ Refresh")
        refreshBtn.addActionListener { reload(root, treeModel) }

        reload(root, treeModel)

        panel.add(JBLabel("Installed (from ~/.capacium/registry.json)").apply {
            font = font.deriveFont(Font.PLAIN, 11f)
            border = BorderFactory.createEmptyBorder(4, 6, 2, 0)
        }, BorderLayout.NORTH)
        panel.add(JBScrollPane(tree), BorderLayout.CENTER)
        panel.add(refreshBtn, BorderLayout.SOUTH)

        return panel
    }

    private fun reload(root: DefaultMutableTreeNode, model: DefaultTreeModel) {
        root.removeAllChildren()

        val caps = loadInstalled()
        if (caps.isEmpty()) {
            root.add(DefaultMutableTreeNode("No capabilities installed"))
            model.reload()
            return
        }

        // Group by kind
        val byKind = caps.groupBy { it["kind"] as? String ?: "skill" }
        byKind.forEach { (kind, list) ->
            val icon = KIND_ICONS[kind] ?: "◻"
            val kindNode = DefaultMutableTreeNode("$icon $kind (${list.size})")
            list.forEach { cap ->
                val name = "${cap["owner"]}/${cap["name"]}"
                val ver = cap["version"] ?: "?"
                kindNode.add(DefaultMutableTreeNode("$name  v$ver"))
            }
            root.add(kindNode)
        }
        model.reload()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadInstalled(): List<Map<String, String>> {
        val candidates = mutableListOf<File>()

        // Workspace-local
        project.basePath?.let { candidates.add(File(it, ".capacium/registry.json")) }

        // Global
        candidates.add(File(System.getProperty("user.home"), ".capacium/registry.json"))

        for (f in candidates) {
            if (!f.exists()) continue
            return try {
                val raw = f.readText()
                // Simple JSON array parser (avoid adding Gson/Jackson dep)
                parseRegistryJson(raw)
            } catch (_: Exception) {
                emptyList()
            }
        }
        return emptyList()
    }

    /**
     * Minimal registry.json parser — handles the flat list format:
     * [{"owner":"x","name":"y","kind":"skill","version":"1.0.0"}, ...]
     * or {"capabilities":[...]}
     */
    private fun parseRegistryJson(raw: String): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        // Strip outer array/object brackets
        val content = raw.trim().removePrefix("{").removeSuffix("}")
            .let {
                val idx = it.indexOf("[")
                if (idx >= 0) it.substring(idx) else "[$it]"
            }
            .trim().removePrefix("[").removeSuffix("]")

        // Split on "},{"
        val objects = content.split(Regex("""}\s*,\s*\{"""))
        for (obj in objects) {
            val entry = mutableMapOf<String, String>()
            val kvPattern = Regex(""""(\w+)"\s*:\s*"([^"]*)"""")
            kvPattern.findAll(obj).forEach { m ->
                entry[m.groupValues[1]] = m.groupValues[2]
            }
            if (entry.isNotEmpty()) results.add(entry)
        }
        return results
    }
}
