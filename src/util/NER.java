package util;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.StringUtils;

/**
 * @author liuliu
 * @version 1.1
 * @since 2009/06/14
 * with thread
 *
 */
public class NER extends Thread{

	/**
	 * @param args
	 */
	public AbstractSequenceClassifier classifier;
	public static NER instance = null;
	private String sentence;
	private ArrayList<String> sentences_toNer;
	private ArrayList<String> sentences_afterNer;
	 
	public NER(){
		String serializedClassifier = "classifiers/ner-eng-ie.crf-3-all2008.ser.gz";
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		sentences_toNer = new ArrayList<String>();
		sentences_afterNer = new ArrayList<String>();
	}
	public NER(String sentence){
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
	
	public static NER getInstance(){
		if (instance == null) instance = new NER();
		return instance;
	}
	
	public void run(){
		String serializedClassifier = "classifiers/ner-eng-ie.crf-3-all2008.ser.gz";
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		tagInBatch();
	}
	
	public void tagInBatch(){
		for(int i = 0;i<sentences_toNer.size();i++){
			if(tag(sentences_toNer.get(i)))
				sentences_afterNer.add(sentences_toNer.get(i));
		}
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
		NER n = new NER("I Abandoned him .");
		n.start();
		NER n1 = new NER("I Abandoned him .");
		while(n.isAlive()){
			//System.out.println("wait");
		}
		System.gc();
		System.runFinalization(); 
		System.out.println(Thread.currentThread().getName());

		n1.start();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
//		System.out.println(n.tag("But there has been a tremendous blessing in my life song"));
//		System.out.println(n.tag("I Abandoned him ."));
//		System.out.println(n.tag("get high riskinternet merchant accounts account Posted"));
	}
}
