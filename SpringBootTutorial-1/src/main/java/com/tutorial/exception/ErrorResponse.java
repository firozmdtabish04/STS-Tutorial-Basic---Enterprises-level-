package com.tutorial.exception;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorResponse {

	private LocalDateTime timestamp;

	private int status;

	private String error;

	private String message;

	private String path;

	private String errorCode;

	private String traceId;
}
