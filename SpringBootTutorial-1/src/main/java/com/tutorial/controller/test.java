package com.tutorial.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class test {
	@RequestMapping("/")
	public String get() {
		return "hey";
	}
}
