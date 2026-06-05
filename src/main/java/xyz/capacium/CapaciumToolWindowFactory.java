package xyz.capacium;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CapaciumToolWindowFactory implements ToolWindowFactory {
    private static final String API_BASE = "https://api.capacium.xyz/v2";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel header = new JLabel("Capacium Marketplace", SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16));
        panel.add(header, BorderLayout.NORTH);
        
        JTextArea content = new JTextArea(
            "Search Capacium for AI agent capabilities.\n\n" +
            "Use Tools → Search Capacium Marketplace\n" +
            "or the search field below.\n\n" +
            "Installed capabilities will appear here."
        );
        content.setEditable(false);
        content.setWrapStyleWord(true);
        content.setLineWrap(true);
        panel.add(new JBScrollPane(content), BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Refresh Status");
        refreshBtn.addActionListener(e -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/stats"))
                    .GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                content.setText("Connected to Capacium Exchange\nTotal capabilities: " + 
                    response.body().split("\"total\"")[1].split(":")[1].split(",")[0].trim());
            } catch (Exception ex) {
                content.setText("Error: " + ex.getMessage());
            }
        });
        panel.add(refreshBtn, BorderLayout.SOUTH);
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content toolContent = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(toolContent);
    }
}
