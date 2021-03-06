package oss.marunowskia.datamining.transactionalcausalinference.utilities;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import oss.marunowskia.datamining.transactionalcausalinference.models.TraceData;
import oss.marunowskia.datamining.transactionalcausalinference.utilities.transactions.TraceDataReader;

public class DatabaseUtililty {
	
	public DatabaseUtililty() {
		
	}
	@PersistenceContext(name="research", unitName="research")
	EntityManager em;
	
	Logger logger = Logger.getLogger(DatabaseUtililty.class);


	public Hashtable<String, String> getConfig() {
		Query query = em.createNativeQuery("SELECT CONFIG_NAME, CONFIG_VALUE FROM RESEARCH..TRANSACTIONAL_CAUSAL_INFERENCE_CONFIG");
		List<Object[]> queryResults = (List<Object[]>)query.getResultList();
		Hashtable<String, String> result = new Hashtable<String, String>();
		for(Object[] configRow : queryResults) {
			result.put((String)configRow[0], (String)configRow[1]);
		}
		return result;
	}
	
	private List<TraceData> fetchSortedTraceDataBatch(Date minimumStartDate) {
		logger.info("Fetching next batch of TraceData rows");
		
		if(minimumStartDate == null) {
			minimumStartDate = new Date(0);
		}
		
		// If this next line isn't here, the entity manager retains an object reference to all the objects it has returned previously. This ensures that previous results are able to be garbage collected.
		// TODO: Figure out if there is a nicer way to avoid the memory overhead associated with the entity manager.
		// TODO: I suppose I could use an EntityManagerFactory, then get a new entity manager for every fetch... 
		em.clear();
		
		
		Query query = em.createNamedQuery("FetchTraceData");
		query.setParameter("minimum_start_date", minimumStartDate);
		return query.getResultList();
	}
	
	public void fetchSortedTraceDataBatches(TraceDataReader traceDataReader) {		
		
		boolean additionalDataAvailable = true;
		while(additionalDataAvailable) {
			long startTime = System.currentTimeMillis();
			List<TraceData> sortedTraceData = fetchSortedTraceDataBatch(traceDataReader.getLatestObservedDate());
			logger.info("Have " + sortedTraceData.size() + " TraceData records to process.");
			if(sortedTraceData == null || sortedTraceData.isEmpty()) {
				additionalDataAvailable = false;
			}
			else {
				traceDataReader.readTraceData(sortedTraceData);
			}
		}
	}
}
