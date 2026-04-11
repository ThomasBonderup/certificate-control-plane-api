package com.combotto.controlplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@SpringBootApplication
@RestController
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}

	@GetMapping("/healthz")
	public RedirectView healthz() {
		return new RedirectView("/actuator/health");
	}

	// @PostMapping("/audit/runs")
	// public String auditRuns(@RequestParam(value = "name", defaultValue = "World")
	// String name) {
	// return String.format("Hello %s!", name);
	// }
}
