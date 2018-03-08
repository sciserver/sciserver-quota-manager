package org.sciserver.fileservice.manager;

public class UnknownVolumeNameException extends RuntimeException {
	private static final long serialVersionUID = -1770769172806700962L;

	UnknownVolumeNameException(String message) {
		super(message);
	}
}
