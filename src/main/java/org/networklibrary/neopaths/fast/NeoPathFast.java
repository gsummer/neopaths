package org.networklibrary.neopaths.fast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo4j.graphalgo.impl.util.DoubleAdder;
import org.neo4j.graphalgo.impl.util.DoubleComparator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.tooling.GlobalGraphOperations;
import org.networklibrary.neopaths.NeoPaths;
import org.networklibrary.neopaths.costevaluators.StringCombinedScore;

public class NeoPathFast implements NeoPaths {

	@Override
	public Iterable<PropertyContainer> connectByShortestPath(GraphDatabaseService graph, 
			Node start, 
			Node end, 
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

		try {

			List<Node> targets = new ArrayList<Node>();
			targets.add(end);
			DijkstraConnector connector = new DijkstraConnector(graph, start, targets, weight, directed, relTypes);

			result.addAll(connector.call());

			try(Transaction tx = graph.beginTx()){
				for(PropertyContainer pc : result){
					pc.setProperty(mark, true);
				}
				tx.success();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}



		return result;
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

		int threadCount = Math.min(16, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		ExecutorService execService = Executors.newFixedThreadPool(threadCount);

		List<Future<List<PropertyContainer>>> results = new ArrayList<Future<List<PropertyContainer>>>();

		if(directed){
			for(Node start : nodes){
				try (Transaction tx = graph.beginTx()){
					DijkstraConnector connector = new DijkstraConnector(graph, start, nodes, weight, directed, relTypes);
					results.add(execService.submit(connector));
				}
			}
		} else {
			for(int i = 0; i < nodes.size(); ++i){
				for(int j = i+1; j < nodes.size(); ++j){
					DijkstraConnector connector = new DijkstraConnector(graph, nodes.get(i), nodes.subList(j, nodes.size()-1), weight, directed, relTypes);
					results.add(execService.submit(connector));
				}
			}
		}

		try(Transaction tx = graph.beginTx()){

			for(Future<List<PropertyContainer>> f : results){
				result.addAll(f.get());
			}

			for(PropertyContainer pc : result){
				pc.setProperty(mark, true);
			}
			tx.success();
		} catch (InterruptedException e) {
			System.out.println("Problem with retrieving the futures");
			e.printStackTrace();
			return null;
		} catch (ExecutionException e) {
			System.out.println("Problem with retrieving the futures");
			e.printStackTrace();
			return null;
		}

		try {
			execService.shutdown();
			execService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println("stopping the execService failed");
			e.printStackTrace();
			return null;
		}

		return result;
	}

	protected class DijkstraConnector implements Callable<List<PropertyContainer>>{

		protected GraphDatabaseService graph;
		protected Node start;
		protected List<Node> targets;
		protected String weight;
		protected boolean directed;
		protected RelationshipType[] relTypes;

		public DijkstraConnector(GraphDatabaseService graph, Node start, Node target, String weight, boolean directed, RelationshipType[] relTypes){
			this.graph = graph;
			this.start = start;
			this.targets = new ArrayList<Node>();
			targets.add(target);

			this.weight = weight;
			this.directed = directed;
			this.relTypes = relTypes;
		}

		public DijkstraConnector(GraphDatabaseService graph, Node start, List<Node> targets, String weight, boolean directed, RelationshipType[] relTypes){
			this.graph = graph;
			this.start = start;
			this.targets = targets;

			this.weight = weight;
			this.directed = directed;
			this.relTypes = relTypes;
		}

		@Override
		public List<PropertyContainer> call() throws Exception {
			List<PropertyContainer> res = new ArrayList<PropertyContainer>();
			try(Transaction tx = graph.beginTx()){
				res.addAll(connectToAllDijkstra(graph, start, targets, weight, directed, relTypes));
				tx.success();
			}
			return res;

		}

		protected List<PropertyContainer> connectToAllDijkstra(GraphDatabaseService graph,
				Node start, 
				List<Node> targets,
				String weight, 
				boolean directed, 
				RelationshipType[] types
				){

			List<PropertyContainer> result = new ArrayList<PropertyContainer>();
			if(start.getDegree() == 0){
				return result;
			}

			Direction direction = null;
			if(directed){
				direction = Direction.OUTGOING;
			} else {
				direction = Direction.BOTH;
			}

			SingleSourceShortestPath<Double> sssPath = new SingleSourceShortestPathDijkstra<Double>(0.0, null, new StringCombinedScore() ,new DoubleAdder(), new DoubleComparator(), direction, types);

			sssPath.setStartNode(start);


			for(Node end : targets){
				if(end != start && end.getDegree() > 0){
					result.addAll(sssPath.getPath(end));
				}
			}

			return result;
		}

	}
}
