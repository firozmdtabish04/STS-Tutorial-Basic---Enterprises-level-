package com.tutorial.security.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventLogger {

	private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

	public void log(SecurityEvent event, String email, String message) {

		log.info("SECURITY_EVENT event={} email={} message={}", event, email, message);
	}

	public void log(SecurityEvent event, Long userId, String email, String message) {

		log.info("SECURITY_EVENT event={} userId={} email={} message={}", event, userId, email, message);
	}
}