package com.uprr.psm.core.cache.vo;

import java.util.*;

import com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.SubdivisionStateData;
import com.uprr.psm.pulse.train.analysis.model.TrainMovementMetadata;

public class TrainCacheObjects {
	
	private TrainMovementMetadata metadata;
	private List<SubdivisionStateData> data;
	
	public TrainMovementMetadata getMetadata() {
		return metadata;
	}
	public void setMetadata(TrainMovementMetadata metadata) {
		this.metadata = metadata;
	}
	public List<SubdivisionStateData> getData() {
		return data;
	}
	public void setData(List<SubdivisionStateData> data) {
		this.data = data;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CacheObjects [metadata=");
		sb.append(metadata);
		sb.append(", data=");
		sb.append(data);
		sb.append("]");
		return sb.toString();
	}
}
