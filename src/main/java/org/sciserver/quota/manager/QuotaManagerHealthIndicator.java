/*******************************************************************************
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sciserver.quota.manager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.sciserver.quota.manager.dto.Quota;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class QuotaManagerHealthIndicator implements HealthIndicator {
	private final Config config;
	private final FileSystemModule fileSystemModule;

	public QuotaManagerHealthIndicator(Config config, FileSystemModule fileSystemModule) {
		this.config = config;
		this.fileSystemModule = fileSystemModule;
	}

	@Override
	public Health health() {
		Health.Builder healthBuilder = new Health.Builder().up();
		try {
			Collection<Quota> allQuotaInfo = fileSystemModule.getUsage();
			List<QuotaProblem> errors = new ArrayList<>();

			config.getRootVolumes().entrySet().stream()
				.forEach(rvEntry -> {
					String rootVolumeName = rvEntry.getKey();
					String rootVolumePath = rvEntry.getValue().getPathOnFileServer();
					Path rootVolumeAsPath = Paths.get(rootVolumePath);
					if (!Files.isDirectory(rootVolumeAsPath)) {
						errors.add(new QuotaProblem(rootVolumePath,
								"Could not find '"+rootVolumeName+"'"));
						return;
					}
					try {
						Files.walk(rootVolumeAsPath, 2)
							.filter(folder -> !folder.equals(rootVolumeAsPath))
							.forEach(folder -> {
								Path relativePath = rootVolumeAsPath.relativize(folder);
								String folderFullName = folder.toAbsolutePath().toString();
								Optional<Quota> folderQuota = allQuotaInfo
									.stream()
									.filter(q -> q.getRootVolumeName().equals(rootVolumeName))
									.filter(q -> relativePath.toString().equals(q.getRelativePath()))
									.findFirst();
								if (relativePath.getNameCount() == 1) {
									checkUserQuota(errors, rvEntry.getValue().getPerUserQuota(),
											folderFullName, folderQuota);
								}
								if (relativePath.getNameCount() == 2) {
									checkUserVolumeQuota(errors, rvEntry.getValue().getPerVolumeQuota(),
											folderFullName, folderQuota);
								}
							});
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			if (!errors.isEmpty()) {
				healthBuilder
					.down()
					.withDetail("errors", errors);
			}
			return healthBuilder.build();
		} catch (Exception e) {
			return healthBuilder.down(e).build();
		}
	}

	private void checkUserQuota(List<QuotaProblem> errors, long perUserQuota,
			String folderFullName, Optional<Quota> folderQuota) {
		long existingQuota = folderQuota.map(Quota::getNumberOfBytesQuota).orElse(0L);

		if (perUserQuota > 0 && !folderQuota.isPresent()) {
			errors.add(new QuotaProblem(folderFullName, "No user-id level quota found"));
		} else if (perUserQuota != existingQuota) {
			errors.add(new QuotaProblem(folderFullName,
				String.format("Expect a quota of %d bytes, but the quota is set to %d bytes",
						perUserQuota,
						existingQuota)));
		}

		// defaulting to zero since we don't know how much space is used unless the
		// folder has a quota set
		long existingBytesUsed = folderQuota.map(Quota::getNumberOfBytesUsed).orElse(0L);
		if (existingBytesUsed > 1.1*perUserQuota) {
			errors.add(new QuotaProblem(folderFullName,
					String.format("A quota of %d bytes is exceeded by over 10%%. %d bytes are in use.",
							perUserQuota, existingBytesUsed)));
		}
	}

	private void checkUserVolumeQuota(List<QuotaProblem> errors, long perVolumeQuota,
			String folderFullName, Optional<Quota> folderQuota) {
		long existingQuota = folderQuota.map(Quota::getNumberOfBytesQuota).orElse(0L);

		if (perVolumeQuota > 0 && !folderQuota.isPresent()) {
			errors.add(new QuotaProblem(folderFullName, "No volume level quota found"));
		} else if (perVolumeQuota != existingQuota) {
			errors.add(new QuotaProblem(folderFullName,
				String.format("Expect a quota of %d bytes, but the quota is set to %d bytes",
						perVolumeQuota,
						existingQuota)));
		}

		// defaulting to zero since we don't know how much space is used unless the
		// folder has a quota set
		long existingBytesUsed = folderQuota.map(Quota::getNumberOfBytesUsed).orElse(0L);
		if (existingBytesUsed > 1.1 * perVolumeQuota) {
			errors.add(new QuotaProblem(folderFullName,
					String.format("A quota of %d bytes is exceeded by over 10%%. %d bytes are in use.",
							perVolumeQuota, existingBytesUsed)));
		}
	}

	@SuppressWarnings("unused")
	private class QuotaProblem {
		private final String path;
		private final String message;
		private QuotaProblem(String path, String message) {
			this.path = path;
			this.message = message;
		}
		public String getPath() {
			return path;
		}
		public String getMessage() {
			return message;
		}
	}
}
