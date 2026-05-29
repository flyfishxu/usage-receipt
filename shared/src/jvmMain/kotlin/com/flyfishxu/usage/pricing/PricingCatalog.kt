package com.flyfishxu.usage.pricing

import com.flyfishxu.usage.model.ModelPricing
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.TokenUsage
import java.math.BigDecimal
import java.math.RoundingMode

class PricingCatalog(
    private val prices: List<ModelPricing> = DefaultPrices,
) {
    fun find(provider: Provider, model: String): ModelPricing? =
        prices.firstOrNull { it.provider == provider && it.matches(model) }

    fun calculateUsd(provider: Provider, model: String, usage: TokenUsage): BigDecimal? {
        val pricing = find(provider, model) ?: return null
        return calculateUsd(pricing, usage)
    }

    fun calculateUsd(pricing: ModelPricing, usage: TokenUsage): BigDecimal {
        val million = BigDecimal("1000000")
        val input = pricing.inputPerMTok.multiply(usage.inputTokens.toBigDecimal()).divide(million)
        val cached = (pricing.cachedInputPerMTok ?: pricing.inputPerMTok)
            .multiply(usage.cachedInputTokens.toBigDecimal())
            .divide(million)
        val cacheWrite = (pricing.cacheWritePerMTok ?: pricing.inputPerMTok)
            .multiply(usage.cacheCreationTokens.toBigDecimal())
            .divide(million)
        val output = pricing.outputPerMTok
            .multiply(usage.billableOutputTokens.toBigDecimal())
            .divide(million)
        return input.add(cached).add(cacheWrite).add(output).setScale(6, RoundingMode.HALF_UP)
    }

    companion object {
        private const val OPENAI_PRICING = "https://platform.openai.com/docs/pricing"
        private const val OPENAI_COMPARE = "https://developers.openai.com/api/docs/models/compare"
        private const val CODEX_RATE_CARD = "https://help.openai.com/articles/20001106-codex-rate-card"
        private const val ANTHROPIC_PRICING = "https://claude.com/pricing#api"

        val DefaultPrices = listOf(
            ModelPricing(
                provider = Provider.OPENAI_CODEX,
                modelPattern = Regex("(?i)^codex-mini-latest$"),
                modelLabel = "codex-mini-latest",
                inputPerMTok = BigDecimal("1.50"),
                cachedInputPerMTok = BigDecimal("0.375"),
                cacheWritePerMTok = null,
                outputPerMTok = BigDecimal("6.00"),
                sourceUrl = OPENAI_PRICING,
                effectiveDate = "2026-05-29",
            ),
            ModelPricing(
                provider = Provider.OPENAI_CODEX,
                modelPattern = Regex("(?i)^gpt-5\\.5.*"),
                modelLabel = "GPT-5.5",
                inputPerMTok = BigDecimal("5.00"),
                cachedInputPerMTok = BigDecimal("0.50"),
                cacheWritePerMTok = null,
                outputPerMTok = BigDecimal("30.00"),
                sourceUrl = OPENAI_COMPARE,
                effectiveDate = "2026-05-29",
            ),
            ModelPricing(
                provider = Provider.OPENAI_CODEX,
                modelPattern = Regex("(?i)^gpt-5\\.4.*"),
                modelLabel = "GPT-5.4",
                inputPerMTok = BigDecimal("2.50"),
                cachedInputPerMTok = BigDecimal("0.25"),
                cacheWritePerMTok = null,
                outputPerMTok = BigDecimal("15.00"),
                sourceUrl = OPENAI_COMPARE,
                effectiveDate = "2026-05-29",
            ),
            ModelPricing(
                provider = Provider.OPENAI_CODEX,
                modelPattern = Regex("(?i)^gpt-5\\.3-codex.*"),
                modelLabel = "GPT-5.3-Codex",
                inputPerMTok = BigDecimal("2.50"),
                cachedInputPerMTok = BigDecimal("0.25"),
                cacheWritePerMTok = null,
                outputPerMTok = BigDecimal("15.00"),
                sourceUrl = CODEX_RATE_CARD,
                effectiveDate = "2026-05-29",
            ),
            ModelPricing(
                provider = Provider.ANTHROPIC_CLAUDE_CODE,
                modelPattern = Regex("(?i)^claude-opus-4(?:[.-].*)?$|^opus-4(?:[.-].*)?$|^claude-opus-4\\.5.*|^opus-4\\.5.*"),
                modelLabel = "Claude Opus 4.x",
                inputPerMTok = BigDecimal("5.00"),
                cachedInputPerMTok = BigDecimal("0.50"),
                cacheWritePerMTok = BigDecimal("6.25"),
                outputPerMTok = BigDecimal("25.00"),
                sourceUrl = ANTHROPIC_PRICING,
                effectiveDate = "2026-05-29",
            ),
            ModelPricing(
                provider = Provider.ANTHROPIC_CLAUDE_CODE,
                modelPattern = Regex("(?i)^claude-sonnet-4(?:[.-].*)?$|^sonnet-4(?:[.-].*)?$|^claude-sonnet-4\\.5.*|^sonnet-4\\.5.*"),
                modelLabel = "Claude Sonnet 4.x",
                inputPerMTok = BigDecimal("3.00"),
                cachedInputPerMTok = BigDecimal("0.30"),
                cacheWritePerMTok = BigDecimal("3.75"),
                outputPerMTok = BigDecimal("15.00"),
                sourceUrl = ANTHROPIC_PRICING,
                effectiveDate = "2026-05-29",
            ),
            ModelPricing(
                provider = Provider.ANTHROPIC_CLAUDE_CODE,
                modelPattern = Regex("(?i)^claude-haiku-4\\.5.*|^haiku-4\\.5.*|^claude-haiku-3\\.5.*|^haiku-3\\.5.*"),
                modelLabel = "Claude Haiku",
                inputPerMTok = BigDecimal("1.00"),
                cachedInputPerMTok = BigDecimal("0.10"),
                cacheWritePerMTok = BigDecimal("1.25"),
                outputPerMTok = BigDecimal("5.00"),
                sourceUrl = ANTHROPIC_PRICING,
                effectiveDate = "2026-05-29",
            ),
        )
    }
}
