package org.networklibrary.neopaths.utils;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

public class PathToString {
	private List<PropertyContainer> path;
	
	public PathToString(List<PropertyContainer> path){
		this.path = path;
	}
	
	@Override
	public String toString(){
		StringBuilder strbuilder = new StringBuilder();
		for(PropertyContainer pc : path){
			if(pc instanceof Node){
				Node n = (Node)pc;
				strbuilder.append("(" + n.getProperty("name")+")");
			}
			if(pc instanceof Relationship){
				Relationship r = (Relationship)pc;
				strbuilder.append("--" + r.getProperty("combined_score") + "--");
			}
		}
		return strbuilder.toString();
	}
}

