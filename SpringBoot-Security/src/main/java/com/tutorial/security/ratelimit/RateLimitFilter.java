package com.tutorial.security.ratelimit;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tutorial.service.RateLimitService;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitService rateLimitService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();
		String method = request.getMethod();

		if (!"POST".equalsIgnoreCase(method)) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientIp = getClientIp(request);

		ConsumptionProbe probe = null;
		long rateLimit = 0;

		if ("/api/auth/login".equals(path)) {

			rateLimit = 5;
			probe = rateLimitService.tryLogin(clientIp);

		} else if ("/api/auth/register".equals(path)) {

			rateLimit = 3;
			probe = rateLimitService.tryRegister(clientIp);

		} else if ("/api/auth/refresh".equals(path)) {

			rateLimit = 10;
			probe = rateLimitService.tryRefresh(clientIp);
		}

		if (probe == null) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));

		response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

		if (!probe.isConsumed()) {

			long retryAfterSeconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);

			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

			response.setContentType("application/json");

			response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

			response.getWriter().write("""
					{
					    "status": 429,
					    "error": "Too Many Requests",
					    "message": "Rate limit exceeded. Please try again later.",
					    "retryAfter": %d
					}
					""".formatted(retryAfterSeconds));

			return;
		}

		filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {

		String forwardedFor = request.getHeader("X-Forwarded-For");

		if (forwardedFor != null && !forwardedFor.isBlank()) {

			return forwardedFor.split(",")[0].trim();
		}

		return request.getRemoteAddr();
	}
}