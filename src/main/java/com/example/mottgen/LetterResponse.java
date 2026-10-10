package com.example.mottgen;

import java.util.List;

public record LetterResponse(String letter, List<String> matches, List<String> gaps) {
}
