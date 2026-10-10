# Mottgen – motivatsioonikirja generaator

Spring Boot + Spring AI (Google Gemini). Kasutaja sisestab töökuulutuse lingi ja CV, AI koostab motivatsioonikirja.

## Käivitamine
```
export GOOGLE_API_KEY=...        # PowerShellis: $env:GOOGLE_API_KEY="..."
./gradlew bootRun                # http://localhost:8080
```

## Kasutatud tehnikad (nagu triage-app'is)
- **Grounding** – `JobPostingFetcher` tõmbab kuulutuse teksti (Jsoup) ja see antakse promptis mudelile; süsteemiprompt keelab fakte välja mõelda.
- **System prompt + few-shot näide** – `LetterService`.
- **Temperature / max tokens** – `application.properties`.
- **Structured output** – `MotivationLetter` (reasoning enne kirja), `validateSchema()`.
- **Advisorid** – `SimpleLoggerAdvisor`, `MessageChatMemoryAdvisor` (conversationId), oma `LetterMetricsAdvisor`.
- **Kaitse** – SSRF-kontroll lingil, pikkuspiirangud, prompt injection'i vastane juhis.
