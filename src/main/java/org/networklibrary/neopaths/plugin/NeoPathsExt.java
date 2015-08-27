package org.networklibrary.neopaths.plugin;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo4j.graphalgo.impl.util.DoubleAdder;
import org.neo4j.graphalgo.impl.util.DoubleComparator;
import org.neo4j.graphalgo.impl.util.IntegerAdder;
import org.neo4j.graphalgo.impl.util.IntegerComparator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Name;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.tooling.GlobalGraphOperations;
import org.networklibrary.neopaths.NeoPathPlugin;
import org.networklibrary.neopaths.costevaluators.StringCombinedScore;
import org.networklibrary.neopaths.utils.PathToString;


public class NeoPathsExt extends ServerPlugin implements NeoPathPlugin {
	protected List<Path> connectDijkstra( @Source GraphDatabaseService graph,
			Node start,
			Node end,
			String weight,
			boolean minimum,
			boolean directed,
			RelationshipType[] types,
			String mark){

		List<Path> result = new ArrayList<Path>();
		
		Direction direction = null;
		if(directed){
			direction = Direction.OUTGOING;
		} else {
			direction = Direction.BOTH;
		}
		
		PathExpanderBuilder builder = PathExpanderBuilder.empty();
		for(RelationshipType t : types){
			builder.add(t,direction);
		}
		
			
		PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(PathExpanders.allTypesAndDirections(), new StringCombinedScore());
		
		try(Transaction tx = graph.beginTx()){
			
			Path p = finder.findSinglePath(start, end);
			
			if(mark != null && !mark.isEmpty()){
				for(PropertyContainer pc : p){
					pc.setProperty(mark, true);
				}
			}
			result.add(p);
			tx.success();
		}
		
		

		return result;
	}
	
	@Name( "connectTwoNodes" )
	@Description( "connects two nodes via the shortest path" )
	@PluginTarget( GraphDatabaseService.class )
	public Iterable<Path> connectByShortestPath( @Source GraphDatabaseService graph,
			@Description( "start node" )
			@Parameter( name = "start", optional = false ) Node start,
			@Description( "end node" )
			@Parameter( name = "end", optional = false ) Node end,
			@Description( "name of property used as a weight" )
			@Parameter( name = "weight", optional = true ) String weight,
			@Description( "minimize the cost of the path" )
			@Parameter( name = "minimum", optional = false ) boolean minimum,
			@Description( "follow edge directions?" )
			@Parameter( name = "directed", optional = false ) boolean directed,
			@Description( "relationship types to follow if none provided defaults to all" )
			@Parameter( name = "types", optional = true ) String[] types,
			@Description( "name of the mark property set on the elements of the path from start to end" )
			@Parameter( name = "start", optional = true ) String mark)
	{
		RelationshipType[] relTypes = null;
		try (Transaction tx = graph.beginTx()){
			relTypes = Iterables.toArray(RelationshipType.class,GlobalGraphOperations.at(graph).getAllRelationshipTypes());
			tx.success();
		}
		
		return connectDijkstra(graph, start, end, weight, minimum, directed, relTypes, mark);
	}

	
//	@Name( "connectSetOfNodes" )
//	@Description( "connects a set of nodes via the shortest path" )
//	@PluginTarget( GraphDatabaseService.class )
	public Iterable<Path> connectByShortestPath(GraphDatabaseService graph, 
			List<Node> nodes,
			String weight,
			boolean minimum, 
			boolean directed, 
			String[] types, 
			String mark) {
		
		List<Path> result = new ArrayList<Path>();
		
		RelationshipType[] relTypes = null;
		try (Transaction tx = graph.beginTx()){
			relTypes = Iterables.toArray(RelationshipType.class,GlobalGraphOperations.at(graph).getAllRelationshipTypes());
			tx.success();
		}
		
		if(directed){
			for(Node start : nodes){
				for(Node end : nodes){
					result.addAll(connectDijkstra(graph, start, end, weight, minimum, directed, relTypes, mark));
				}
			}
		} else {
			for(int i = 0; i < nodes.size(); ++i){
				for(int j = i+1; j < nodes.size(); ++j){
					result.addAll(connectDijkstra(graph, nodes.get(i), nodes.get(j), weight, minimum, directed, relTypes, mark));
				}	
			}
		}
		
		return result;
	}

}
