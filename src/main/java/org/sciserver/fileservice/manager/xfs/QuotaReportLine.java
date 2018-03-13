package org.sciserver.fileservice.manager.xfs;

public class QuotaReportLine {
	/* Project ID   Used   Soft   Hard Warn/Grace
	 * Project IDs are set to simply be the full path
	 */
	private final String fullPath;
	private final long used;
	private final long hardLimit;

	public QuotaReportLine(String line) {
		String[] lineComponents = line.split("\\s+");
		fullPath = lineComponents[0];
		used = Long.parseLong(lineComponents[1]);
		hardLimit = Long.parseLong(lineComponents[3]);
	}

	public String getFullPath() {
		return fullPath;
	}

	public long getUsed() {
		return used;
	}

	public long getHardLimit() {
		return hardLimit;
	}
}
