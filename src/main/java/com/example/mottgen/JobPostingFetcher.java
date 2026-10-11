package com.example.mottgen;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Grounding: tõmbab töökuulutuse tegeliku teksti, et AI ei peaks ametikohta ise "ära arvama". */
@Component
public class JobPostingFetcher {

	// Märksõnad, mille järgi ettevõtte lehelt "meist" tüüpi alamlehti otsitakse (link-tekstis või aadressis).
	private static final List<String> ABOUT_KEYWORDS = List.of("meist", "meie", "missioon", "mida me teeme",
			"ekspertiis", "teenused", "about", "mission", "what we do", "expertise", "who we are");
	// Tööportaalid ja sotsiaalmeedia, mida ettevõtte enda veebiks ei loeta.
	private static final List<String> IGNORED_HOSTS = List.of("linkedin.", "facebook.", "instagram.", "twitter.",
			"x.com", "youtube.", "cv.ee", "cvkeskus.", "tootukassa.", "google.", "tiktok.");
	private static final int MAX_ABOUT_PAGES = 3;
	private static final int MAX_ABOUT_CHARS = 2500;
	// Alla selle pikkuse loetakse kuulutuse kirjeldus (või kogu leht) puuduvaks.
	private static final int MIN_DESCRIPTION_CHARS = 200;
	private static final int MIN_PAGE_CHARS = 100;
	// Leiab JSON-LD plokist JobPosting'u kirjelduse välja.
	private static final Pattern JOB_DESCRIPTION = Pattern.compile(
			"\"@type\"\\s*:\\s*\"JobPosting\".*?\"description\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.DOTALL);

	private final int maxChars;
	private final int timeoutMs;

	// Konstruktor võtab seadistusest maksimaalse teksti pikkuse ja
	// ühenduse ajalimiidi. Spring süstib väärtused application
	// properties failist @Value annotatsiooni abil.
	public JobPostingFetcher(@Value("${mottgen.job-posting.max-chars}") int maxChars,
							 @Value("${mottgen.job-posting.timeout-ms}") int timeoutMs) {
		this.maxChars = maxChars;
		this.timeoutMs = timeoutMs;
	}

	// Põhimeetod: kontrollib lingi, laeb lehe alla ja puhastab selle tekstiks.
	// Eemaldab skriptid, menüüd ja muu müra, et AI saaks ainult kuulutuse sisu.
	// Liiga pika teksti lõikab maksimumpikkuseni, et hoida tokenite arvu kontrolli all.
	public String fetchText(String jobUrl) {
		URI uri = parseAndValidate(jobUrl);
		Document doc = download(uri);
		// Ettevõtte link tuleb leida enne puhastust, sest see on tihti jaluses või päises.
		String companyUrl = findCompanyUrl(doc, uri.getHost());
		// JSON-LD asub skriptis, seega see kontroll tuleb teha enne skriptide eemaldamist.
		boolean emptyDescription = hasEmptyJobDescription(doc);
		doc.select("script, style, nav, footer, header, noscript, form").remove();
		String text = doc.title() + "\n" + doc.body().text();
		if (emptyDescription || doc.body().text().length() < MIN_PAGE_CHARS) {
			throw new LetterException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Lehelt ei leitud kuulutuse teksti (see võib olla pilt või laetakse JavaScriptiga). "
							+ "Kopeeri kuulutuse tekst käsitsi lisatekstiks.");
		}
		if (text.length() > maxChars) {
			text = text.substring(0, maxChars);
		}
		return text + fetchCompanyInfo(companyUrl);
	}

	// Kontrollib lehe JSON-LD andmeid: kui seal on JobPosting, aga selle kirjeldus on peaaegu tühi,
	// on kuulutuse tegelik tekst tõenäoliselt pildina. Ilma JobPosting'uta lehel tagastab false.
	boolean hasEmptyJobDescription(Document doc) {
		for (var script : doc.select("script[type=application/ld+json]")) {
			Matcher m = JOB_DESCRIPTION.matcher(script.data());
			if (m.find()) {
				String description = m.group(1).replaceAll("<[^>]*>", " ").trim();
				return description.length() < MIN_DESCRIPTION_CHARS;
			}
		}
		return false;
	}

	// Otsib kuulutuselt esimese lingi, mis viib teise domeeni kui kuulutus ise ja
	// pole tööportaal ega sotsiaalmeedia. Seda loetakse ettevõtte kodulehe aadressiks.
	// Tagastab null, kui sobivat linki ei leitud.
	String findCompanyUrl(Document doc, String postingHost) {
		for (var link : doc.select("a[href]")) {
			String href = link.absUrl("href");
			try {
				URI candidate = new URI(href);
				String host = candidate.getHost();
				if (host != null && href.startsWith("http") && !sameSite(host, postingHost) && !isIgnored(host)) {
					return candidate.getScheme() + "://" + host;
				}
			} catch (URISyntaxException e) {
				// vigane link jäetakse vahele
			}
		}
		return null;
	}

