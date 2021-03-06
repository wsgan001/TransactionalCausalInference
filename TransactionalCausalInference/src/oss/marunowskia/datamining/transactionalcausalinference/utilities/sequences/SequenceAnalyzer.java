package oss.marunowskia.datamining.transactionalcausalinference.utilities.sequences;

import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import oss.marunowskia.datamining.transactionalcausalinference.models.AssociationRuleNode;
import oss.marunowskia.datamining.transactionalcausalinference.models.SequenceNode;

/**
 * A SequenceAnalyzer takes a sequence of objects, each of which is associated with an "event type", 
 * and identifies associations between the sequence of events and the class label applied to those events.
 * 
 * These patterns are stored as "AssociationRuleNodes"
 * @author Alex
 *
 */
public class SequenceAnalyzer {

	private Logger logger = Logger.getLogger(SequenceAnalyzer.class);

	private Hashtable<Long, AssociationRuleNode> associationRuleRoots = new Hashtable<Long, AssociationRuleNode>();
	private Hashtable<Object, SequenceNode> nodeLookup = new Hashtable<Object, SequenceNode>();

	// This LinkedList is used for tracking the head SequenceNode, tail SequenceNode, and depth with very little code
	// SequenceNode is used simply for convenient recursive traversal through the sequence, without having to deal with iterators or indices
	private LinkedList<SequenceNode> sequence = new LinkedList<>();


	// Useful for debugging
	private long numberOfRules = 0;

	// Config values
	private int ruleDepthLimit;
	private int supportThreshold;
	private int historyLimit;

	public SequenceAnalyzer() {
		// Reasonable default values...
		historyLimit = 50;
		ruleDepthLimit = 10;
		supportThreshold = 200;
	}

	public SequenceAnalyzer(Hashtable<String, String> config) {
		historyLimit = safeGetInt(config, "HISTORY_LIMIT");
		ruleDepthLimit = safeGetInt(config, "RULE_DEPTH_LIMIT");
		supportThreshold = safeGetInt(config, "SUPPORT_THRESHOLD");
	}

	// Public methods
	public void recordEvent(Object eventSource, long eventType) {
		SequenceNode sequenceNode = new SequenceNode();
		sequenceNode.setNodeType(eventType);
		sequenceNode.setEventSource(eventSource);
		recordSequenceNode(sequenceNode);
	}

	public void analyzeEvent(Object eventSource, boolean eventLabel) {
		SequenceNode correspondingSequenceNode = nodeLookup.get(eventSource);
		if(correspondingSequenceNode == null) {
			return;
		}
		analyzeSequenceNode(correspondingSequenceNode, eventLabel);
	}

	public void reportAssociationRules() {
		AssociationRuleReportingUtility.reportAssociationRuleStatistics(associationRuleRoots, supportThreshold);
	}

	public int getHistoryLimit() {
			return historyLimit;
	}

	public void setHistoryLimit(int historyLimit) {
		this.historyLimit = historyLimit;
	}

	public int getRuleDepthLimit() {
		return ruleDepthLimit;
	}

	public void setRuleDepthLimit(int ruleDepthLimit) {
		this.ruleDepthLimit = ruleDepthLimit;
	}

	public int getSupportThreshold() {
		return supportThreshold;
	}

	public void setSupportThreshold(int supportThreshold) {
		this.supportThreshold = supportThreshold;
	}

	// Private methods
	private int safeGetInt(Hashtable<String, String> configTable, String configName) {
		if(!configTable.containsKey(configName)) {
			throw new IllegalArgumentException("Missing config row: " + configName);
		}
		try {
			return Integer.parseInt(configTable.get(configName));
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Invalid integer format for " + configName + ": " + configTable.get(configName));
		}
	}

