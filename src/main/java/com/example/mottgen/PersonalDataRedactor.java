package com.example.mottgen;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PersonalDataRedactor {

	private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");

	private static final Pattern PERSONAL_ID = Pattern.compile("(?<!\\d)[1-6]\\d{10}(?!\\d)");

	// Suunakoodiga (+372 / 00372) sobib iga 7-8 kohaline number. Ilma suunakoodita nõuame, et number algaks
	// 5-8ga (Eesti mobiil- ja lauatelefonid), et aastavahemikud ("2019-2023") ega palgad kaasa ei tuleks.
	private static final Pattern PHONE = Pattern.compile(
			"(?<![\\w+])(?:(?:\\+|00)\\s?372[\\s-]?\\d|[5-8])(?:[\\s-]?\\d){6,7}(?!\\d)");

	private static final int[] WEIGHTS_1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
	private static final int[] WEIGHTS_2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

	// Eemaldab tekstist e-posti aadressid, isikukoodid ja telefoninumbrid ning asendab need kohatäitega.
	// Järjekord on oluline: isikukood tuleb enne telefoni, et selle numbreid telefoniks ei peetaks.
	public String redact(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String result = EMAIL.matcher(text).replaceAll("[E-POST]");
		result = replacePersonalIds(result);
		return PHONE.matcher(result).replaceAll("[TELEFON]");
	}

	private String replacePersonalIds(String text) {
		Matcher m = PERSONAL_ID.matcher(text);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			m.appendReplacement(sb, Matcher.quoteReplacement(
					hasValidChecksum(m.group()) ? "[ISIKUKOOD]" : m.group()));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	// Eesti isikukoodi kontrollnumber: kaalutud summa mod 11, vajadusel teine kaalude komplekt.
	static boolean hasValidChecksum(String code) {
		int check = checksum(code, WEIGHTS_1);
		if (check == 10) {
			check = checksum(code, WEIGHTS_2);
			if (check == 10) {
				check = 0;
			}
		}
		return check == code.charAt(10) - '0';
	}

	private static int checksum(String code, int[] weights) {
		int sum = 0;
		for (int i = 0; i < 10; i++) {
			sum += (code.charAt(i) - '0') * weights[i];
		}
		return sum % 11;
	}
}
