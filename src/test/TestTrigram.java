package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import util.*;

/**
 * @author liuliu
 * @purpos This program is used to test whether the trigram frequencies 
 * correlates with Moddy's score better than five-gram frequencies.
 */
public class TestTrigram {

	/**
	 * @param sentence
	 * @param target_word
	 * @return
	 */
	public long getAvgTrigramFrequency(String sentence, String target_word, Database db){
		long avg = 0;
		try{
			String[] words = sentence.split("\t")[0].split(" ");
			long sumFreq = 0; //sum of frequencies of trigrams which contain the target word
			int numFreq = 0; //number of trigrams which contain the target word
			for(int i = 0;i<words.length-2;i++){
				String current_trigram = words[i]+" "+words[i+1]+" "+words[i+2];
				if(current_trigram.toLowerCase().contains(target_word.toLowerCase())){
					String query = "select frequency from liuliu."+target_word+"_3grams where trigram = '"+current_trigram+"'";
					ResultSet rs = db.execute(query);
					if(rs.next()){
						long frequency  = rs.getLong("frequency");
						sumFreq+=frequency;
						numFreq++;
						System.out.print(frequency+"\t");
					}else{
						System.out.println(current_trigram);
					}
				}
			}
			System.out.println();
			avg = sumFreq/numFreq;
		}catch(SQLException e){
			e.printStackTrace();
		}
		return avg;
	}
	
	//highest or lowest
	public long getHighestTrigramFrequency(String sentence, String target_word, Database db){
		long highestFreq = Long.MAX_VALUE; //sum of frequencies of trigrams which contain the target word
		try{
			String[] words = sentence.split("\t")[0].split(" ");
			
			for(int i = 0;i<words.length-2;i++){
				String current_trigram = words[i]+" "+words[i+1]+" "+words[i+2];
				if(current_trigram.toLowerCase().contains(target_word.toLowerCase())){
					String query = "select frequency from liuliu."+target_word+"_3grams where trigram = '"+current_trigram+"'";
					ResultSet rs = db.execute(query);
					if(rs.next()){
						long frequency  = rs.getLong("frequency");
						if(highestFreq>frequency)
							highestFreq = frequency;
					}else{
						System.out.println(current_trigram);
					}
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return highestFreq;
	}
	
	public long getHighestFivegramFrequency(String sentence, String target_word, Database db){
		long highestFreq = Long.MIN_VALUE; //sum of frequencies of trigrams which contain the target word
		try{
			String[] words = sentence.split("\t")[0].split(" ");
			
			for(int i = 0;i<words.length-4;i++){
				String current_fivegram = words[i]+" "+words[i+1]+" "+words[i+2]+" "+words[i+3]+" "+words[i+4];
				if(current_fivegram.toLowerCase().contains(target_word.toLowerCase())){
					String query = "select frequency from liuliu."+target_word+"_5grams where five_gram = '"+current_fivegram+"'";
					ResultSet rs = db.execute(query);
					if(rs.next()){
						long frequency  = rs.getLong("frequency");
						System.out.print(frequency+"\t");
						if(highestFreq<frequency)
							highestFreq = frequency;
					}else{
						System.out.println(current_fivegram);
					}
				}
			}
			System.out.println();
		}catch(SQLException e){
			e.printStackTrace();
		}
		return highestFreq;
	}
	
	public void testTrigram(String filename){
		Database db = new Database();
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while((line=br.readLine())!=null){
				line = line.replaceAll("'", "\\\\'");
				String target_word = line.split("\t")[0];
				String sentence = line.split("\t")[1];
				//long avg = getAvgTrigramFrequency(sentence, target_word, db);
				long highest = getHighestFivegramFrequency(sentence, target_word, db);
				//System.out.println(highest);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestTrigram tt = new TestTrigram();
		tt.testTrigram(".//input//dictionary.txt");
		//tt.testTrigram(".//input//");
	}

}
