package org.networklibrary.neopaths.costevaluators;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

public class StepScore implements CostEvaluator<Integer> {

	@Override
	public Integer getCost(Relationship relationship, Direction direction) {
		return new Integer(1);
	}

}
