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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuotaApplier {
    private final Logger logger = LoggerFactory.getLogger(QuotaApplier.class);
    private final Config config;
    private final FileSystemModule fileSystemModule;

    public QuotaApplier(Config config, FileSystemModule fileSystemModule) {
        this.fileSystemModule = fileSystemModule;
        this.config = config;
    }

    public void applyQuotas() {
        logger.info("[Re-]applying quotas");
        config.getRootVolumes().entrySet().stream()
            .forEach(rvEntry -> {
                String rootVolumePath = rvEntry.getValue().getPathOnFileServer();
                Path rootVolumeAsPath = Paths.get(rootVolumePath);
                if (!Files.isDirectory(rootVolumeAsPath)) {
                    return;
                }
                try {
                    Files.walk(rootVolumeAsPath, 2)
                        .filter(folder -> !folder.equals(rootVolumeAsPath))
                        .forEach(folder -> {
                            Path relativePath = rootVolumeAsPath.relativize(folder);
                            String folderFullName = folder.toAbsolutePath().toString();
                            if (relativePath.getNameCount() == 1 && rvEntry.getValue().getPerUserQuota() > 0) {
                                fileSystemModule.setQuota(folderFullName, rvEntry.getValue().getPerUserQuota());
                            }
                            if (relativePath.getNameCount() == 2 && rvEntry.getValue().getPerVolumeQuota() > 0 ) {
                                fileSystemModule.setQuota(folderFullName, rvEntry.getValue().getPerVolumeQuota());
                            }
                        });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }
}
