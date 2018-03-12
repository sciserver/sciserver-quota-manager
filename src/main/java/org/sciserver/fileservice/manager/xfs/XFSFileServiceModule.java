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
package org.sciserver.fileservice.manager.xfs;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sciserver.fileservice.manager.FileServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
	private static final Path PROJECTS_FILE = Paths.get("/etc/projects");

	@Override
	@Async
	public void setQuota(String filePath, long numberOfBytes) {
		try {
			logger.info("Setting quota to {} bytes on {}", numberOfBytes, filePath);
			Map<String, Long> pathsToProjectIds = getXFSProjects();

			if (pathsToProjectIds.containsKey(filePath)) {
				logger.info(
						"Updating quota on {} (with project id={}) to {} bytes",
						filePath,
						pathsToProjectIds.get(filePath),
						numberOfBytes);
			} else {
				logger.info(
						"Creating new XFS project {} on {} with {} bytes",
						firstFreeId(pathsToProjectIds),
						filePath,
						numberOfBytes);
			}
		} catch (Exception e) {
			logger.error(
					"Error setting quota {} on {}",
					numberOfBytes,
					filePath,
					e);
		}
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
				pathsToProjectIds.put(rowAsMap.get("path"), Long.parseLong(rowAsMap.get("projectId")));
			}
		}
		return pathsToProjectIds;
	}

	private long firstFreeId(Map<String, Long> pathsToProjectIds) {
		List<Long> ids = new ArrayList<>(pathsToProjectIds.values());
		Collections.sort(ids);
		for(long i=0; i < MAX_PROJECT_ID; ++i) {
			if (!ids.get((int) i).equals(i)) {
				return i;
			}
		}
		throw new IllegalStateException("There appears to be too many assigned projects");
	}
}
