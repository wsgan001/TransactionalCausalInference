package oss.marunowskia.datamining.transactionalcausalinference.models;

import java.util.Date;

import oss.marunowskia.datamining.transactionalcausalinference.models.TransactionBoundary.EventType;

public class Transaction {
	
	
	private long transactionType;
	private long transactionId;
	
	private Date startDate;
	private Date endDate;
	
	private TransactionBoundary startBoundary;
	private TransactionBoundary endBoundary;

	private long duration;
	private boolean durationLongerThanNormal;
	
	private String optionalTextInformation;
	
	public long getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(long transactionType) {
		this.transactionType = transactionType;
	}
	public long getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public boolean isDurationLongerThanNormal() {
		return durationLongerThanNormal;
	}
	public void setDurationLongerThanNormal(boolean durationLongerThanNormal) {
		this.durationLongerThanNormal = durationLongerThanNormal;
	}
	public TransactionBoundary getStartBoundary() {
		if(startBoundary == null) {
			startBoundary = new TransactionBoundary();
			startBoundary.setDate(getStartDate());
			startBoundary.setTransaction(this);
			startBoundary.setEventType(EventType.TRANSACTION_START);
		}
		return startBoundary;
	}
	public void setStartBoundary(TransactionBoundary startBoundary) {
		this.startBoundary = startBoundary;
	}
	public TransactionBoundary getEndBoundary() {
		if(endBoundary == null) {
			endBoundary = new TransactionBoundary();
			endBoundary.setDate(getEndDate());
			endBoundary.setTransaction(this);
			endBoundary.setEventType(EventType.TRANSACTION_END);
		}
		return endBoundary;
	}
	public void setEndBoundary(TransactionBoundary endBoundary) {
		this.endBoundary = endBoundary;
	}
	public String getOptionalTextInformation() {
		return optionalTextInformation;
	}
	public void setOptionalTextInformation(String optionalTextInformation) {
		this.optionalTextInformation = optionalTextInformation;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	
	
	
}