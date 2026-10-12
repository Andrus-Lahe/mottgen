package com.example.mottgen;

import java.util.List;

public record LetterResponse(String letter, List<String> matches, List<String> gaps) {
}

// LetterResponse on record, mis kirjeldab vastust, mille LetterController saadab POST /api/letter päringule tagasi JSON-ina.
// väljad on kindlad:
// public record LetterResponse(String letter, List<String> matches, List<String> gaps, LetterStats stats)
//  - letter on valmis motivatsioonikiri. Leht näitab seda kirja aknas.
//  - matches on kuulutuse nõuded, mille kohta CV-s on tõend, kujul "nõue – tõend CV-st".
//   - gaps on nõuded, mille kohta CV-s tõendit pole. Kirjas neid kandidaadi omaks ei väideta.
//   - stats on LetterStats ehk selle LLM-kõne mõõdikud: tokenid, kestus, kulud, mudel ja finishReason. Leht kuvab need kirja akna all ühe reana.
//
// LetterService.createLetter koostab selle mudeli vastusest (MotivationLetter). Kui mudel jätab matches või gaps tühjaks (null),
// asendab teenus need tühja listiga, et leht ei peaks null-i käsitlema. matches ja gaps täidavad
//  <details> lõigu "Kuidas AI sellise kirja koostas". Seda näidatakse ainult siis, kui vähemalt üks neist pole tühi.