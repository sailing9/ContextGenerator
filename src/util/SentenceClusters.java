package util;

import java.util.ArrayList;

public class SentenceClusters {
	ArrayList<SentenceCluster> clusters;
	int num_cluster;
	
	public SentenceClusters(){
		clusters = new ArrayList<SentenceCluster>();
		num_cluster = 0;
	}
	
	public void addCluster(SentenceCluster sc){
		clusters.add(sc);
		num_cluster++;
	}
	
	public void setCluster(SentenceCluster sc, int index){
		clusters.set(index, sc);
	}
	
	public ArrayList<SentenceCluster> getClusters(){
		return clusters;
	}
	
	public SentenceCluster getCluster(int index){
		return clusters.get(index);
	}
	
	public int getNumCluster(){
		return num_cluster;
	}
}
