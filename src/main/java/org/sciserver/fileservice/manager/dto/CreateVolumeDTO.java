package org.sciserver.fileservice.manager.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateVolumeDTO {
	private final String rootVolumeName;
	private final String relativePath;

	@JsonCreator
	public CreateVolumeDTO(
			@JsonProperty("rootVolumeName") String rootVolumeName,
			@JsonProperty("relativePath") String relativePath) {
		super();
		this.rootVolumeName = rootVolumeName;
		this.relativePath = relativePath;
	}

	public String getRootVolumeName() {
		return rootVolumeName;
	}

	public String getRelativePath() {
		return relativePath;
	}
}
