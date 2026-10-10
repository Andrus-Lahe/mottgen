package com.example.mottgen;

import org.springframework.http.HttpStatus;

public class LetterException extends RuntimeException {

	private final HttpStatus status;

	// Loob erindi, mis kannab endaga HTTP staatust ja kasutajale
	// mõeldud veateksti. Sõnum edastatakse hiljem JSON-vastuses.
	public LetterException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	// Annab HTTP staatuse, mida kontrolleri veakäsitleja
	// vastuse staatusena kasutab.
	public HttpStatus getStatus() {
		return status;
	}
}
