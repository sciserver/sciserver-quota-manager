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
package org.sciserver.quota.manager.xfs;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.sciserver.quota.manager.Config;
import org.sciserver.quota.manager.FileServiceModule;
import org.sciserver.quota.manager.dto.Quota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Component
@Profile("xfs")
public class XFSFileServiceModule implements FileServiceModule {
	private final Logger logger = LoggerFactory.getLogger(XFSFileServiceModule.class);
	/* Note that despite this limit, the java code in this class
	 * assumes that we can index on the project id, i.e., that
	 * the project id's are less then Integer.MAX_VALUE (2^31)
	 */
	private static final long MAX_PROJECT_ID = 2^32 - 2;
	private static final long MIN_PROJECT_ID = 1;
	private static final Path PROJECTS_FILE = Paths.get("/etc/projects");
	private static final Path PROJIDS_FILE = Paths.get("/etc/projid");

	private final Config config;
	private final XFSConfig xfsConfig;
	private final DefaultExecutor quotaExecutor;

	public XFSFileServiceModule(Config config, XFSConfig xfsConfig) {
		this.config = config;
		this.xfsConfig = xfsConfig;
		quotaExecutor = new DefaultExecutor();
		quotaExecutor.setStreamHandler(
				new PumpStreamHandler(new LogOutputStream() {
					@Override
					protected void processLine(String line, int logLevel) {
						logger.info("[xfs_quota] " + line);
					}
				},
				new LogOutputStream() {
					@Override
					protected void processLine(String line, int logLevel) {
						logger.error("[xfs_quota] " + line);
					}
				}));
	}

	@Override
	@Async("xfsEditProjectsExecutor")
	public void setQuota(String filePath, long numberOfBytes) {
		try {
			Map<String, Long> pathsToProjectIds = getXFSProjects();

			long projectId;
			if (pathsToProjectIds.containsKey(filePath)) {
				logger.info(
						"Updating quota on {} (with project id={}) to {} bytes",
						filePath,
						pathsToProjectIds.get(filePath),
						numberOfBytes);
				projectId = pathsToProjectIds.get(filePath);
			} else {
				projectId = firstFreeId(pathsToProjectIds);
				logger.info(
						"Creating new XFS project {} on {} with {} bytes",
						projectId,
						filePath,
						numberOfBytes);
				Files.write(
						PROJECTS_FILE,
						String.format("%d:%s\n", projectId, filePath).getBytes(),
						StandardOpenOption.APPEND);
				Files.write(
						PROJIDS_FILE,
						String.format("%s:%d\n", filePath, projectId).getBytes(),
						StandardOpenOption.APPEND);

				quotaExecutor.execute(new CommandLine("sudo")
						.addArgument("xfs_quota")
						.addArgument("-xc")
						.addArgument(String.format("project -s %d", projectId), false));
			}

			quotaExecutor.execute(new CommandLine("sudo")
					.addArgument("xfs_quota")
					.addArgument("-xc")
					.addArgument(String.format("limit -p bhard=%d %d", numberOfBytes, projectId), false)
					.addArguments(splitIfPossible(xfsConfig.getMountedFileSystems())));

		} catch (Exception e) {
			logger.error(
					"Error setting quota {} on {}",
					numberOfBytes,
					filePath,
					e);
		}
	}

	@Override
	@Async("xfsEditProjectsExecutor")
	public void removeUserVolumeWithQuota(String filePath) {
		try {
			logger.info("Removing " + filePath + " from XFS project files");

			Map<String, Long> pathsToProjectIds = getXFSProjects();
			quotaExecutor.execute(new CommandLine("sudo")
					.addArgument("xfs_quota")
					.addArgument("-xc")
					.addArgument(String.format(
							"limit -p bhard=%d %d", 0, pathsToProjectIds.get(filePath)), false)
					.addArguments(splitIfPossible(xfsConfig.getMountedFileSystems())));

			List<String> projidFileLines = FileUtils.readLines(
					PROJIDS_FILE.toFile(), Charset.defaultCharset());
			List<String> updatedProjidFileLines = projidFileLines
					 .stream()
					 .filter(s -> !s.contains(filePath + ":"))
					 .collect(Collectors.toList());
			FileUtils.writeLines(PROJIDS_FILE.toFile(), updatedProjidFileLines);

			List<String> projectFileLines = FileUtils.readLines(
					PROJECTS_FILE.toFile(), Charset.defaultCharset());
			List<String> updatedProjectFileLines = projectFileLines
					 .stream()
					 .filter(s -> !s.contains(":" + filePath))
					 .collect(Collectors.toList());
			FileUtils.writeLines(PROJECTS_FILE.toFile(), updatedProjectFileLines);

			FileUtils.deleteDirectory(new File(filePath));
		} catch (Exception e) {
			logger.error(
					"Error remove quota on {}",
					filePath,
					e);
		}
	}

