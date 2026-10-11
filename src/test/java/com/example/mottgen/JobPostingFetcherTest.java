package com.example.mottgen;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobPostingFetcherTest {

	private final JobPostingFetcher fetcher = new JobPostingFetcher(8000, 1000);

	// Kontrollib, et tühi või ainult tühikutest koosnev link
	// lükatakse tagasi LetterException'iga.
	@Test
	void rejectsBlankUrl() {
		assertThrows(LetterException.class, () -> fetcher.fetchText(" "));
	}

	// Kontrollib, et mitte-http(s) skeem (nt file://) lükatakse tagasi.
	// See takistab serveri failide lugemist lingi kaudu.
	@Test
	void rejectsNonHttpScheme() {
		assertThrows(LetterException.class, () -> fetcher.fetchText("file:///etc/passwd"));
	}

	// Kontrollib, et localhost on keelatud.
	// See on SSRF kaitse test: sisemisi aadresse ei tohi tõmmata.
	@Test
	void rejectsLocalhost() {
		assertThrows(LetterException.class, () -> fetcher.fetchText("http://localhost:8080/admin"));
	}

	// Kontrollib, et ettevõtte link leitakse ka jaluses olevast lingist
	// ja et tulemuseks on ainult skeem + host (ilma teeta).
	@Test
	void findsCompanyUrlFromFooterLink() {
		Document doc = Jsoup.parse(
				"<body><p>Tööpakkumine</p><footer><a href='https://www.firma.ee/avaleht?x=1'>Meie veeb</a></footer></body>",
				"https://tootukassa.example/job/1");
		assertEquals("https://www.firma.ee", fetcher.findCompanyUrl(doc, "tootukassa.example"));
	}

	// Kontrollib, et sama domeeni lingid ning tööportaalid ja sotsiaalmeedia
	// ei lähe ettevõtte kodulehe kandidaatideks.
	@Test
	void ignoresSameSiteAndSocialLinks() {
		Document doc = Jsoup.parse(
				"<a href='/kandideeri'>Kandideeri</a>"
						+ "<a href='https://www.job.ee/teine'>Sama sait</a>"
						+ "<a href='https://www.linkedin.com/company/firma'>LinkedIn</a>"
						+ "<a href='https://facebook.com/firma'>Facebook</a>"
						+ "<a href='mailto:info@firma.ee'>Kiri</a>",
				"https://job.ee/kuulutus/1");
		assertNull(fetcher.findCompanyUrl(doc, "job.ee"));
	}

	// Kontrollib, et ignoreeritud linkide järel olev esimene päris ettevõtte link võetakse.
	@Test
	void skipsIgnoredLinksAndPicksCompany() {
		Document doc = Jsoup.parse(
				"<a href='https://www.linkedin.com/company/firma'>LinkedIn</a>"
						+ "<a href='https://firma.ee/'>Kodulehekülg</a>",
				"https://job.ee/kuulutus/1");
		assertEquals("https://firma.ee", fetcher.findCompanyUrl(doc, "job.ee"));
	}

	// Kontrollib, et "meist" tüüpi märksõnadega lingid leitakse nii eesti kui inglise keeles,
	// ebaolulised lingid (kontakt) jäetakse välja ja #ankur lõigatakse maha.
	@Test
	void findsAboutLinksByKeywords() {
		Document home = Jsoup.parse(
				"<a href='/meist'>Meist</a>"
						+ "<a href='/missioon#top'>Meie missioon</a>"
						+ "<a href='/en/about-us'>Tutvu</a>"
						+ "<a href='/kontakt'>Kontakt</a>",
				"https://firma.ee");
		Set<String> links = fetcher.findAboutLinks(home, "firma.ee");
		assertEquals(Set.of("https://firma.ee/meist", "https://firma.ee/missioon", "https://firma.ee/en/about-us"), links);
	}

	// Kontrollib, et välised domeenid ei satu "meist" linkide hulka ja
	// www.firma.ee ning firma.ee loetakse samaks saidiks.
	@Test
	void aboutLinksStayOnSameSite() {
		Document home = Jsoup.parse(
				"<a href='https://www.firma.ee/meist'>Meist</a>"
						+ "<a href='https://teine.ee/meist'>Meist</a>",
				"https://firma.ee");
		assertEquals(Set.of("https://www.firma.ee/meist"), fetcher.findAboutLinks(home, "firma.ee"));
	}

	// Kontrollib, et alamlehti võetakse maksimaalselt kolm, et päringute arv ei kasvaks.
	@Test
	void limitsAboutLinksToThree() {
		Document home = Jsoup.parse(
				"<a href='/meist'>a</a><a href='/about'>b</a><a href='/missioon'>c</a>"
						+ "<a href='/expertise'>d</a><a href='/mission'>e</a>",
				"https://firma.ee");
		assertEquals(3, fetcher.findAboutLinks(home, "firma.ee").size());
	}

	// Kontrollib, et JobPosting koos peaaegu tühja kirjeldusega (kuulutus on pildil) tuvastatakse.
	@Test
	void detectsEmptyJobDescription() {
		Document doc = Jsoup.parse("<script type='application/ld+json'>{\"@graph\":[{\"@type\":\"JobPosting\","
				+ "\"title\":\"Uurija\",\"description\":\"<p>Uurija Amet</p>\"}]}</script><body>x</body>");
		assertTrue(fetcher.hasEmptyJobDescription(doc));
	}

	// Kontrollib, et piisavalt pikk JobPosting'i kirjeldus ega JSON-LD puudumine ei anna valehäiret.
	@Test
	void acceptsLongDescriptionOrNoJsonLd() {
		String longText = "a".repeat(300);
		Document withLong = Jsoup.parse("<script type='application/ld+json'>{\"@type\":\"JobPosting\","
				+ "\"description\":\"" + longText + "\"}</script><body>x</body>");
		assertFalse(fetcher.hasEmptyJobDescription(withLong));
		assertFalse(fetcher.hasEmptyJobDescription(Jsoup.parse("<body>tavaline leht</body>")));
	}
}
