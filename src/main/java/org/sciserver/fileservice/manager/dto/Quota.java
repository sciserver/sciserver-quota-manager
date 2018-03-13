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
package org.sciserver.fileservice.manager.dto;

public class Quota {
	private final String rootVolumeName;
	private final String relativePath;
	private final long numberOfFilesUsed;
	private final long numberOfFilesQuota;
	private final long numberOfBytesUsed;
	private final long numberOfBytesQuota;

	public Quota(String rootVolumeName, String relativePath, long numberOfFilesUsed, long numberOfFilesQuota,
			long numberOfBytesUsed, long numberOfBytesQuota) {
		this.rootVolumeName = rootVolumeName;
		this.relativePath = relativePath;
		this.numberOfFilesUsed = numberOfFilesUsed;
		this.numberOfFilesQuota = numberOfFilesQuota;
		this.numberOfBytesUsed = numberOfBytesUsed;
		this.numberOfBytesQuota = numberOfBytesQuota;
	}
	public String getRootVolumeName() {
		return rootVolumeName;
	}
	public String getRelativePath() {
		return relativePath;
	}
	public long getNumberOfFilesUsed() {
		return numberOfFilesUsed;
	}
	public long getNumberOfFilesQuota() {
		return numberOfFilesQuota;
	}
	public long getNumberOfBytesUsed() {
		return numberOfBytesUsed;
	}
	public long getNumberOfBytesQuota() {
		return numberOfBytesQuota;
	}
	@Override
	public String toString() {
		return "Quota [rootVolumeName=" + rootVolumeName + ", relativePath=" + relativePath + ", numberOfFilesUsed="
				+ numberOfFilesUsed + ", numberOfFilesQuota=" + numberOfFilesQuota + ", numberOfBytesUsed="
				+ numberOfBytesUsed + ", numberOfBytesQuota=" + numberOfBytesQuota + "]";
	}
}
