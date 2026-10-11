# Mottgen – motivatsioonikirja generaator

Õppeprojekt. Kasutaja sisestab töökuulutuse lingi ja CV (+ lisatekst), AI (Google Gemini) koostab motivatsioonikirja.
Töötab päris kuulutusega (kontrollitud). Repo: https://github.com/Andrus-Lahe/mottgen (privaatne, `main`).

## Stack
- Java 21, Gradle (wrapper), Spring Boot 4.1.1, Spring AI 2.0.1 (`spring-ai-starter-model-google-genai`)
- Thymeleaf (`templates/index.html`, ühe lehe frontend), Jsoup (kuulutuse tõmbamine), Lombok
- Mudel: `gemini-3.5-flash-lite`, JSON-vastus (`response-mime-type=application/json`)
- Aluseks võetud kõrvalprojekt `IdeaProjects/main` (triage-app): sealt grounding, temperature jt mustrid

## Käivitamine
- Vajab keskkonnamuutujat `GOOGLE_API_KEY`. Võtit **ei panda** faili ega chatti. Ilma võtmeta rakendus kukub käivitusel kokku
  ("Incomplete Google GenAI configuration ... Vertex AI" – eksitav tekst, põhjus on lihtsalt tühi võti).
- IntelliJ: `MottgenApplication` run-konfiguratsioon, võti Environment variables väljal. Ava http://localhost:8080
- Terminalist: `./gradlew bootRun` (PowerShell: `$env:GOOGLE_API_KEY="..."` ja `.\gradlew.bat bootRun`)
- Testid: `./gradlew test`
- Claude ei saa rakendust käivitada, sest tal pole võtit. Käivitamise kontrolliks on kasutatud võltsitud Gemini serverit.

## Keskkond (WSL + IntelliJ)
- `JAVA_HOME` viitas IntelliJ Windowsi JDK-le (`.jdks/jbr-25...`), mis WSL-is ei tööta. Projekt vajab Java 21.
  `~/.bashrc` sisaldab juba varuvarianti, mis seab `/usr/lib/jvm/java-21-openjdk-amd64`. Vana terminal vajab `source ~/.bashrc`.
- `gradle.properties` sisaldab `org.gradle.configuration-cache=true`.
- IntelliJ-is "run build.gradle" annab vea `Unknown command-line option '-b'` (Gradle 9 eemaldas `-b`). Ära kasuta; käivita `MottgenApplication` või `bootRun`.

## Struktuur (`src/main/java/com/example/mottgen`)
- `LetterController` – `GET /` (leht), `POST /api/letter` (JSON), `LetterException` -> `{"error": ...}`
- `LetterService` – süsteemiprompt + few-shot, CV valideerimine ja isikuandmete eemaldamine, mudelikutse, temperature, conversationId
- `JobPostingFetcher` – kuulutuse tekst Jsoupiga + ettevõtte "meist" lehtede info (max 3 lehte, 2500 märki; LinkedIn/Facebook jms ignoreeritud); SSRF-kaitse (`rejectInternalHost`)
  Kui kuulutuse teksti lehelt ei leita (JSON-LD `JobPosting` kirjeldus < 200 märki, nt kuulutus on pildina, või leht < 100 märki), viskab 422 vea enne mudelikutset.
- `PersonalDataRedactor` – e-post, telefon, isikukood (11 numbrit, algab 1–6-ga, kontrollsummat ei kontrollita) -> `[E-POST]`, `[TELEFON]`, `[ISIKUKOOD]`
- `LetterMetricsAdvisor` – oma advisor, logib tokenid jms
- `LetterRequest(jobUrl, cvText, conversationId, temperature)`, `LetterResponse`, `MotivationLetter` (`reasoning` enne `letter`) – recordid
- Testid: `JobPostingFetcherTest` (sh ettevõtte info leidmine), `PersonalDataRedactorTest`

## Disainiotsused
- **Grounding:** kuulutuse tegelik tekst antakse promptis; prompt keelab fakte välja mõelda; kuulutuse/CV sees olevad juhised on andmed, mitte käsud (prompt injection).
- **Structured output:** `MotivationLetter` + `useProviderStructuredOutput().validateSchema()`.
- **Advisorid:** `SimpleLoggerAdvisor`, `MessageChatMemoryAdvisor` (leht hoiab conversationId sessionStorage'is), `LetterMetricsAdvisor`.
- **Temperature:** vaikimisi 0.6 (`application.properties`), max tokens 1500. Frontendi slider (0,5–2,0, samm 0,1) saadab väärtuse
  päringus ja kirjutab selle üle ainult selle päringu jaoks; failis olev väärtus ei muutu. Serveripoolne vahemik: `MIN/MAX_TEMPERATURE` `LetterService`'is.
- **Kuulutus pildina:** CVKeskus.ee kuulutuste tekst on mõnikord pilt (`/gfx/tpl_jobs/...jpg`), HTML-is ainult pealkiri ja palk. Fetcher annab selge veateate
  ("Kopeeri kuulutuse tekst käsitsi lisatekstiks"). Pildi lugemine Geminiga (multimodaalne sisend) on kaalutud, aga tegemata; mälu kasvaks pildi võrra.
- **Privaatsus:** isikuandmed (e-post, telefon, isikukood) eemaldatakse CV-st enne Google'i saatmist (kontrollitud võltsserveriga). 
  **Nimi, aadress ja lingid jäävad teksti** ja lähevad Google'ile (nii on kokku lepitud). Vormil on selle kohta märkus CV välja all.
  Eestikeelne lauatelefon, mis algab 1–4 või 9-ga ilma `+372`-ta, jääb märkamata.
- **Piirangud** (`application.properties`): `mottgen.job-posting.max-chars=8000`, `timeout-ms=8000`, `mottgen.cv.max-chars=12000`.
- **Postman:** ainus lõpp-punkt on `POST /api/letter`, JSON `{"jobUrl": "...", "cvText": "...", "temperature": 1.2}`; `temperature` ja `conversationId` on valikulised.

## Stiil
- Kommentaarid ja UI tekstid **eesti keeles**; kõigil meetoditel 2–5-realine `//` kommentaar (annotatsiooni kohal); taandeks tabid.
- `.idea/Docs` all on mockup (`motivatsioon-mockup.html`), stiilinäited (`skill/`) ja sessioonimärkmed; `.idea` on gitignore'is.

## Töökorraldus
- Kasutaja kirjutab eesti keeles; vasta eesti keeles. Kui ta ütleb "ära muuda", siis ainult selgita.
- Võtmeid ei kirjutata faili ega chatti. `.env` on gitignore'is.

## Võimalikud järgmised sammud
- Kirja kvaliteedi hindamine (ainult CV faktid, pikkus, toon); prompti/temperature'i tuunimine
- Veaolukorrad: vigane link, tühi CV, sisselogimist nõudev kuulutus (pildina kuulutus on tehtud)
- Gemini otsingu-grounding (kui soovitud)
