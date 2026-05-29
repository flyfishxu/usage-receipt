package com.flyfishxu.usage.hooks

import com.flyfishxu.usage.model.Provider
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HookInstallerTest {
    @Test
    fun installsCodexHookWithoutDroppingExistingHooksAndCreatesBackup() {
        val dir = createTempDirectory("usage-receipt-hooks").toFile()
        val codex = dir.resolve("codex-hooks.json")
        val claude = dir.resolve("claude-settings.json")
        val script = dir.resolve("bin/usagereceipt-hook")
        codex.writeText(
            """
            {
              "hooks": {
                "Stop": [
                  {
                    "hooks": [
                      {
                        "type": "command",
                        "command": "/tmp/existing-hook",
                        "timeout": 10
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        val installer = HookInstaller(codexHooksFile = codex, claudeSettingsFile = claude, hookScriptFile = script)
        val result = installer.install(Provider.OPENAI_CODEX)

        assertTrue(result.isSuccess)
        assertTrue(script.exists())
        val written = codex.readText()
        assertContains(written, "/tmp/existing-hook")
        assertContains(written, "usagereceipt-hook")
        assertContains(written, "--provider codex")
        assertTrue(installer.status(Provider.OPENAI_CODEX).installed)
        assertEquals(1, dir.listFiles().orEmpty().count { it.name.startsWith("codex-hooks.json.usageReceipt.") })
    }

    @Test
    fun uninstallRemovesOnlyUsageReceiptProviderHook() {
        val dir = createTempDirectory("usage-receipt-hooks").toFile()
        val codex = dir.resolve("codex-hooks.json")
        val claude = dir.resolve("claude-settings.json")
        val script = dir.resolve("bin/usagereceipt-hook")
        val installer = HookInstaller(codexHooksFile = codex, claudeSettingsFile = claude, hookScriptFile = script)

        assertTrue(installer.install(Provider.OPENAI_CODEX).isSuccess)
        assertTrue(installer.uninstall(Provider.OPENAI_CODEX).isSuccess)

        assertFalse(installer.status(Provider.OPENAI_CODEX).installed)
        assertFalse("--provider codex" in codex.readText())
    }
}
