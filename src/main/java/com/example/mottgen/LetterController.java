package com.example.mottgen;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class LetterController {

	private final LetterService letterService;

	// Konstruktor saab LetterService'i Springi sõltuvuste süstimise kaudu.
	// Kontroller ise äriloogikat ei sisalda, vaid delegeerib teenusele.
	public LetterController(LetterService letterService) {
		this.letterService = letterService;
	}

	// Serveerib avalehe: tagastab mallinime "index", mille Spring
	// leiab templates kaustast ja kuvab brauseris vormina.
	@GetMapping("/")
	public String index() {
		return "index";
	}

	// Võtab vastu JSON päringu (töökuulutuse link ja CV) ning annab selle teenusele.
	// Tagastab valmis motivatsioonikirja JSON-vastusena.
	@PostMapping("/api/letter")
	@ResponseBody
	public LetterResponse createLetter(@RequestBody LetterRequest request) {
		return letterService.createLetter(request);
	}

	// Püüab kinni LetterException'i ja muudab selle JSON-vastuseks kujul {"error": ...}.
	// HTTP staatus võetakse erindist, nii et frontend näeb sobivat veateadet.
	@ExceptionHandler(LetterException.class)
	@ResponseBody
	public ResponseEntity<Map<String, String>> handle(LetterException e) {
		return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
	}
}
