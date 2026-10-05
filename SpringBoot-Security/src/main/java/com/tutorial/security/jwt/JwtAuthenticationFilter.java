package com.tutorial.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tutorial.security.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	private final CustomUserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Get Authorization header
		String authHeader = request.getHeader("Authorization");

		// No token
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {

			filterChain.doFilter(request, response);
			return;
		}

		// Extract JWT
		String token = authHeader.substring(7);

		try {

			// Extract username/email from JWT
			String username = jwtService.extractUsername(token);

			// Check if user is not already authenticated
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				// Load user from database
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				// Validate JWT
				if (jwtService.isTokenValid(token, userDetails)) {

					// Create authentication
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());

					// Add request details
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					// Store authentication
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}

		} catch (Exception e) {

			// Invalid or expired JWT
			SecurityContextHolder.clearContext();
		}

		// Continue filter chain
		filterChain.doFilter(request, response);
	}
}