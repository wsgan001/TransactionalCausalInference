package oss.marunowskia.datamining.transactionalcausalinference.utilities.transactions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import oss.marunowskia.datamining.transactionalcausalinference.models.TraceData;
import oss.marunowskia.datamining.transactionalcausalinference.models.Transaction;
import oss.marunowskia.datamining.transactionalcausalinference.models.TransactionBoundary;

public class TransactionScanner implements TraceDataReader{

	private Logger logger = Logger.getLogger(TransactionScanner.class);
	
	private TreeSet<TransactionBoundary> sortedEndBoundaries = new TreeSet<TransactionBoundary>(TransactionBoundaryComparator.COMPARATOR);
	private TransactionSequenceAnalyzer transactionSequenceAnalyzer;
	private Date previousDate;

	public TransactionScanner(Hashtable<String, String> config) {
		transactionSequenceAnalyzer = new TransactionSequenceAnalyzer(config);
	}

	@Override
	public void readTraceData(List<TraceData> traceDataList) {
		// Note, traceDataList must be sorted by startDate
		for(TraceData traceData : traceDataList) {
			
			if(duplicateTraceDataCheck(traceData)) {
				continue;
			}
			
			Transaction transaction = createTransaction(traceData);
			sortedEndBoundaries.add(transaction.getEndBoundary());
			Date traceDataStart = traceData.getStartTime();
			// This helps us create a well-ordered event replay sequence.
			while(traceDataStart.after(sortedEndBoundaries.first().getDate())) {
				processTransactionBoundary(sortedEndBoundaries.pollFirst());
			}
			processTransactionBoundary(transaction.getStartBoundary());
		}
		transactionSequenceAnalyzer.reportAssociationRules();
		
	}
	
	

	@Override
	public Date getLatestObservedDate() {
		return previousDate;
	}
	
	
	public void processTransactionBoundary(TransactionBoundary transactionBoundary) {
		// NOTE: This is exposed as public, so this algorithm can be used on a live trace that returns data even before transactions have completed...
		// rather than only using pre-recorded traces.
		enforceMonotonicity(transactionBoundary.getDate());
		transactionSequenceAnalyzer.analalyzeTransactionBoundary(transactionBoundary);
	}


	private TreeSet<Long> observedRowNumbers = new TreeSet<Long>();
	private boolean duplicateTraceDataCheck(TraceData traceData) {
		// It is possible for our query to return the same database-record more than once, if multiple transactions start at the same time.
		// This method provides a way to avoid this (tiny) source of error.
		if(traceData.getStartTime().equals(previousDate)) {
			if(!observedRowNumbers.contains(traceData.getRowNumber())) {
				observedRowNumbers.add(traceData.getRowNumber());
			}
			else 
				return true;
		}
		else {
			observedRowNumbers.clear();
		}
		return false;
	}
	
	private void enforceMonotonicity(Date newDate) {
		if(previousDate != null) {
			if(newDate.before(previousDate)) {
				throw new IllegalArgumentException("Non-monotonic date observed on a TransactionBoundary");
			}
		}
		previousDate = newDate; 
	}
	
	// These methods could reasonably be moved to a utility class
	private static Transaction createTransaction(TraceData traceData) {
		Transaction transaction = new Transaction();
		transaction.setStartDate(traceData.getStartTime());
		transaction.setEndDate(traceData.getEndTime());
		transaction.setOptionalTextInformation(traceData.getTextData());
		transaction.setTransactionId(traceData.getRowNumber());
		transaction.setTransactionType(QueryTextClassifier.classifyQuery(traceData.getTextData()));
		transaction.setDuration(traceData.getDuration());
		return transaction;
	}
	
	public void reportResults() {
		transactionSequenceAnalyzer.reportAssociationRules();
	}

}
