package oss.marunowskia.datamining.transactionalcausalinference.utilities.transactions;

import java.util.Date;
import java.util.List;

import oss.marunowskia.datamining.transactionalcausalinference.models.TraceData;

public interface TraceDataReader {
	public void readTraceData(List<TraceData> traceData);
	public Date getLatestObservedDate();
}
