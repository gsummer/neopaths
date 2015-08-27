package org.networklibrary.neopaths.fast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo4j.graphalgo.impl.util.DoubleAdder;
import org.neo4j.graphalgo.impl.util.DoubleComparator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.server.plugins.Source;
import org.neo4j.tooling.GlobalGraphOperations;
import org.networklibrary.neopaths.NeoPaths;
import org.networklibrary.neopaths.costevaluators.StringCombinedScore;

public class NeoPathFast implements NeoPaths {

	protected List<PropertyContainer> connectDijkstra( @Source GraphDatabaseService graph,
			Node start,
			Node end,
			String weight,
//			boolean minimum,
			boolean directed,
			RelationshipType[] types,
			String mark){

		List<PropertyContainer> result = new ArrayList<PropertyContainer>();

		Direction direction = null;
		if(directed){
			direction = Direction.OUTGOING;
		} else {
			direction = Direction.BOTH;
		}

		SingleSourceShortestPath<Double> sssPath = new SingleSourceShortestPathDijkstra<Double>(0.0, null, new StringCombinedScore() ,new DoubleAdder(), new DoubleComparator(), direction, types);

		try(Transaction tx = graph.beginTx()){
			sssPath.setStartNode(start);
			start.setProperty(mark, true);
			
			List<PropertyContainer> path = sssPath.getPath(end);

			if(path != null && !path.isEmpty()){

				if(mark != null && !mark.isEmpty()){
					for(PropertyContainer pc : path){
						pc.setProperty(mark, true);
					}
				}
				result.addAll(path);
			}
			tx.success();
		}

		return result;
	}

	@Override
	public Iterable<PropertyContainer> connectByShortestPath(GraphDatabaseService graph, 
			Node start, 
			Node end, 
			String weight,
//			boolean minimum, 
			boolean directed, 
//			String[] types, 
			String mark) {

		RelationshipType[] relTypes = null;
		try (Transaction tx = graph.beginTx()){
			relTypes = Iterables.toArray(RelationshipType.class,GlobalGraphOperations.at(graph).getAllRelationshipTypes());
			tx.success();
		}

		return connectDijkstra(graph, start, end, weight, /*minimum,*/ directed, relTypes, mark);
	}


	public Iterable<PropertyContainer> connectByShortestPath(GraphDatabaseService graph, 
			List<Node> nodes,
			String weight,
//			boolean minimum, 
			boolean directed, 
//			String[] types, 
			String mark) {

		Set<PropertyContainer> result = new HashSet<PropertyContainer>();

		RelationshipType[] relTypes = null;
		try (Transaction tx = graph.beginTx()){
			relTypes = Iterables.toArray(RelationshipType.class,GlobalGraphOperations.at(graph).getAllRelationshipTypes());
			tx.success();
		}

		
		Set<Distance> distances = new HashSet<Distance>();
		
		if(directed){
			for(Node start : nodes){
				for(Node end : nodes){
//					result.addAll(connectDijkstra(graph, start, end, weight, minimum, directed, relTypes, mark));
					distances.add(new Distance(start,end));
				}
			}
		} else {
			for(int i = 0; i < nodes.size(); ++i){
				for(int j = i+1; j < nodes.size(); ++j){
//					result.addAll(connectDijkstra(graph, nodes.get(i), nodes.get(j), weight, minimum, directed, relTypes, mark));
					distances.add(new Distance(nodes.get(i),nodes.get(j)));
				}	
			}
		}
		
		System.out.println("num distances: " + distances.size());
//		int i = 0;
		while(!distances.isEmpty()){
			Distance d = distances.iterator().next();
			List<PropertyContainer> path = connectDijkstra(graph, d.getStart(), d.getEnd(), weight, /*minimum,*/ directed, relTypes, mark);
			
			if(path.isEmpty()){
				distances.remove(d);
			}
			for(PropertyContainer pc : path){
				if(pc instanceof Relationship){
					continue;
				}
				
				Node n = (Node)pc;
				
				if(nodes.contains(n)){
					distances.remove(new Distance(n,d.getEnd()));
				}
			}
			
//			System.out.println("path length: " + path.size());
//			System.out.println("num distances change: " + (begin - end));
//			System.out.println("distance calc time: " + (dend - dstart));
//			System.out.println("distance filter time: " + (send - sstart));
//			System.out.println("distances left: "+ distances.size());
			
			result.addAll(path);
//			++i;
		}
//		System.out.println("num loops: " + i);

		return result;
	}


}
