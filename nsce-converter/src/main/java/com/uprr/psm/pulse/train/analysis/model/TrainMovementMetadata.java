package com.uprr.psm.pulse.train.analysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uprr.psm.core.cache.vo.CacheMetadata;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value = {"cacheName", "cacheCount"})
public class TrainMovementMetadata extends CacheMetadata {

	private String name;
	
	private Integer totalSubdivisions;

	private String trainId;

	private String locomotiveId;
	
	private Integer currentSubdivisionId;

	private String triggeringEvent;
		
	private boolean isValidReasonCode;

	private String eventTimestamp;
	
	private String messageSource;
	
	private String correlationId;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}	
	
	public Integer getTotalSubdivisions() {
		return totalSubdivisions;
	}

	public void setTotalSubdivisions(Integer totalSubdivisions) {
		this.totalSubdivisions = totalSubdivisions;
	}

	public String getTrainId() {
		return trainId;
	}

	public void setTrainId(String trainId) {
		this.trainId = trainId;
	}

	public String getLocomotiveId() {
		return locomotiveId;
	}

	public void setLocomotiveId(String locomotiveId) {
		this.locomotiveId = locomotiveId;
	}

	public Integer getCurrentSubdivisionId() {
		return currentSubdivisionId;
	}

	public void setCurrentSubdivisionId(Integer currentSubdivisionId) {
		this.currentSubdivisionId = currentSubdivisionId;
	}

	public String getTriggeringEvent() {
		return triggeringEvent;
	}

	public void setTriggeringEvent(String triggeringEvent) {
		this.triggeringEvent = triggeringEvent;
	}
	
	@JsonProperty("isValidReasonCode")
	public boolean isValidReasonCode() {
		return isValidReasonCode;
	}

	public void setValidReasonCode(boolean isValidReasonCode) {
		this.isValidReasonCode = isValidReasonCode;
	}

	public String getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(String eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}
	
	public String getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(String messageSource) {
		this.messageSource = messageSource;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TrainMovementMetadata [name=");
		sb.append(name);
		sb.append(", totalSubdivisions=");
		sb.append(totalSubdivisions);
		sb.append(", trainId=");
		sb.append(trainId);
		sb.append(", locomotiveId=");
		sb.append(locomotiveId);
		sb.append(", currentSubdivisionId=");
		sb.append(currentSubdivisionId);
		sb.append(", triggeringEvent=");
		sb.append(triggeringEvent);
		sb.append(", isValidReasonCode=");
		sb.append(isValidReasonCode);
		sb.append(", eventTimestamp=");
		sb.append(eventTimestamp);
		sb.append(", messageSource=");
		sb.append(messageSource);
		sb.append(", correlationId=");
		sb.append(correlationId);
		sb.append("]");
		return sb.toString();
	}
}
