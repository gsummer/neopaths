package org.networklibrary.neopaths.costevaluators;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

public class IntegerPropertyCost implements CostEvaluator<Integer> {

	protected String propKey;
	
	public IntegerPropertyCost(String propKey){
		this.propKey = propKey;
	}
	
	@Override
	public Integer getCost(Relationship relationship, Direction direction) {
		return 1000-(Integer)relationship.getProperty(propKey);
	}

}