	private void recordSequenceNode(SequenceNode sequenceNode) {

		sequenceNode.setPreviousNode(sequence.peekFirst());
		sequence.addFirst(sequenceNode);
		nodeLookup.put(sequenceNode.getEventSource(), sequenceNode);

		// Remove oldest events from our list until we have a sufficiently small list
		while(sequence.size() > getHistoryLimit()) {
			SequenceNode lastNode = sequence.removeLast();
			nodeLookup.remove(lastNode.getEventSource(), lastNode);
		}

		// Remove a single back-reference so that all the removed sequenceNodes can be garbage collected.
		SequenceNode tail = sequence.peekLast();
		if(tail!=null) {
			tail.setPreviousNode(null);
		}
	}

	/**
	 * Explores all reasonable subsequences that start from sequenceNode. This should only be called once per event.
	 * @param sequenceNode
	 * @param eventLabel
	 */
	private void analyzeSequenceNode(SequenceNode sequenceNode, boolean eventLabel) {
		AssociationRuleNode associationRuleNode = getRootAssociationRuleNode(sequenceNode.getEventType());
		depthFirstSequenceTraversal(sequenceNode, associationRuleNode, eventLabel, sequenceNode.getEventSource());
	}


	private SequenceNode depthFirstSequenceTraversal(SequenceNode startingPoint, AssociationRuleNode currentRuleNode, boolean positiveExample, Object eventSource) {

		validateDepthFirstSearchTraversalParameters(currentRuleNode, eventSource);

		if(startingPoint == null) {
			return null;
		}

		if(currentRuleNode.incrementSupport(positiveExample, eventSource ) == -1) {
			// This will occur if "currentRuleNode" has already been modified as a result of "eventSource"
			return null;
		}

		// Check the support threshold and depth limit to make sure we don't waste time/memory exploring nodes that aren't interesting
		if(currentRuleNode.getSupport() >= getSupportThreshold() && currentRuleNode.getDepth() < getRuleDepthLimit()) {
			SequenceNode nextNodeToExpand = startingPoint.getPreviousNode();
			// Once this hits a null, we have finished exploring all desired subsequences of this node.
			while(nextNodeToExpand != null) {
				long nextNodeType = nextNodeToExpand.getEventType();
				AssociationRuleNode deeperAssociationRuleNode = currentRuleNode.getSubsequences().get(nextNodeType);

				if(deeperAssociationRuleNode == null) {
					deeperAssociationRuleNode = new AssociationRuleNode();
					deeperAssociationRuleNode.setEventType(nextNodeType);
					deeperAssociationRuleNode.setDepth(currentRuleNode.getDepth()+1);
					if(++numberOfRules%1000 == 0) {
						System.out.println("Have " + numberOfRules + " rules");
					}
					
					deeperAssociationRuleNode.setParentAssociationRule(currentRuleNode);
					currentRuleNode.getSubsequences().put(nextNodeType, deeperAssociationRuleNode);
					
				}
				nextNodeToExpand = depthFirstSequenceTraversal(nextNodeToExpand, deeperAssociationRuleNode, positiveExample, eventSource);
			}
		}

		// Done searching subsequences of "startingPoint".
		// Pass "the preceding event" to the callee so that it can analyze subsequences which EXCLUDE "startingPoint"
		return startingPoint.getPreviousNode();
	}

	private void validateDepthFirstSearchTraversalParameters(
			AssociationRuleNode currentRuleNode, Object eventSource) {
		if(currentRuleNode == null) {
			throw new IllegalArgumentException("currentRuleNode may not be null");
		}

		if(eventSource == null) {

		}
	}

	private AssociationRuleNode getRootAssociationRuleNode(long eventType) {
		if(associationRuleRoots.containsKey(eventType)) {
			return associationRuleRoots.get(eventType);
		}
		else {
			AssociationRuleNode associationRuleNode = new AssociationRuleNode();
			associationRuleNode.setDepth(0);
			associationRuleNode.setEventType(eventType);
			associationRuleRoots.put(eventType, associationRuleNode);
			return associationRuleNode;
		}
	}
}
