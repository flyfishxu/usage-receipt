package com.flyfishxu.usage.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.ui.components.CommandButton
import com.flyfishxu.usage.ui.components.EmptyState
import com.flyfishxu.usage.ui.components.MetricPill
import com.flyfishxu.usage.ui.components.Panel
import com.flyfishxu.usage.ui.components.SmallCommandButton
import com.flyfishxu.usage.ui.components.StatusDot
import com.flyfishxu.usage.ui.theme.GeekTheme
import com.flyfishxu.usage.usage.SessionSummary
import com.flyfishxu.usage.usage.TurnSummary
import com.flyfishxu.usage.usage.totalTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionExplorerPanel(
    sessions: List<SessionSummary>,
    selected: SessionSummary?,
    onSelectSession: (SessionSummary) -> Unit,
    onRefresh: () -> Unit,
    onPrintSession: (SessionSummary) -> Unit,
    onPreviewSession: (SessionSummary) -> Unit,
    onPrintTurn: (ConversationUsage) -> Unit,
    onPreviewTurn: (ConversationUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(title = "Sessions", modifier = Modifier.width(360.dp).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandButton(text = "REFRESH", width = 118.dp, onClick = onRefresh)
                Text(
                    "${sessions.size} sessions",
                    color = GeekTheme.textMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
            if (sessions.isEmpty()) {
                EmptyState(
                    title = "WAITING FOR HOOK DATA",
                    subtitle = "~/.usageReceipt/sessions.jsonl",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions, key = { "${it.provider.hookId}:${it.sessionId}" }) { session ->
                        SessionRow(
                            session = session,
                            active = session.key == selected?.key,
                            onClick = { onSelectSession(session) },
                        )
                    }
                }
            }
        }

        Panel(title = "Turns", modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selected == null) {
                EmptyState(
                    title = "NO SESSION SELECTED",
                    subtitle = "Select a session to inspect turns",
                    modifier = Modifier.weight(1f),
                )
            } else {
                SessionToolbar(
                    session = selected,
                    onPrintSession = { onPrintSession(selected) },
                    onPreviewSession = { onPreviewSession(selected) },
                )
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selected.turns, key = { it.usage.id + it.endedAt.toString() }) { turn ->
                        TurnRow(
                            turn = turn,
                            onPrintTurn = { onPrintTurn(turn.usage) },
                            onPreviewTurn = { onPreviewTurn(turn.usage) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionToolbar(
    session: SessionSummary,
    onPrintSession: () -> Unit,
    onPreviewSession: () -> Unit,
) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(session.provider.displayName.uppercase(), color = GeekTheme.textPrimary, fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallCommandButton(text = "PREVIEW SESSION", width = 154.dp, onClick = onPreviewSession)
            SmallCommandButton(text = "PRINT SESSION", width = 144.dp, primary = true, onClick = onPrintSession)
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionSummary,
    active: Boolean,
    onClick: () -> Unit,
) {
    val providerColor = session.provider.providerColor()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) GeekTheme.activeSurface else GeekTheme.surfaceInset)
            .border(1.dp, if (active) providerColor else GeekTheme.border)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDot(providerColor)
                    Column {
                        Text(session.provider.displayName, color = GeekTheme.textPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "SESSION ${session.sessionId.takeLast(12)}",
                            color = GeekTheme.textMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
                Text(session.totals.usdCost.usdText(), color = providerColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("TURNS", session.turns.size.toString(), modifier = Modifier.weight(1f))
                MetricPill("TOK", session.totals.totalTokens().toString(), modifier = Modifier.weight(1.15f))
                MetricPill("LAST", shortTime(session.endedAt), modifier = Modifier.weight(1.25f))
            }
        }
    }
}

@Composable
private fun TurnRow(
    turn: TurnSummary,
    onPrintTurn: () -> Unit,
    onPreviewTurn: () -> Unit,
) {
    val providerColor = turn.provider.providerColor()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GeekTheme.surfaceInset)
            .border(1.dp, GeekTheme.border)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDot(providerColor)
                    Column {
                        Text(TimeFormatter.short(turn.endedAt), color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(
                            "TURN ${turn.turnId?.takeLast(12) ?: "unknown"} / ${turn.usage.items.joinToString(", ") { it.model }}",
                            color = GeekTheme.textMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("TOK", turn.totals.totalTokens().toString(), modifier = Modifier.weight(1f))
                MetricPill("USD", turn.totals.usdCost.usdText(), modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement  = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallCommandButton(text = "PREVIEW", width = 82.dp, onClick = onPreviewTurn)
                SmallCommandButton(text = "PRINT", width = 68.dp, primary = true, onClick = onPrintTurn)
            }
        }
    }
}

private val SessionSummary.key: String
    get() = "${provider.hookId}:$sessionId"

private fun Provider.providerColor() =
    when (this) {
        Provider.OPENAI_CODEX -> GeekTheme.accent
        Provider.ANTHROPIC_CLAUDE_CODE -> GeekTheme.warning
    }

private object TimeFormatter {
    private val shortFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun short(instant: Instant): String = shortFormatter.format(instant)
}

private fun shortTime(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun BigDecimal?.usdText(): String =
    this?.setScale(6, RoundingMode.HALF_UP)?.stripTrailingZeros()?.toPlainString()?.let { "$$it" } ?: "N/A"
