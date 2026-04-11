package com.combotto.controlplane;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Application.class)
class ApplicationTests {

	@Autowired
	MockMvc mvc;

	@Test
	void hello_defaultName() throws Exception {
		mvc.perform(get("/hello"))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello World!"));
	}

	@Test
	void hello_customName() throws Exception {
		mvc.perform(get("/hello").param("name", "Thomas"))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello Thomas!"));
	}

	@Test
	void contextLoads() {
	}

}
