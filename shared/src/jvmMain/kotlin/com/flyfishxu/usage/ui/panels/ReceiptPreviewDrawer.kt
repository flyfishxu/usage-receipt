package com.flyfishxu.usage.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.ReceiptWidth
import com.flyfishxu.usage.receipt.ReceiptRenderer
import com.flyfishxu.usage.receipt.toSessionReceipt
import com.flyfishxu.usage.ui.components.CommandButton
import com.flyfishxu.usage.ui.components.Panel
import com.flyfishxu.usage.ui.theme.GeekTheme
import com.flyfishxu.usage.usage.SessionSummary

sealed interface ReceiptPreviewTarget {
    val title: String
    val subtitle: String
    fun text(renderer: ReceiptRenderer, width: ReceiptWidth): String
}

data class TurnReceiptPreviewTarget(
    val usage: ConversationUsage,
) : ReceiptPreviewTarget {
    override val title: String = "Turn Preview"
    override val subtitle: String = usage.turnId?.takeLast(16) ?: "unknown turn"

    override fun text(renderer: ReceiptRenderer, width: ReceiptWidth): String =
        renderer.previewText(usage, width)
}

data class SessionReceiptPreviewTarget(
    val session: SessionSummary,
) : ReceiptPreviewTarget {
    override val title: String = "Session Preview"
    override val subtitle: String = "${session.turns.size} turn(s) / ${session.sessionId.takeLast(16)}"

    override fun text(renderer: ReceiptRenderer, width: ReceiptWidth): String =
        renderer.previewSessionText(session.toSessionReceipt(), width)
}

@Composable
fun ReceiptPreviewModal(
    target: ReceiptPreviewTarget?,
    width: ReceiptWidth,
    renderer: ReceiptRenderer,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedTarget by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (target != null) displayedTarget = target
    }

    AnimatedVisibility(
        visible = target != null,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 3 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 3 }),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(onClick = onClose),
            )
            displayedTarget?.let { currentTarget ->
                ReceiptPreviewDrawer(
                    target = currentTarget,
                    width = width,
                    renderer = renderer,
                    onClose = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ReceiptPreviewDrawer(
    target: ReceiptPreviewTarget,
    width: ReceiptWidth,
    renderer: ReceiptRenderer,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paperWidth = width.previewPaperWidth()
    Panel(title = "Receipt Preview", modifier = modifier.width(paperWidth + 64.dp).fillMaxHeight()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(target.title.uppercase(), color = GeekTheme.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(target.subtitle, color = GeekTheme.textMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            CommandButton(text = "CLOSE", width = 86.dp, onClick = onClose)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(GeekTheme.surfaceInset)
                .padding(14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(paperWidth)
                    .background(GeekTheme.receiptPaper)
                    .border(1.dp, GeekTheme.borderBright)
                    .padding(14.dp),
            ) {
                Text(
                    text = target.text(renderer, width),
                    color = GeekTheme.receiptInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

private fun ReceiptWidth.previewPaperWidth(): Dp =
    printWidthPixels.dp
