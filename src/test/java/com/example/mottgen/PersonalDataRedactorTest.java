package com.example.mottgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDataRedactorTest {

	private final PersonalDataRedactor redactor = new PersonalDataRedactor();

	@Test
	void redactsEmail() {
		assertEquals("Kirjuta [E-POST] peale.", redactor.redact("Kirjuta mari.maasikas+cv@example.ee peale."));
	}

	@Test
	void redactsPhoneFormats() {
		assertEquals("[TELEFON]", redactor.redact("+372 5123 4567"));
		assertEquals("[TELEFON]", redactor.redact("+37251234567"));
		assertEquals("[TELEFON]", redactor.redact("0037251234567"));
		assertEquals("[TELEFON]", redactor.redact("51234567"));
		assertEquals("[TELEFON]", redactor.redact("512 34567"));
		assertEquals("[TELEFON]", redactor.redact("5123-4567"));
		assertEquals("[TELEFON]", redactor.redact("6123456"));
	}

	@Test
	void redactsValidPersonalId() {
		assertEquals("Isikukood: [ISIKUKOOD].", redactor.redact("Isikukood: 37605030299."));
		assertEquals("[ISIKUKOOD]", redactor.redact("49403136526"));
	}

	@Test
	void redactsPersonalIdEvenWithWrongChecksum() {
		assertEquals("[ISIKUKOOD]", redactor.redact("37605030298"));
	}

	@Test
	void doesNotTouchElevenDigitsStartingWithSevenToNine() {
		assertEquals("78605030298", redactor.redact("78605030298"));
	}

	@Test
	void doesNotTouchOrdinaryNumbers() {
		String text = "2019-2023 töötasin, palk 1800 eurot, aastal 2020 ja 2021, postiindeks 10115, 3 aastat.";
		assertEquals(text, redactor.redact(text));
	}

	@Test
	void redactsAllTypesTogether() {
		assertEquals("[E-POST], [TELEFON], [ISIKUKOOD]",
				redactor.redact("mari@x.ee, +372 5123 4567, 37605030299"));
	}

	@Test
	void handlesNullAndEmpty() {
		assertEquals(null, redactor.redact(null));
		assertEquals("", redactor.redact(""));
	}
}
