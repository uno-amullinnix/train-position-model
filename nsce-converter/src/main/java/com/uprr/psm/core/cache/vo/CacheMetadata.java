package com.uprr.psm.core.cache.vo;

public class CacheMetadata {
	
	private String cacheName;
	private int cacheCount;
	
	public String getCacheName() {
		return cacheName;
	}
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
	public int getCacheCount() {
		return cacheCount;
	}
	public void setCacheCount(int cacheCount) {
		this.cacheCount = cacheCount;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CacheMetadata [cacheName=");
		sb.append(cacheName);
		sb.append(", cacheCount=");
		sb.append(cacheCount);
		sb.append("]");
		return sb.toString();
	}
}