	// Laeb ettevõtte avalehe ja sealt kuni MAX_ABOUT_PAGES "meist" tüüpi alamlehte (üks link sügavale).
	// Tagastab leitud tekstid ühe plokina kuulutuse teksti lõppu lisamiseks.
	// Kõik vead neelatakse, sest ettevõtte info on lisa ja kuulutus peab töötama ka ilma.
	private String fetchCompanyInfo(String companyUrl) {
		if (companyUrl == null) {
			return "";
		}
		try {
			URI home = parseAndValidate(companyUrl);
			Document homePage = download(home);
			Set<String> aboutLinks = findAboutLinks(homePage, home.getHost());
			if (aboutLinks.isEmpty()) {
				return "";
			}
			StringBuilder info = new StringBuilder("\n\nEttevõtte info (" + home.getHost() + "):");
			for (String aboutUrl : aboutLinks) {
				try {
					Document page = download(parseAndValidate(aboutUrl));
					page.select("script, style, nav, footer, header, noscript, form").remove();
					String text = page.body().text();
					info.append("\n").append(text, 0, Math.min(text.length(), MAX_ABOUT_CHARS));
				} catch (LetterException e) {
					// üks ebaõnnestunud alamleht ei peata ülejäänuid
				}
			}
			return info.toString();
		} catch (LetterException e) {
			return "";
		}
	}

	// Valib ettevõtte avalehe lingid, mille tekst või aadress sisaldab "meist" tüüpi märksõna
	// ja mis jäävad samale domeenile. Kordused jäetakse välja ning hulk piiratakse MAX_ABOUT_PAGES-iga.
	Set<String> findAboutLinks(Document homePage, String host) {
		Set<String> result = new LinkedHashSet<>();
		for (var link : homePage.select("a[href]")) {
			String url = link.absUrl("href");
			String label = (link.text() + " " + url).toLowerCase(Locale.ROOT);
			boolean matches = ABOUT_KEYWORDS.stream().anyMatch(label::contains);
			try {
				String linkHost = new URI(url).getHost();
				if (matches && linkHost != null && sameSite(linkHost, host)) {
					result.add(url.split("#")[0]);
				}
			} catch (URISyntaxException e) {
				// vigane link jäetakse vahele
			}
			if (result.size() >= MAX_ABOUT_PAGES) {
				break;
			}
		}
		return result;
	}

	// Võrdleb kahte hostinime ilma "www." eesliiteta, et www.firma.ee ja firma.ee loetaks samaks.
	private boolean sameSite(String a, String b) {
		return a.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "")
				.equals(b.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", ""));
	}

	// Kontrollib, kas host kuulub tööportaali või sotsiaalmeedia nimekirja,
	// mida ettevõtte enda veebiks ei peeta.
	private boolean isIgnored(String host) {
		String h = host.toLowerCase(Locale.ROOT);
		return IGNORED_HOSTS.stream().anyMatch(h::contains);
	}

	// Kontrollib, et link pole tühi ja on korrektne http(s) aadress.
	// Keelatud hostide korral viskab LetterException'i, mis muutub
	// kasutajale arusaadavaks veateateks (HTTP 400).
	private URI parseAndValidate(String jobUrl) {
		if (jobUrl == null || jobUrl.isBlank()) {
			throw new LetterException(HttpStatus.BAD_REQUEST, "Sisesta töökuulutuse link.");
		}
		try {
			URI uri = new URI(jobUrl.trim());
			String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme) || uri.getHost() == null) {
				throw new LetterException(HttpStatus.BAD_REQUEST, "Link peab algama http:// või https://.");
			}
			rejectInternalHost(uri.getHost());
			return uri;
		} catch (URISyntaxException e) {
			throw new LetterException(HttpStatus.BAD_REQUEST, "Link ei ole korrektne.");
		}
	}

	// Lahendab hostinime IP-aadressiks ja lükkab tagasi sisemised aadressid
	// (localhost, privaatvõrk, link-local). See kaitseb serverit SSRF rünnaku eest.
	// Tundmatu domeeni korral annab kasutajale vea.
	private void rejectInternalHost(String host) {
		try {
			InetAddress address = InetAddress.getByName(host);
			if (address.isLoopbackAddress() || address.isSiteLocalAddress()
					|| address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
				throw new LetterException(HttpStatus.BAD_REQUEST, "See aadress ei ole lubatud.");
			}
		} catch (UnknownHostException e) {
			throw new LetterException(HttpStatus.BAD_REQUEST, "Linki ei leitud (tundmatu domeen).");
		}
	}

	// Laeb lehe Jsoup'iga alla kindla User-Agenti ja ajalimiidiga.
	// Võrguvea või ebaõnnestunud päringu korral viskab LetterException'i
	// staatusega BAD_GATEWAY, et kontroller saaks selle kasutajale tagastada.
	private Document download(URI uri) {
		try {
			return Jsoup.connect(uri.toString())
					.userAgent("Mozilla/5.0 (compatible; MottgenBot/1.0)")
					.timeout(timeoutMs)
					.get();
		} catch (IOException e) {
			throw new LetterException(HttpStatus.BAD_GATEWAY, "Töökuulutust ei õnnestunud avada. Kontrolli linki.");
		}
	}
}
