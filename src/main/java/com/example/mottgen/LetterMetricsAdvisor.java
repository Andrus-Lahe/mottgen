package com.example.mottgen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

import java.math.BigDecimal;

/** Logib iga LLM-kõne tokenid, hinna, latentsuse ja finishReason'i. */
@Slf4j
public class LetterMetricsAdvisor implements CallAdvisor {

	private static final BigDecimal INPUT_TOKEN_COST = new BigDecimal("0.30").divide(new BigDecimal(1_000_000));
	private static final BigDecimal OUTPUT_TOKEN_COST = new BigDecimal("2.50").divide(new BigDecimal(1_000_000));

	// Mähib LLM-kõne ümber: mõõdab kestuse ja loeb vastusest tokenite kasutuse.
	// Arvutab sisend- ja väljundtokenite hinna ning logib kõik ühe reana.
	// Vastust ei muuda, see antakse edasi samal kujul.
	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		long start = System.currentTimeMillis();

		ChatClientResponse response = chain.nextCall(request);

		ChatResponse chatResponse = response.chatResponse();
		Usage usage = chatResponse.getMetadata().getUsage();
		int in = usage.getPromptTokens() == null ? 0 : usage.getPromptTokens();
		int out = usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens();

		log.info("[LetterMetrics] llm_call in={} out={} latencyMs={} inputCost={} outputCost={} model={} finishReason={}",
				in, out, System.currentTimeMillis() - start,
				INPUT_TOKEN_COST.multiply(new BigDecimal(in)),
				OUTPUT_TOKEN_COST.multiply(new BigDecimal(out)),
				chatResponse.getMetadata().getModel(),
				chatResponse.getResult().getMetadata().getFinishReason());

		return response;
	}

	// Tagastab advisori nime, mille järgi see Spring AI ahelas tuvastatakse.
	@Override
	public String getName() {
		return "letter-metrics";
	}

	// Määrab advisori järjekorra ahelas. HIGHEST_PRECEDENCE tähendab, et see
	// käivitub esimesena ja mõõdab seega kogu ahela kestuse.
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
