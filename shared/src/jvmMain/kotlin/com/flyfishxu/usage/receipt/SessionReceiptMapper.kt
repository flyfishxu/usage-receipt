package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.usage.SessionSummary

fun SessionSummary.toSessionReceipt(): SessionReceipt =
    SessionReceipt(
        provider = provider,
        sessionId = sessionId,
        cwd = cwd,
        startedAt = startedAt,
        endedAt = endedAt,
        turns = turns.map { turn ->
            SessionReceiptTurn(
                turnId = turn.turnId,
                startedAt = turn.startedAt,
                endedAt = turn.endedAt,
                items = turn.usage.items,
                totals = turn.totals,
            )
        },
        totals = totals,
        warnings = warnings,
    )
