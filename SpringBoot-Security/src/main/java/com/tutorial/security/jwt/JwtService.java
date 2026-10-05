package com.tutorial.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${app.jwt.secret}")
	private String secret;

	@Value("${app.jwt.expiration}")
	private long jwtExpiration;

	private SecretKey getSigningKey() {

		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(UserDetails userDetails) {

		Date now = new Date();

		Date expiration = new Date(now.getTime() + jwtExpiration);

		return Jwts.builder().subject(userDetails.getUsername()).issuedAt(now).expiration(expiration)
				.signWith(getSigningKey()).compact();
	}

	public String extractUsername(String token) {

		return parseToken(token).getPayload().getSubject();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {

		try {

			String username = extractUsername(token);

			return username.equals(userDetails.getUsername()) && !isTokenExpired(token);

		} catch (JwtException | IllegalArgumentException e) {

			return false;
		}
	}

	private boolean isTokenExpired(String token) {

		Date expiration = parseToken(token).getPayload().getExpiration();

		return expiration.before(new Date());
	}

	private Jws<Claims> parseToken(String token) {

		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
	}

	public long getExpiration() {
		return jwtExpiration;
	}
}
