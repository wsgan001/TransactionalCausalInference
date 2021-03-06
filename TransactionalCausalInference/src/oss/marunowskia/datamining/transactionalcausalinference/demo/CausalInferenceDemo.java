package oss.marunowskia.datamining.transactionalcausalinference.demo;

import org.jboss.logging.Logger;

public class CausalInferenceDemo {

	Logger logger = Logger.getLogger(CausalInferenceDemo.class);
	
	public void runDemo() {
		logger.info("Starting TransactionalCausalInferenceDemo.");
		
		
		// Demo 1. Uninterrupted sequences
		int demoSequenceLength = 100000;
		FixedSequenceDemo fixedSequenceDemo = new FixedSequenceDemo();
		fixedSequenceDemo.setFixedSequenceProbability(0.1);
		fixedSequenceDemo.setInterruptionProbability(0);
		fixedSequenceDemo.setNumberOfEventTypes(300);
		logger.info("Running unoptimized fixed-sequence-detection demo");
		fixedSequenceDemo.runDemo(demoSequenceLength);
		
		logger.info("Running optimized fixed-sequence-detection demo");
		fixedSequenceDemo.optimizeConfiguration(demoSequenceLength);
		fixedSequenceDemo.runDemo(demoSequenceLength);
		
		
		
		
		// Demo 2. Interrupted sequences
		fixedSequenceDemo = new FixedSequenceDemo();
		fixedSequenceDemo.setFixedSequenceProbability(0.1);
		fixedSequenceDemo.setInterruptionProbability(0.8);
		fixedSequenceDemo.setNumberOfEventTypes(300);
		
		logger.info("Running unoptimized fixed-sequence-detection demo with sequence interruptions");
		fixedSequenceDemo.runDemo(demoSequenceLength);
		
		logger.info("Running optimized fixed-sequence-detection demo with sequence interruptions");
		fixedSequenceDemo.optimizeConfiguration(demoSequenceLength);
		fixedSequenceDemo.runDemo(demoSequenceLength);

//		new MarkovModelDemo().runDemo();
//		new TransactionalDemo().runDemo();
//		new NoisyTransactionalDemo().runDemo();
	}
}
