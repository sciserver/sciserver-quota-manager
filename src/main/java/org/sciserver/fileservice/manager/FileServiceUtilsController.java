package org.sciserver.fileservice.manager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.sciserver.fileservice.manager.Config.RootVolume;
import org.sciserver.fileservice.manager.dto.CreateVolumeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileServiceUtilsController {
	private static final String RELATIVE_PATH_PATTERN = "{keystoneId}/{userVolumeName}";

	private final FileServiceModule fileServiceModule;
	private final Config config;

	@Autowired
	public FileServiceUtilsController(Config config, FileServiceModule fileServiceModule) {
		this.config = config;
		this.fileServiceModule = fileServiceModule;
	}

	/**
	 * Do any and all setup required for creating a volume
	 * given a root folder and a relative path.
	 * @param path
	 * @throws IOException
	 */
	@PostMapping("afterCreateVolume")
	public void afterCreateVolume(@RequestBody CreateVolumeDTO newVolumeInfo) {
		PathMatcher matcher = new AntPathMatcher();
		Map<String, String> pathVariables = matcher.extractUriTemplateVariables(
				RELATIVE_PATH_PATTERN, newVolumeInfo.getRelativePath());
		RootVolume rv = config.getRootVolumes().computeIfAbsent(newVolumeInfo.getRootVolumeName(),
				key -> {
					throw new UnknownVolumeNameException("Unknown root volume "
							+ newVolumeInfo.getRootVolumeName());
				});

		if (rv.getPerUserQuota() != 0) {
			fileServiceModule.setQuota(
					Paths.get(rv.getPathOnFileServer(), pathVariables.get("keystoneId")).toString(),
					rv.getPerUserQuota());
		}
		if (rv.getPerVolumeQuota() != 0) {
			fileServiceModule.setQuota(
					Paths.get(rv.getPathOnFileServer(),
							pathVariables.get("keystoneId"),
							pathVariables.get("userVolumeName")).toString(),
					rv.getPerVolumeQuota());
		}
	}
}
