package com.example.mottgen;

public record LetterRequest(String jobUrl, String cvText, String conversationId, Double temperature) {
}

// LetterRequest on record, mis kirjeldab sissetulevat päringut. Frontend (või Postman) saadab
// selle JSON-ina POST /api/letter lõpp-punktile ja Spring teisendab JSON-i automaatselt selleks objektiks.
//  - jobUrl on töökuulutuse link. JobPostingFetcher tõmbab selle lehe teksti.
//  - cvText on CV ja lisatekst. LetterService kontrollib, et see poleks tühi ega pikem kui 12 000 märki. Seejärel eemaldab PersonalDataRedactor sealt e-posti, telefoni ja isikukoodi.
//  - conversationId on valikuline vestluse ID, mille järgi MessageChatMemoryAdvisor leiab vestlusmälu. Leht hoiab seda sessionStorage'is. Kui seda pole, genereerib teenus uue UUID.
//  - temperature on valikuline loovuse väärtus. Teenus piirab selle vahemikku 0,5–2,0 ja kirjutab sellega üle application.properties vaikeväärtuse (0,6) ainult selle päringu jaoks. Kui seda pole, kasutatakse vaikeväärtust.
//
// Valikulisi välju (conversationId, temperature) võib puudu jääda. Siis loevad need null-ina ja teenus kasutab vaikeväärtusi.