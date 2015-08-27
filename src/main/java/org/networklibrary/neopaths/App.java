package org.networklibrary.neopaths;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.networklibrary.neopaths.fast.NeoPathFast;

/**
 * Hello world!
 *
 */
public class App 
{
	/*
	 * args[0] = db dir (graph.db)
	 * args[1] = id list file
	 * args[2] = name of property for the visited mark
	 * args[3] = name of the property used as a weight
	 * args[4] = boolean for directed or not
	 * 
	 */
	public static void main( String[] args ) throws IOException
	{
		String db = args[0];
		db = "graph.db";
		String idFile = args[1];
		String mark = args[2];
		String weight = args[3];
		boolean directed = Boolean.parseBoolean(args[4]);

		List<String> ids = readFromFile(new File(idFile));

		System.out.println("db: " + db);

		GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(db).newGraphDatabase();

		Set<Node> nodes = new HashSet<Node>();
		
		try (Transaction tx = g.beginTx()){
	
			for(String id : ids){
				Node n = g.findNode(DynamicLabel.label("protein_coding"), "ensdarg", id);
				if(n == null){
					System.out.println("WARNING: " + id + " not found");
				} else {
					if(n.getDegree() > 0){
						nodes.add(n);
					}
				}
			}
			
			for(Node n : nodes){
				n.setProperty(mark + "_initial", true);
			}
		
			tx.success();
		}
		

		System.out.println("num ids: " + ids.size());
		
		NeoPaths pathExt = new NeoPathFast();

		long start = System.currentTimeMillis();
		Iterable<PropertyContainer> res = pathExt.connectByShortestPath(g, new ArrayList<Node>(nodes), weight, directed, mark);
		long end = System.currentTimeMillis();

		System.out.println("graph algo: " + (end - start));
		try(Transaction tx = g.beginTx()){
			for(PropertyContainer pc : res){
				
			}
			tx.success();
		}
		// */

	}
	
	static public List<String> readFromFile(File f) throws IOException {
		List<String> ids = null;

		BufferedReader r = new BufferedReader(new FileReader(f));
		ids = new ArrayList<String>();

		while(r.ready()){
			String line = r.readLine();
			ids.add(line.trim());
		}
		r.close();

		return ids;
	}
}
