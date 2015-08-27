package org.networklibrary.neopaths.costevaluators;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

public class StringCombinedScore implements CostEvaluator<Double> {

	@Override
	public Double getCost(Relationship relationship, Direction direction) {
//		if(relationship.hasProperty("combined_score")){
			
			return 1000.0-((Integer)relationship.getProperty("combined_score")).doubleValue();
//		}else{
//			return Double.MAX_VALUE;
//		}
	}

}
