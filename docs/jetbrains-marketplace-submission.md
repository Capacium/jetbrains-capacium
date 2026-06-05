# JetBrains Marketplace Submission Guide

## Pre-Submission Checklist

- [ ] Plugin builds: `gradle buildPlugin`
- [ ] Plugin verified locally (Install Plugin from Disk...)
- [ ] build.gradle.kts has valid sinceBuild/untilBuild
- [ ] plugin.xml has complete metadata
- [ ] README.md is current

## Submission Steps

1. **Register on JetBrains Marketplace**: https://plugins.jetbrains.com/author/me
2. **Generate upload token**: My Profile → Authentication Tokens
3. **Set environment variable**: `export PUBLISH_TOKEN=perm:...`
4. **Publish**:
   ```bash
   ./gradlew publishPlugin
   ```
5. **Verify**: Check https://plugins.jetbrains.com/plugin/... for listing status

## Plugin Metadata

| Field | Value |
|-------|-------|
| Name | Capacium |
| ID | xyz.capacium.jetbrains-capacium |
| Category | Tools Integration |
| Price | Free |
| License | MIT |
| Vendor | Capacium (hello@capacium.xyz) |
| Website | https://capacium.xyz |

## Review Criteria

JetBrains reviews plugins for:
- **No malware / harmful code**: All source is public at github.com/Capacium/jetbrains-capacium
- **Privacy**: No telemetry, no data collection. Only queries api.capacium.xyz on user action.
- **Stability**: Single tool window + one action. Minimal surface area.
- **Description accuracy**: plugin-description.html matches plugin.xml description.

Expected review time: 1-3 business days.
