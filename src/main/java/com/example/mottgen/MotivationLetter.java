package com.example.mottgen;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** AI struktureeritud vastus. "reasoning" tuleb enne kirja, et mudel mõtleks enne kirjutamist. */
@JsonPropertyOrder({"reasoning", "letter"})
public record MotivationLetter(String reasoning, String letter) {
}
