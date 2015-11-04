package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


public class POStagger {

	private static Hashtable<String, HashSet<String>> tags;
	private static POStagger instance = null;
	private static int counter;
	private static MyProcess proc1  = new MyProcess();
	
	public static POStagger getInstance() {
		if (instance == null) instance = new POStagger();
		return instance;
	}
	
	private POStagger(){
		//java -mx300m -classpath stanford-postagger.jar edu.stanford.nlp.tagger.maxent.MaxentTagger -model models/bidirectional-wsj-0-18.tagger
//		proc1.start("java","-mx300m","-classpath", 
//				"stanford-postagger.jar", 
//				"edu.stanford.nlp.tagger.maxent.MaxentTagger",
//				"-model", "models/left3words-wsj-0-18.tagger");
		proc1.start("java","-mx300m","-classpath", 
				"stanford-postagger.jar", 
				"edu.stanford.nlp.tagger.maxent.MaxentTagger",
				"-model", "models/bidirectional-wsj-0-18.tagger");
		
		counter = 0;
		tags = new Hashtable<String, HashSet<String>>();
		//noun
		HashSet<String> noun = new HashSet<String>();
		noun.add("NN");
		noun.add("NNS");
		noun.add("NNP");
		noun.add("NNPS");
		tags.put("noun", noun);
		
		//verb
		HashSet<String> verb = new HashSet<String>();
		verb.add("VB");
		verb.add("VBD");
		verb.add("VBG");
		verb.add("VBN");
		verb.add("VBP");
		verb.add("VBZ");
		tags.put("verb", verb);
		
		//adj
		HashSet<String> adj = new HashSet<String>();
		adj.add("JJ");
		adj.add("JJR");
		adj.add("JJS");
		tags.put("adj", adj);
		
		//adv
		HashSet<String> adv = new HashSet<String>();
		adv.add("RB");
		adv.add("RBR");
		adv.add("RBS");
		tags.put("adv", adv);
		
		//other 
		HashSet<String> other = new HashSet<String>();
		other.add("CC");
		other.add("CD");
		other.add("DT");
		other.add("EX");
		other.add("FW");
		other.add("IN");
		other.add("LS");
		other.add("MD");
		other.add("PDT");
		other.add("POS");
		other.add("PRP");
		other.add("PRP$");
		other.add("RP");
		other.add("SYM");
		other.add("TO");
		other.add("UH");
		other.add("WDT");
		other.add("WP");
		other.add("WP$");
		other.add("WRB");
		tags.put("other", other);
		
	}
	
	public void restart(){
		counter = 1;
		proc1.start("java","-mx300m","-classpath", 
				"stanford-postagger.jar", 
				"edu.stanford.nlp.tagger.maxent.MaxentTagger",
				"-model", "models/bidirectional-wsj-0-18.tagger");
		
	}
	
	public boolean tag(String sentence, String target_word, String target_pos, Database db){
		try{ 
			if(counter==10000){
				this.endPOStagger();
				this.restart();
			}else
				counter++;
			sentence = sentence.replaceAll("<S> ", "");
			sentence = sentence.replaceAll(" </S>", "");
			sentence = sentence.replaceAll("\\?", "");
			sentence = sentence.replaceAll("\\!", "");
			sentence = sentence.split("\t")[0];
			sentence+="\n";
			
			//System.out.println(sentence);
			proc1.write(sentence);
			String tagged_sentence = proc1.readLine();
			System.out.println(tagged_sentence);
			if(tagged_sentence.equalsIgnoreCase("program ends")){
				this.endPOStagger();
				this.restart();
				return false;
			}
			proc1.readLine();
			proc1.err.readLine();
			proc1.err.readLine();
			String[] tagged_words = tagged_sentence.split(" ");
	        for(int i = 0;i<tagged_words.length;i++){
	        	String[] word_tag = tagged_words[i].split("_");
	        	if(word_tag[0].equalsIgnoreCase(target_word)&&tags.get(target_pos).contains(word_tag[1])){
	        		//upper case and not named entity
	        		return true;
	        	}
	        }
		}catch(Exception e){
			e.printStackTrace();
			this.restart();
			return false;
		}
		
		return false;
	}
	
	public boolean tagDictStory(String sentence, String target_word, String target_pos, Database db){
		try{ 
			sentence = sentence.replaceAll("<S> ", "");
			sentence = sentence.replaceAll(" </S>", "");
			sentence = sentence.replaceAll("\\?", "");
			sentence = sentence.replaceAll("\\!", "");
			sentence = sentence.split("\t")[0];
			sentence+="\n";
//			if(target_word.equalsIgnoreCase("slender")){
//				System.out.println(sentence);
//			}
			//System.out.println(sentence);
			proc1.write(sentence);
			String tagged_sentence = proc1.readLine();
			proc1.readLine();
			proc1.err.readLine();
			proc1.err.readLine();
			String[] tagged_words = tagged_sentence.split(" ");
	        for(int i = 0;i<tagged_words.length;i++){
	        	String[] word_tag = tagged_words[i].split("_");
	        	String query = "select baseform from liuliu.word_base_pos where word = '"+word_tag[0].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
	        	ResultSet rs = db.execute(query);
	        	String base;
	        	if(rs.next()){ // we only choose the most frequently used baseform
	        		base = rs.getString("baseform");
	        	}else{
	        		base = word_tag[0].toLowerCase();
	        	}
	        	if(base.equalsIgnoreCase(target_word)&&tags.get(target_pos).contains(word_tag[1])){
	        	
	        		return true;
	        	}
	        }
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	
	public void endPOStagger(){
		proc1.destroy();
	}
	
	public static void main(String[] args) throws Exception {
		POStagger tagger =  POStagger.getInstance();
		Database db = new Database();
		//boolean result = tagger.tagDictStory("It made Bowser the Hound seem very slow , as , with his nose to the ground , he came racing after Reddy , making a tremendous noise with his great voice . ", "tremendous", "adj", db);
		for(int i = 0;i<10000;i++)
		{boolean result = tagger.tag("This is n't the greatest example sentence in the world because I 've seen better .", "tremendous", "adj", db);
		System.out.println(result);
		}
		
	}
}


