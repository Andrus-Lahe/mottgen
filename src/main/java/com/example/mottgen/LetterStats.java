package com.example.mottgen;

import java.math.BigDecimal;

// Ühe LLM-kõne mõõdikud, mida advisor logib ja leht kuvab ühe reana.
public record LetterStats(int inputTokens, int outputTokens, long latencyMs,
						  BigDecimal inputCost, BigDecimal outputCost,
						  String model, String finishReason) {
}
