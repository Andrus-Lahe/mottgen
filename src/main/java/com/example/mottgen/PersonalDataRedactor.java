package com.example.mottgen;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PersonalDataRedactor {

	private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");

	// Isikukood: täpselt 11 numbrit, mis algavad 1-6ga. Ees olev + jäetakse välja, et +372-ga telefon
	// (+37251234567) ei loetaks isikukoodiks. Kontrollnumbrit ei arvutata, vigane kood eemaldatakse samuti.
	private static final Pattern PERSONAL_ID = Pattern.compile("(?<![\\d+])[1-6]\\d{10}(?!\\d)");

	// Suunakoodiga (+372 / 00372) sobib iga 7-8 kohaline number. Ilma suunakoodita nõuame, et number algaks
	// 5-8ga (Eesti mobiil- ja lauatelefonid), et aastavahemikud ("2019-2023") ega palgad kaasa ei tuleks.
	private static final Pattern PHONE = Pattern.compile(
			"(?<![\\w+])(?:(?:\\+|00)\\s?372[\\s-]?\\d|[5-8])(?:[\\s-]?\\d){6,7}(?!\\d)");

	// Eemaldab tekstist e-posti aadressid, isikukoodid ja telefoninumbrid ning asendab need kohatäitega.
	// Järjekord on oluline: isikukood tuleb enne telefoni, et selle numbreid telefoniks ei peetaks.
	public String redact(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String result = EMAIL.matcher(text).replaceAll("[E-POST]");
		result = PERSONAL_ID.matcher(result).replaceAll("[ISIKUKOOD]");
		return PHONE.matcher(result).replaceAll("[TELEFON]");
	}
}
