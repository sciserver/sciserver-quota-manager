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
package org.sciserver.fileservice.manager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.sciserver.fileservice.manager.dto.Quota;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FileServiceManagerHealthIndicator implements HealthIndicator {
	private final Config config;
	private final FileServiceModule fileServiceModule;

	public FileServiceManagerHealthIndicator(Config config, FileServiceModule fileServiceModule) {
		this.config = config;
		this.fileServiceModule = fileServiceModule;
	}

	@Override
	public Health health() {
		Health.Builder healthBuilder = new Health.Builder().up();
		try {
			Collection<Quota> allQuotaInfo = fileServiceModule.getUsage();
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
								if (relativePath.getNameCount() == 1 &&
										(rvEntry.getValue().getPerUserQuota() > 0 || folderQuota.isPresent())) {
									if (!folderQuota.isPresent()) {
										errors.add(new QuotaProblem(folderFullName, "No user-id level quota found"));
									} else {
										if (folderQuota.get().getNumberOfBytesQuota() != rvEntry.getValue().getPerUserQuota()) {
											errors.add(new QuotaProblem(folderFullName,
												String.format("Expect a quota of %d bytes, but the quota is set to %d bytes",
														rvEntry.getValue().getPerUserQuota(),
														folderQuota.get().getNumberOfBytesQuota())));
										}
									}
								}
								if (relativePath.getNameCount() == 2 &&
										(rvEntry.getValue().getPerVolumeQuota() > 0 || folderQuota.isPresent())) {
									if (!folderQuota.isPresent()) {
										errors.add(new QuotaProblem(folderFullName, "No volume level quota found"));
									} else {
										if (folderQuota.get().getNumberOfBytesQuota() != rvEntry.getValue().getPerVolumeQuota()) {
											errors.add(new QuotaProblem(folderFullName,
												String.format("Expect a quota of %d bytes, but the quota is set to %d bytes",
														rvEntry.getValue().getPerVolumeQuota(),
														folderQuota.get().getNumberOfBytesQuota())));
										}
									}
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
