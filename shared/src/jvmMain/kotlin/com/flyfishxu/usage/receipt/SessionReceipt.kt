package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.UsageLineItem
import com.flyfishxu.usage.model.UsageTotals
import java.time.Instant

data class SessionReceipt(
    val provider: Provider,
    val sessionId: String,
    val cwd: String?,
    val startedAt: Instant?,
    val endedAt: Instant,
    val turns: List<SessionReceiptTurn>,
    val totals: UsageTotals,
    val warnings: List<String>,
)

data class SessionReceiptTurn(
    val turnId: String?,
    val startedAt: Instant?,
    val endedAt: Instant,
    val items: List<UsageLineItem>,
    val totals: UsageTotals,
)
