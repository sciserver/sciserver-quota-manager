package org.sciserver.fileservice.manager;

public interface FileServiceModule {
	public void setQuota(String filePath, long numberOfBytes);
}
