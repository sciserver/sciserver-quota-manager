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
			healthBuilder.withDetail("numberOfQuotaInfos", allQuotaInfo.size());
			List<RuntimeException> errors = new ArrayList<>();

			config.getRootVolumes().entrySet().stream()
				.forEach(rvEntry -> {
					String rootVolumeName = rvEntry.getKey();
					String rootVolumePath = rvEntry.getValue().getPathOnFileServer();
					Path rootVolumeAsPath = Paths.get(rootVolumePath);
					if (!Files.isDirectory(rootVolumeAsPath)) {
						errors.add(new RootVolumeNotFound("Could not find '"+rootVolumeName+"' at "+rootVolumePath));
						return;
					}
					try {
						Files.walk(rootVolumeAsPath, 2)
							.filter(folder -> !folder.equals(rootVolumeAsPath))
							.forEach(folder -> {
								Path relativePath = rootVolumeAsPath.relativize(folder);
								Optional<Quota> folderQuota = allQuotaInfo
									.stream()
									.filter(q -> q.getRootVolumeName().equals(rootVolumeName))
									.filter(q -> relativePath.toString().equals(q.getRelativePath()))
									.findFirst();
								if (relativePath.getNameCount() == 1 &&
										(rvEntry.getValue().getPerUserQuota() > 0 || folderQuota.isPresent())) {
									if (!folderQuota.isPresent()) {
										errors.add(new NoQuotaOnUserFolder(
											"No user-id level quota found on " + folder.toAbsolutePath()));
									} else {
										if (folderQuota.get().getNumberOfFilesQuota() != rvEntry.getValue().getPerUserQuota()) {
											errors.add(new IncorrectQuotaOnUserFolder(
												String.format("Expect a quota of %d bytes on %s, but the quota is set to %d bytes",
														rvEntry.getValue().getPerUserQuota(),
														folder.toAbsolutePath(),
														folderQuota.get().getNumberOfFilesQuota())));
										}
									}
								}
								if (relativePath.getNameCount() == 2 &&
										(rvEntry.getValue().getPerVolumeQuota() > 0 || folderQuota.isPresent())) {
									if (!folderQuota.isPresent()) {
										errors.add(new NoQuotaOnUserVolume(
											"No volume level quota found on " + folder.toAbsolutePath()));
									} else {
										if (folderQuota.get().getNumberOfFilesQuota() != rvEntry.getValue().getPerUserQuota()) {
											errors.add(new IncorrectQuotaOnUserFolder(
												String.format("Expect a quota of %d bytes on %s, but the quota is set to %d bytes",
														rvEntry.getValue().getPerUserQuota(),
														folder.toAbsolutePath(),
														folderQuota.get().getNumberOfFilesQuota())));
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
					.withDetail("error", errors.stream().map(e -> e.toString()));
			}
			return healthBuilder.build();
		} catch (Exception e) {
			return healthBuilder.down(e).build();
		}
	}

	private class NoQuotaOnUserFolder extends RuntimeException {
		private static final long serialVersionUID = -4439581930288487714L;
		private NoQuotaOnUserFolder(String message) {
			super(message);
		}
	}

	private class NoQuotaOnUserVolume extends RuntimeException {
		private static final long serialVersionUID = -3001467880444892661L;
		private NoQuotaOnUserVolume(String message) {
			super(message);
		}
	}

	private class IncorrectQuotaOnUserFolder extends RuntimeException {
		private static final long serialVersionUID = -7482250478054034144L;
		private IncorrectQuotaOnUserFolder(String message) {
			super(message);
		}
	}

	private class RootVolumeNotFound extends RuntimeException {
		private static final long serialVersionUID = 8045474482470737834L;
		private RootVolumeNotFound(String message) {
			super(message);
		}
	}
}
