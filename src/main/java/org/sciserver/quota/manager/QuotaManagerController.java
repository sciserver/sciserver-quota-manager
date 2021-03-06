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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.sciserver.quota.manager.Config.RootVolume;
import org.sciserver.quota.manager.dto.Quota;
import org.sciserver.quota.manager.dto.VolumeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuotaManagerController {
	private static final String RELATIVE_PATH_PATTERN = "{keystoneId}/{userVolumeName}";

	private final FileSystemModule fileSystemModule;
	private final Config config;

	@Autowired
	public QuotaManagerController(Config config, FileSystemModule fileSystemModule) {
		this.config = config;
		this.fileSystemModule = fileSystemModule;
	}

	/**
	 * Do any and all setup required for creating a volume
	 * given a root folder and a relative path.
	 * @param path
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("createVolume")
	public void createVolume(@RequestBody VolumeDTO newVolumeInfo) throws IOException {
		PathMatcher matcher = new AntPathMatcher();
		Map<String, String> pathVariables = matcher.extractUriTemplateVariables(
				RELATIVE_PATH_PATTERN, newVolumeInfo.getRelativePath());
		RootVolume rv = config.getRootVolumes().computeIfAbsent(newVolumeInfo.getRootVolumeName(),
				key -> {
					throw new UnknownVolumeNameException("Unknown root volume "
							+ newVolumeInfo.getRootVolumeName());
				});

		Path userFolder = Paths.get(rv.getPathOnFileServer(), pathVariables.get("keystoneId"));
		Path userVolumeFolder = userFolder.resolve(pathVariables.get("userVolumeName"));

		Files.createDirectories(userVolumeFolder);
		Files.setPosixFilePermissions(userVolumeFolder,
				PosixFilePermissions.fromString("rwxrwxrwx"));

		if (rv.getPerUserQuota() != 0) {
			fileSystemModule.setQuota(
					userFolder.toString(),
					rv.getPerUserQuota());
		}
		if (rv.getPerVolumeQuota() != 0) {
			fileSystemModule.setQuota(
					userVolumeFolder.toString(),
					rv.getPerVolumeQuota());
		}
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("deleteVolume")
	public void deleteVolume(@RequestBody VolumeDTO newVolumeInfo) throws IOException {
		PathMatcher matcher = new AntPathMatcher();
		Map<String, String> pathVariables = matcher.extractUriTemplateVariables(
				RELATIVE_PATH_PATTERN, newVolumeInfo.getRelativePath());
		RootVolume rv = config.getRootVolumes().computeIfAbsent(newVolumeInfo.getRootVolumeName(),
				key -> {
					throw new UnknownVolumeNameException("Unknown root volume "
							+ newVolumeInfo.getRootVolumeName());
				});

		Path userFolder = Paths.get(rv.getPathOnFileServer(), pathVariables.get("keystoneId"));
		Path userVolumeFolder = userFolder.resolve(pathVariables.get("userVolumeName"));

		if (rv.getPerVolumeQuota() != 0) {
			fileSystemModule.removeUserVolumeWithQuota(
					userVolumeFolder.toString());
		} else {
			FileUtils.deleteDirectory(userVolumeFolder.toFile());
		}
	}

	@GetMapping("getUsage")
	public Collection<Quota> getUsage() throws IOException {
		return fileSystemModule.getUsage();
	}
}
