package xyz.capacium.plugin.validator

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

/**
 * Capacium capability.yaml inline inspection.
 *
 * Runs on every capability.yaml save and flags:
 *   - Missing required fields (ERROR)
 *   - Invalid semver (ERROR)
 *   - Invalid name format (ERROR)
 *   - Missing kind-specific meta blocks (WARNING for recommended, ERROR for required)
 *   - Description too long (WARNING)
 *
 * Registered via capacium-yaml.xml (only active when YAML plugin present).
 */
class CapabilityYamlInspection : LocalInspectionTool() {

    override fun getDisplayName() = "capability.yaml v1.0 validation"
    override fun getGroupDisplayName() = "Capacium"
    override fun getShortName() = "CapaciumCapabilityYaml"
    override fun isEnabledByDefault() = true

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file.name != "capability.yaml" && file.name != "capability.yml") return
                val text = file.text
                val issues = validateYamlText(text)
                for (issue in issues) {
                    // Register at file level (line-level positions require YAML PSI traversal)
                    holder.registerProblem(
                        file,
                        issue.message,
                        issue.severity,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Validation logic (mirrors Python + TypeScript validators)
// ---------------------------------------------------------------------------

data class ValidationIssue(
    val message: String,
    val severity: com.intellij.codeInspection.ProblemHighlightType,
)

private val SEMVER_RE = Regex(
    """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([\w.-]+))?(?:\+([\w.-]+))?$"""
)
private val NAME_RE = Regex("""^[a-zA-Z0-9][a-zA-Z0-9._-]*/[a-z0-9][a-z0-9-]*$""")
private val VALID_KINDS = setOf(
    "skill", "mcp-server", "bundle", "tool", "prompt",
    "template", "workflow", "connector-pack",
    "operator", "checkpoint", "policy",
)

fun validateYamlText(text: String): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    val fields = parseTopLevelKeys(text)

    val ERROR = com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
    val WARN  = com.intellij.codeInspection.ProblemHighlightType.WARNING

    // Required fields
    for (f in listOf("name", "version", "kind", "description")) {
        if (fields[f].isNullOrBlank()) {
            issues += ValidationIssue("Required field missing or empty: '$f'", ERROR)
        }
    }

    val name    = fields["name"] ?: ""
    val version = fields["version"] ?: ""
    val kind    = fields["kind"] ?: ""
    val desc    = fields["description"] ?: ""

    if (name.isNotBlank() && !NAME_RE.matches(name)) {
        issues += ValidationIssue(
            "name '$name' must match 'owner/capability-name' (lowercase + hyphens)", ERROR
        )
    }

    if (version.isNotBlank()) {
        if (!SEMVER_RE.matches(version)) {
            issues += ValidationIssue(
                "version '$version' is not valid semver (use MAJOR.MINOR.PATCH)", ERROR
            )
        } else if (version == "0.0.0") {
            issues += ValidationIssue("version '0.0.0' — update to a real release version", WARN)
        }
    }

    if (kind.isNotBlank() && kind !in VALID_KINDS) {
        issues += ValidationIssue(
            "kind '$kind' is not in the v1.0 spec kind list", WARN
        )
    }

    if (kind == "mcp-server" && "mcp_meta" !in fields) {
        issues += ValidationIssue(
            "kind 'mcp-server' is missing recommended 'mcp_meta' block (transport, tools)", WARN
        )
    }
    if (kind == "bundle" && "bundle_meta" !in fields) {
        issues += ValidationIssue(
            "kind 'bundle' is missing recommended 'bundle_meta' block (capabilities)", WARN
        )
    }
    if (kind == "operator" && "operator_meta" !in fields) {
        issues += ValidationIssue(
            "kind 'operator' requires 'operator_meta' block (role, sla_hours, approval_modes)", ERROR
        )
    }
    if (kind == "checkpoint" && "checkpoint_meta" !in fields) {
        issues += ValidationIssue(
            "kind 'checkpoint' requires 'checkpoint_meta' block (fallback)", ERROR
        )
    }
    if (kind == "policy" && "policy_meta" !in fields) {
        issues += ValidationIssue(
            "kind 'policy' requires 'policy_meta' block (minimum_trust_state)", ERROR
        )
    }

    if (desc.length > 200) {
        issues += ValidationIssue(
            "description is ${desc.length} chars (recommended ≤ 200) — use 'long_description' for extended text", WARN
        )
    }

    return issues
}

/** Extract top-level key: value pairs from YAML text (no full parse needed). */
private fun parseTopLevelKeys(text: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (line in text.lines()) {
        val m = Regex("""^([a-zA-Z_][a-zA-Z0-9_]*):\s*(.*)$""").find(line) ?: continue
        val key = m.groupValues[1]
        val value = m.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
        if (key !in result) result[key] = value
    }
    return result
}