	@Override
	public Collection<Quota> getUsage() throws ExecuteException, IOException {
		Map<String, Map<String, QuotaReportLine>> collectedQuotaOutput = new HashMap<>();

		logAndRun("bytes", collectedQuotaOutput, new CommandLine("sudo")
				.addArgument("xfs_quota")
				.addArgument("-xc")
				.addArgument("report -Np", false)
				.addArguments(splitIfPossible(xfsConfig.getMountedFileSystems())));
		logAndRun("files", collectedQuotaOutput, new CommandLine("sudo")
				.addArgument("xfs_quota")
				.addArgument("-xc")
				.addArgument("report -Ni", false)
				.addArguments(splitIfPossible(xfsConfig.getMountedFileSystems())));

		return collectedQuotaOutput.entrySet()
			.stream()
			.map(folderEntry ->
				config.getRootVolumes().entrySet().stream()
					.filter(e -> folderEntry.getKey().startsWith(e.getValue().getPathOnFileServer()))
					.findAny()
					.map(rvEntry ->
						new Quota(
								rvEntry.getKey(),
								folderEntry.getKey().replaceFirst("^"+rvEntry.getValue().getPathOnFileServer()+"/", ""),
								folderEntry.getValue().get("files").getUsed(),
								folderEntry.getValue().get("files").getHardLimit(),
								// xfs_quota reports these in kilobytes
								folderEntry.getValue().get("bytes").getUsed() * 1024,
								folderEntry.getValue().get("bytes").getHardLimit() * 1024)
					))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	private void logAndRun(String label, Map<String, Map<String, QuotaReportLine>> outputHolder, CommandLine cmdLine) throws ExecuteException, IOException {
		DefaultExecutor executor = new DefaultExecutor();
		executor.setStreamHandler(saveLines(label, outputHolder));
		logger.trace("Running command: " + cmdLine.toString());
		executor.execute(cmdLine);
	}

	// Helper method to split a string or pass on a null
	// Equivalent to Kotlin's line?.split("\\s+")
	private String[] splitIfPossible(String line) {
		if (line == null) return null;
		return line.split("\\s+");
	}

	private ExecuteStreamHandler saveLines(String label, Map<String, Map<String, QuotaReportLine>> outputHolder) {
		return new PumpStreamHandler(new LogOutputStream() {
			@Override
			protected void processLine(String line, int logLevel) {
				if (StringUtils.isEmpty(line)) return;
				logger.trace("[xfs_quota] " + line);
				String[] quotaComponents = line.split("\\s+");
				outputHolder.computeIfAbsent(quotaComponents[0], (s) -> new HashMap<>());
				outputHolder.get(quotaComponents[0]).put(label, new QuotaReportLine(line));
			}
		},
		new LogOutputStream() {
			@Override
			protected void processLine(String line, int logLevel) {
				logger.error("[xfs_quota] " + line);
			}
		});
	}

	private Map<String, Long> getXFSProjects() throws IOException {
		CsvSchema schema = CsvSchema.builder()
				.addColumn("projectId", CsvSchema.ColumnType.NUMBER)
				.addColumn("path")
				.setColumnSeparator(':')
				.build();
		CsvMapper mapper = new CsvMapper();
		Map<String, Long> pathsToProjectIds = new HashMap<>();

		try (Reader input = Files.newBufferedReader(PROJECTS_FILE)) {
			MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
					.with(schema)
					.readValues(input);
			while(it.hasNext()) {
				Map<String, String> rowAsMap = it.next();
				// skip trailing new lines or empty lines
				if (StringUtils.isEmpty(rowAsMap.get("path")) && StringUtils.isEmpty(rowAsMap.get("projectId"))) {
					continue;
				}
				pathsToProjectIds.put(rowAsMap.get("path"), Long.parseLong(rowAsMap.get("projectId")));
			}
		}
		return pathsToProjectIds;
	}

	private long firstFreeId(Map<String, Long> pathsToProjectIds) {
		List<Long> ids = new ArrayList<>(pathsToProjectIds.values());
		Collections.sort(ids);
		int numberOfIds = ids.size();
		for(long i=0; i < MAX_PROJECT_ID; ++i) {
			if (i >= numberOfIds || !ids.get((int) i).equals(i+MIN_PROJECT_ID)) {
				return i+MIN_PROJECT_ID;
			}
		}
		throw new IllegalStateException("There appears to be too many assigned projects");
	}
}
