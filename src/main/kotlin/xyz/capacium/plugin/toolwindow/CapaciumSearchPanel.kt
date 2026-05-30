package xyz.capacium.plugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import xyz.capacium.plugin.services.CapabilityListing
import xyz.capacium.plugin.services.ExchangeServiceImpl
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

private val KIND_ICONS = mapOf(
    "skill"          to "⚡",
    "mcp-server"     to "🔌",
    "bundle"         to "📦",
    "tool"           to "🔧",
    "prompt"         to "💬",
    "template"       to "📄",
    "workflow"       to "🔄",
    "connector-pack" to "🔗",
    "operator"       to "👤",
    "checkpoint"     to "🔒",
    "policy"         to "📋",
)

private val TRUST_COLORS = mapOf(
    "discovered" to Color(107, 114, 128),
    "audited"    to Color(59, 130, 246),
    "verified"   to Color(34, 197, 94),
    "signed"     to Color(245, 158, 11),
)

class CapaciumSearchPanel(private val project: Project) {

    val component: JComponent = buildPanel()

    private lateinit var queryField: JBTextField
    private lateinit var kindCombo: ComboBox<String>
    private lateinit var resultList: JBList<CapabilityListing>
    private lateinit var statusLabel: JBLabel
    private val listModel = DefaultListModel<CapabilityListing>()

    private fun buildPanel(): JPanel {
        val panel = JPanel(BorderLayout(4, 4))

        // ── Search bar ─────────────────────────────────────────────────────
        val topBar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        queryField = JBTextField(20)
        queryField.emptyText.text = "Search capabilities…"
        kindCombo = ComboBox(arrayOf(
            "All kinds", "skill", "mcp-server", "bundle", "tool",
            "prompt", "workflow", "operator", "checkpoint", "policy",
        ))
        val searchBtn = JButton("Search")
        topBar.add(queryField)
        topBar.add(kindCombo)
        topBar.add(searchBtn)

        // ── Status label ───────────────────────────────────────────────────
        statusLabel = JBLabel("Type to search Capacium Exchange")
        statusLabel.font = statusLabel.font.deriveFont(Font.PLAIN, 11f)
        statusLabel.foreground = JBColor.GRAY

        // ── Results list ───────────────────────────────────────────────────
        resultList = JBList(listModel)
        resultList.cellRenderer = CapabilityListCellRenderer()
        resultList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // Install on double-click
        resultList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val cap = resultList.selectedValue ?: return
                    installCapability(cap.canonicalName)
                }
            }
        })

        // ── Assemble ───────────────────────────────────────────────────────
        val north = JPanel(BorderLayout(2, 2))
        north.add(topBar, BorderLayout.NORTH)
        north.add(statusLabel, BorderLayout.SOUTH)

        panel.add(north, BorderLayout.NORTH)
        panel.add(JBScrollPane(resultList), BorderLayout.CENTER)

        // Install button below the list
        val installBtn = JButton("Install selected")
        installBtn.addActionListener {
            val cap = resultList.selectedValue ?: return@addActionListener
            installCapability(cap.canonicalName)
        }
        panel.add(installBtn, BorderLayout.SOUTH)

        // Search action
        val doSearch = Runnable {
            val q = queryField.text.trim()
            if (q.isBlank()) return@Runnable
            doSearch(q)
        }
        searchBtn.addActionListener { doSearch.run() }
        queryField.addActionListener { doSearch.run() }

        return panel
    }

    private fun doSearch(query: String) {
        statusLabel.text = "Searching…"
        listModel.clear()

        ApplicationManager.getApplication().executeOnPooledThread {
            val kind = kindCombo.selectedItem?.toString()?.takeIf { it != "All kinds" }
            val svc = ExchangeServiceImpl.getInstance()
            val results = svc.search(query, kind, 30)

            SwingUtilities.invokeLater {
                listModel.clear()
                results.results.forEach { listModel.addElement(it) }
                statusLabel.text = "${results.total} result${if (results.total != 1) "s" else ""}"
            }
        }
    }

    private fun installCapability(capName: String) {
        val terminal = com.intellij.openapi.wm.ToolWindowManager
            .getInstance(project)
            .getToolWindow("Terminal")
        if (terminal != null) {
            terminal.show()
        }
        // Fire cap install in the built-in terminal via ShellTerminalWidget or
        // fall back to a notification with the install command.
        val notification = com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Capacium.Notifications")
            .createNotification(
                "Install capability",
                "Run: <code>cap install $capName</code>",
                com.intellij.notification.NotificationType.INFORMATION,
            )
        notification.notify(project)
    }
}

// ---------------------------------------------------------------------------
// Cell renderer
// ---------------------------------------------------------------------------

private class CapabilityListCellRenderer : ListCellRenderer<CapabilityListing> {
    override fun getListCellRendererComponent(
        list: JList<out CapabilityListing>,
        value: CapabilityListing?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val cap = value ?: return JLabel()
        val icon = KIND_ICONS[cap.kind] ?: "◻"
        val trustColor = TRUST_COLORS[cap.trustState] ?: Color.GRAY
        val quality = cap.qualityScore?.let { "Q${it.toInt()}" } ?: ""
        val stars = if (cap.githubStars > 0) "★${cap.githubStars}" else ""

        val panel = JPanel(BorderLayout(4, 0))
        panel.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        if (isSelected) {
            panel.background = list.selectionBackground
        }

        val title = JBLabel("$icon ${cap.canonicalName}")
        title.font = title.font.deriveFont(Font.BOLD, 12f)

        val sub = JBLabel("${cap.shortDescription.take(70)}  $stars $quality  ${cap.trustState}")
        sub.font = sub.font.deriveFont(Font.PLAIN, 10f)
        sub.foreground = trustColor

        val textCol = JPanel(BorderLayout())
        textCol.isOpaque = false
        textCol.add(title, BorderLayout.NORTH)
        textCol.add(sub, BorderLayout.SOUTH)
        panel.add(textCol, BorderLayout.CENTER)

        return panel
    }
}
