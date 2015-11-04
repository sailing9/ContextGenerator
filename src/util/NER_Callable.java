package util;

import java.util.ArrayList;
import java.util.concurrent.*;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

/**
 * @author liuliu
 * @version 1.0
 * @since 2009/06/10 
 * implements Callable, which can return any type of value
 */
public class NER_Callable implements Callable<ArrayList<String>>{

	/**
	 * @param args
	 */
	public AbstractSequenceClassifier classifier;
	private String sentence;
	private ArrayList<String> sentences_toNer;
	private ArrayList<String> sentences_afterNer;
	 
	public NER_Callable(){
		String serializedClassifier = "classifiers/ner-eng-ie.crf-3-all2008.ser.gz";
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		sentences_toNer = new ArrayList<String>();
	}
	public NER_Callable(String sentence){
		this.sentence = sentence;
	}
	
	public void addSentence(String sentence){
		this.sentences_toNer.add(sentence);
	}
	
	public void clearSentences(){
		this.sentences_toNer.clear();
	}
	
	public void setSentences(ArrayList<String> sentences_toner){
		this.sentences_toNer = sentences_toner;
	}
	
	public ArrayList<String> getSentences(){
		return sentences_toNer;
	}
	
	
	public ArrayList<String> call(){
		return tagInBatch();
	}
	
	public ArrayList<String> tagInBatch(){
		ArrayList<String> results = new ArrayList<String>();
		for(int i = 0;i<sentences_toNer.size();i++){
			if(tag(sentences_toNer.get(i)))
				//sentences_afterNer.add(sentences_toNer.get(i));
				results.add(sentences_toNer.get(i));
		}
		sentences_toNer.clear(); //free the memory
		return results;
	}
	
	public boolean tag(String sentence) {
		// TODO Auto-generated method stub
		sentence = sentence.replaceAll("<S> ", "");
		sentence = sentence.replaceAll(" </S>", "");
		 sentence = sentence.split("\t")[0]; //[1] is the frequency of the sentence
		 String tagged_sentence = classifier.classifyToString(sentence);
		 String[] tagged_words = tagged_sentence.split(" ");
	        for(int i = 1;i<tagged_words.length;i++){
	        	String[] word_tag = tagged_words[i].split("/");
	        	if(Character.isUpperCase(word_tag[0].charAt(0))&&word_tag[1].equalsIgnoreCase("O")){
	        		//upper case and not named entity
	        		return false;
	        	}
	        }
		 return true;
	}
	
	public static void main(String[] args){
		try{
		while(true){
			NER_Callable n = new NER_Callable();
			for(int i = 0;i<1000;i++)
				n.addSentence("But there has been a tremendous blessing in my life song");
			ExecutorService exec = Executors.newCachedThreadPool();
			Future<ArrayList<String>> results = exec.submit(n);
			ArrayList<String> nerResults = results.get();
			//for(int i = 0;i<nerResults.size();i++)
				//System.out.println(nerResults.get(i));
			System.out.println(Thread.activeCount());
		}
		}catch(InterruptedException e){
			e.printStackTrace();
			return;
		}catch(ExecutionException e){
			e.printStackTrace();
			return;
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
//		System.out.println(n.tag("But there has been a tremendous blessing in my life song"));
//		System.out.println(n.tag("I Abandoned him ."));
//		System.out.println(n.tag("get high riskinternet merchant accounts account Posted"));
	}
}
