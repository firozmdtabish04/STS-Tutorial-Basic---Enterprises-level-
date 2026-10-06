package com.tutorial.security.jwt;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public JwtAccessDeniedHandler(ObjectMapper objectMapper) {

		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
			throws IOException, ServletException {

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		response.setCharacterEncoding("UTF-8");

		ErrorResponse error = new ErrorResponse(403, "Forbidden", "You do not have permission to access this resource",
				request.getRequestURI());

		response.getWriter().write(objectMapper.writeValueAsString(error));
	}

	private record ErrorResponse(int status, String error, String message, String path) {
	}
}