package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

import relatedWord.RelatedWords;
import util.Database;
import util.TimeStat;
import generator.*;

public class TestGenerator {

	ArrayList<String> part_keywords; //309 target words
	ArrayList<String> all_keywords;
	
	public TestGenerator(String partfile, String allfile){
		part_keywords= new ArrayList<String>();
		all_keywords = new ArrayList<String>();
		initialPartKeys(partfile);
		initialAllKeys(allfile);
	}
	
	private void initialPartKeys(String filename){
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				part_keywords.add(line.toLowerCase());
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void initialAllKeys(String filename){
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				all_keywords.add(line.toLowerCase());
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	static public String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestGenerator tg = new TestGenerator(".//resource//journal_words.txt",".//resource//Tier2.txt");
		TimeStat ts = new TimeStat();
		ts.start();
		Database db = new Database();
		Generator eg = new Generator();

		RelatedWords rw = new RelatedWords();
		Filters rr = new Filters();
		rr.initialRemoveString(".//resource//punctuations.txt");
		rr.initialFunctionWords(".//resource//functionWords.txt", db);
		rr.initialWebWords(".//resource//webspecific.txt", db);
		rr.initialspecialPunctuations();
		rr.initialtabooWords();
		rr.initialcontrolWords();
		rr.initialrelativePronouns();
		rr.initialSpecialVerbs();
		System.out.println(ts.getElapsedTimeSeconds());
		System.out.println(System.currentTimeMillis());
//		String current_phrase = "am really anxious to get the";
//		long current_frequency = 500;
//		Hashtable<String, Long> candidates = new Hashtable<String, Long>();
//		ArrayList<String> candidatelist = new ArrayList<String>();
//		eg.add_StartEnd_fivegrams(target_word, "adj", current_phrase, current_frequency, db, rr, rw, candidates, candidatelist);
//		System.out.println(candidates.size());
//		System.out.println(candidatelist.size());
		
		
		//Step 1: generate context candidates
//		String[] words = "anxious, courage, declare, extinct, merchant, remarkable, slender, stout, suspicious, tremendous"
//			.split(", ");
//		tg.part_keywords.clear();
//		for (int i = 0; i < words.length; i++)
//			tg.part_keywords.add(words[i]);
		
		for(int i = tg.part_keywords.size()-1;i>=0;i--){
			String target_word = tg.part_keywords.get(i);
			HashSet<String> relatedwords = new HashSet<String>();
			eg.readTop100RelatedWordList(target_word, relatedwords, db);
			System.out.println(target_word);
			ts.start();
			System.out.println(getDateTime());
			//eg.getCandidates_withStartEnd(100, rw, rr, relatedwords, target_word, "noun", db,true);
			System.out.println(ts.getElapsedTimeSeconds());
			ts.start();
			eg.getCandidates_withStartEnd(100, rw, rr, relatedwords, target_word, "noun", db,false);
			System.out.println(ts.getElapsedTimeSeconds());
		}
	
		try{
		//Step 2: find good examples
			String dirname = ".//result//new_generator";
		File dir = new File(dirname);
		
		String str[] = dir.list();
 
//		for( int i = 0; i<str.length; i++){
//			ts.start();
//			String candidate_file = dirname+ "//" + str[i];
//			String[] candidates = candidate_file.split("//");
//			
//			String target_word = candidates[candidates.length-1].split("_")[0];
//			System.out.println(target_word);
//			System.out.println(candidate_file);
//			File fcandidate =new File(candidate_file );
//			if(fcandidate.isDirectory())
//				continue;
//			String cluster_file = dirname+ "//cluster//" + str[i].replaceAll(".txt", "")+"_cluster.txt";
//			rr.cluster_content_words(candidate_file,cluster_file, 3, db);
//			System.out.println(ts.getElapsedTimeSeconds());
//			ts.start();
//			String score_file = dirname+ "//score//" + str[i].replaceAll(".txt", "")+"_score.txt";
//			String example_file = dirname+ "//good//" + str[i].replaceAll(".txt", "")+"_good.txt";
//			rr.findGoodExample(db,target_word,cluster_file,score_file,example_file);
//			System.out.println(ts.getElapsedTimeSeconds());
//		}
		
//		rr.findGoodExample_constraints(db,"abandoned",".//result//new_generator//cluster//abandoned_5_2009-07-12_03-31-56_cluster.txt",".//result//new_generator//temp//score.txt",".//result//new_generator//temp//good.txt");
		
			rr.bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		ts.start();
		eg.clearArray();

	}

}
