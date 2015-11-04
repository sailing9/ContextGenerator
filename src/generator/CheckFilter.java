package generator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import relatedWord.RelatedWords;
import util.Database;

public class CheckFilter {

	public static Filters filter;
	public RelatedWords rw;
	public Database db;
	public Hashtable<String, String> word_pos;
	public CheckFilter(){
		db = new Database();
		filter = new Filters();
		rw = new RelatedWords();
		word_pos = new Hashtable<String, String>();
		filter.initialRemoveString("D:\\LiuLiu\\Listen\\Research Resource\\Punctuations\\punctuations.txt");
		filter.initialFunctionWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\functionWords.txt", db);
		filter.initialWebWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\webSpecific\\webspecific.txt", db);
		filter.initialspecialPunctuations();
		filter.initialtabooWords();
		filter.initialrelativePronouns();
		filter.initialSpecialVerbs();
		initializePOS();
	}
	
	
	public void initializePOS(){
		try{
			BufferedReader br = new BufferedReader(new FileReader(".//resource//word_pos.txt"));
			String line = "";
			while((line=br.readLine())!=null){
				String word = line.split("\t")[0];
				String pos = line.split("\t")[1];
				word_pos.put(word, pos);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void checkWithGoodExample(String sentence){
		String target_word = sentence.split("\t")[0];
		sentence = sentence.split("\t")[1];
		String[] words = sentence.split(" ");
		String target_pos = word_pos.get(target_word);
		
		System.out.print(sentence+"\t");
		if(!filter.checkTaboo(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkPunctuations(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkWebspecific(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkPronouns(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkCapitalize(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkWordLevel(target_word, words, rw, db)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkStartEnd(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
			
		if(!filter.checkSpecialVerbs(words)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkContentWords(words, db)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkGrammar(sentence)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkPOSDictStory(sentence, target_word, target_pos, db)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		
		if(!filter.checkNER(sentence)){
			System.out.print("0\t");
		}else{
			System.out.print("1\t");
		}
		System.out.println();
	}
	
	public void checkWithGoodExamples(String filename){
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while((line=br.readLine())!=null){
				checkWithGoodExample(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void checkRelatedWordFilters(String filename){
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while((line=br.readLine())!=null){
				checkRelatedWordFilter(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @param sentence
	 * Check the filter: The start five-gram has to contain at least one related word
	 * Here, the start five-gram is the five-gram containing the target word. Any one is OK. 
	 * As long as there is a five-gram containing a related word, we say the sentence pass the filter
	 */
	public void checkRelatedWordFilter(String sentence){
		//extract all the five-grams containing the target word
		String target_word = sentence.split("\t")[0];
		sentence = sentence.split("\t")[1];
		String[] words = sentence.split(" ");
		HashSet<String> relatedWords = new HashSet<String>();
		Filters.readTop100RelatedWordList(target_word, relatedWords, db);
		try{
		//get the index of the target word
		int index = -1;
		 for(int i = 0;i<words.length;i++){
	        	String query = "select baseform from liuliu.word_base_pos where word = '"+words[i].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
	        	ResultSet rs = db.execute(query);
	        	String base;
	        	if(rs.next()){ // we only choose the most frequently used baseform
	        		base = rs.getString("baseform");
	        	}else{
	        		base = words[i].toLowerCase();
	        	}
	        	if(base.equalsIgnoreCase(target_word)){
	        		index = i;
	        		break;
	        	}
		 }
		 
		 //get all the five-grams
		 int start = (index-4>0)?(index-4):0;
		 int end = (words.length-5>index)?index+4:(words.length-1);
		 String[] base = new String[end-start+1];
		 int num_relatedwords = 0;
		 for(int i = start;i<=end;i++){
		//	 System.out.print(words[i]+"\t");
			 base[i-start] = rw.getMostFrequentBase(words[i], db) == null ? words[i].toLowerCase()
							: rw.getMostFrequentBase(words[i], db);
			if (relatedWords.contains(base[i-start])) {
				System.out.print(words[i]+"\t");
				num_relatedwords++;
			}
		}
				if (num_relatedwords == 0)
					System.out.println(0); 
				else
					System.out.println(1);
								
				 
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CheckFilter cf = new CheckFilter();
		cf.checkWithGoodExamples(".//checkFilter//story.txt");
		//cf.checkRelatedWordFilters(".//checkFilter//story.txt");
	}

}
