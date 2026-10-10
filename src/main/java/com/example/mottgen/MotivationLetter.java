package com.example.mottgen;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * AI struktureeritud vastus (Chain of Thought). Väljade järjekord on oluline: mudel analüüsib
 * enne kirjutamist samm-sammult nõuded, vasted ja puudujäägid ning kirjutab kirja alles lõpus.
 */
@JsonPropertyOrder({"requirements", "matches", "gaps", "letter"})
public record MotivationLetter(List<String> requirements, List<String> matches, List<String> gaps, String letter) {
}
