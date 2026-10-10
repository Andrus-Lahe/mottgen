package com.example.mottgen;

public record LetterRequest(String jobUrl, String cvText, String conversationId, Double temperature) {
}
