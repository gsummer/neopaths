package org.networklibrary.neopaths;

import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

public interface NeoPathPlugin {

	public Iterable<Path> connectByShortestPath( GraphDatabaseService graph,
			List<Node> nodes,
			String weight,
			boolean minimum,
			boolean directed,
			String[] types,
			String mark
			);
	
	public Iterable<Path> connectByShortestPath( GraphDatabaseService graph,
			Node start,
			Node end,
			String weight,
			boolean minimum,
			boolean directed,
			String[] types,
			String mark
	);
	
}
