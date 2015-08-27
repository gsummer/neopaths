package org.networklibrary.neopaths;

import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

public interface NeoPaths {
	
	public Iterable<PropertyContainer> connectByShortestPath( GraphDatabaseService graph,
			Node start,
			Node end,
			String weight,
//			boolean minimum,
			boolean directed,
//			String[] types,
			String mark);
	
	public Iterable<PropertyContainer> connectByShortestPath( GraphDatabaseService graph,
			List<Node> nodes,
			String weight,
//			boolean minimum,
			boolean directed,
//			String[] types,
			String mark
			);
}
