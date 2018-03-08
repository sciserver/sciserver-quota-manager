package org.sciserver.fileservice.manager;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.ScriptAssert;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Configuration
@ConfigurationProperties(prefix="org.sciserver.fileservice.manager")
@Validated
public class Config {
	@Valid
	private Map<String, RootVolume> rootVolumes = new HashMap<>();

	public Map<String, RootVolume> getRootVolumes() {
		return rootVolumes;
	}

	@ScriptAssert(lang="javascript", script="!(_this.perUserQuota && _this.perVolumeQuota)",
			message="Cannot set both a per-user and per-volume quota")
	public static class RootVolume {
		@NotBlank
		private String pathOnFileServer;
		private long perUserQuota;
		private long perVolumeQuota;

		public void setPathOnFileServer(String pathOnFileServer) {
			this.pathOnFileServer = pathOnFileServer;
		}
		public void setPerUserQuota(long perUserQuota) {
			this.perUserQuota = perUserQuota;
		}
		public void setPerVolumeQuota(long perVolumeQuota) {
			this.perVolumeQuota = perVolumeQuota;
		}
		public String getPathOnFileServer() {
			return pathOnFileServer;
		}
		public long getPerUserQuota() {
			return perUserQuota;
		}
		public long getPerVolumeQuota() {
			return perVolumeQuota;
		}
	}
}
