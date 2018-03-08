package org.sciserver.fileservice.manager.xfs;

import org.sciserver.fileservice.manager.FileServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Profile("xfs")
public class XFSFileServiceModule implements FileServiceModule {
	private final Logger logger = LoggerFactory.getLogger(XFSFileServiceModule.class);

	@Override
	@Async
	public void setQuota(String filePath, long numberOfBytes) {
		logger.info("Setting quota to {} bytes on {}", numberOfBytes, filePath);
	}
}
