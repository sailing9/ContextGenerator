package generator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.sql.*;

import util.NER_Callable;
import util.RandomNumber;
import util.SentenceCluster;
import util.SentenceClusters;
import util.SortHashtableByValue;
import util.TimeStat;
import util.Database;
import util.NER;
import util.POStagger;
import util.GrammarChecker;

import relatedWord.*;


/**
 * @author liuliu
 * @Purpose: This class is used to refine the generated sentences examples according to the criteria written in the document 
 * "D:\LiuLiu\Listen\Research Progress\2009Spring\Week1(1.12-1.18)"
 * 
 */

/* @since 2009/06/16: disable the original NER filter. Add one NER filter in batch mode
 * */

/*
 * Difficulties in this process
 * 1. words and baseforms
 * common words [there, is, good, chance] is the same as [there, was, good, chances]
 * two ways to solve the problem (1) use the table "word_base_pos"
 * 								 (2) use the stemming technology
 * question: which is better under this condition
 * Solution: Now I feel "word_base_pos" is better. If we stem the word, "is" and "was" are different words but actually they have the same base form.
 * 
 * 
 * 2. <S>, </S> and punctuations which should not be considered in the common words sets
 * methods: get a punctuation list
 * 
 * 3. The common words should not contain functions words like "is, the, a". There is no 
 * semantically similarity between sentences which only overlap in these words. So we should add
 * these functions words in to the removeString list. 
 * Method: get a functions words list
 * Top 100 words in Google unigram data
 *
 * */



public class Filters {
	
	ArrayList<SentenceCluster> scs ;
	HashSet<String> removeString;
	HashSet<String> functionWords;
	HashSet<String> webWords;
	HashSet<String>	tabooWords;
	HashSet<String> specialPunctuations;
	HashSet<String> relativePronouns;
	HashSet<String> specialVerbs;
	HashSet<String> controlWords;
	Random rn;
	POStagger pos;
	NER ner;
	GrammarChecker grammar;
	public BufferedWriter bw;
	public Filters(){
		scs = new ArrayList<SentenceCluster>();
		removeString = new HashSet<String>();
		functionWords = new HashSet<String>();
		webWords = new HashSet<String>();
		specialPunctuations = new HashSet<String>();
		tabooWords = new HashSet<String>();
		relativePronouns = new HashSet<String>();
		specialVerbs = new HashSet<String>();
		controlWords = new HashSet<String>();
		rn = new Random();
		pos = POStagger.getInstance();
		//ner = NER.getInstance();
		grammar = GrammarChecker.getInstance();
		try{
			bw = new BufferedWriter(new FileWriter(".\\result\\temp",true));
		}catch(IOException e){
			
		}
	}
	
	public void initialRemoveString(String filename){
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				//keywords.add(line.toLowerCase());
				removeString.add(line.toLowerCase());
			}
			removeString.add("<s>");
			removeString.add("</s>");

		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void initialrelativePronouns(){
		
		String relatives = "who whom whose which whomever whatever";
		String[] words = relatives.split(" ");
		for(int i = 0;i<words.length;i++)
			relativePronouns.add(words[i]);
	}
	
	
	public void initialtabooWords(){
		String taboo = "roll in the hay;hand job;blow job;french kiss;French kiss;soul kiss;deep kiss;jacking off;jerking off;finish off;get off;piss off;bugger off;asshole;assholes;bitch;bitches;cock;cocks;cocksucker;cocksuckers;cunt;cunts;fag;fags;faggot;faggots;fuck;fucker;fucking;fucks;injun;injuns;kike;kikes;motherfucker;motherfucking;motherfuckers;nigger;niggers;piss;pisses;pissing;pussy;pussies;shit;shitting;shits;slut;sluts;tit;tits;whore;whores";
		String[] words = taboo.split(";");
		for(int i = 0;i<words.length;i++)
			tabooWords.add(words[i]);
	}
	
	public void initialcontrolWords(){
		//String controlWords_target ="boast	coarse	combine	commit	compete	consider	creature	delight	discovery	doleful	fond	fortune	gape	grumble	haughty	imagine	instinct	loom	occupy	pace	pout	prosper	prudent	quarrel	realize	reduce	scent	spectator	suppose	vex";
		String controlWords_control = "blend	crude	attitude	ignore	postpone	justify	impulse	contrast	distinction	tranquil	bluff	department	sulk	profile	swarthy	elevate	panic	tweed	indicate	boss	steak	broaden	profuse	budget	stimulate	tighten	quart	contractor	garnish	wheeze";
		String controlWordString = controlWords_control;
		String[] words = controlWordString.split("\t");
		for(int i = 0;i<words.length;i++)
			controlWords.add(words[i]);
	}
	
	public void initialspecialPunctuations(){
		String punctuations = "<UNK> \' ( ) [ ] { } `` '' < > : , - _ ... \" ; / ` ~ @ # $ % ^ & * + | \\ = -- » © £ .. ® ° .... ---";
		String[] words = punctuations.split(" ");
		for(int i = 0;i<words.length;i++)
			specialPunctuations.add(words[i]);
	}
	
	public void initialFunctionWords(String filename, Database db){
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				//keywords.add(line.toLowerCase());
				String query = "select baseform from liuliu.word_base_pos where word = '"+line.replaceAll("'", "\\\\'")+"'";
				ResultSet rs = db.execute(query);
				while(rs.next()){
					String word = rs.getString("baseform");
					functionWords.add(word.toLowerCase());
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void initialWebWords(String filename, Database db){
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				//keywords.add(line.toLowerCase());
				String query = "select baseform from liuliu.word_base_pos where word = '"+line.replaceAll("'", "\\\\'")+"'";
				ResultSet rs = db.execute(query);
				while(rs.next()){
					String word = rs.getString("baseform");
					webWords.add(word.toLowerCase());
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void initialSpecialVerbs(){
		String[] verbs = "do,does,did,be,am,is,are,was,were,been,'s,'m,have,had,has,can,could,will,shall,must,may".split(",");
		for(int i = 0;i<verbs.length;i++){
			specialVerbs.add(verbs[i]);
		}
	}
	

	/**
	 * @param sentence
	 * @return
	 */
	public int calSentenceLength(String sentence){
		int num = 0;
		String[] words = sentence.split("\t")[0].split(" ");
		//String[] words = sentence.split(" ");
		num = words.length;
		if(sentence.contains("<S>")){
			num--;
		}
		if(sentence.contains("</S>")){
			num--;
		}
		return num;
	}
	
	public int calNumContentWord(String sentence){
		int num = 0;
		String[] words = sentence.split("\t")[0].split(" ");
		//String[] words = sentence.toLowerCase().split(" ");
		for(int i = 0;i<words.length;i++){
			if(!(removeString.contains(words[i])||functionWords.contains(words[i]))){
				num++;
			}
		}
		return num;
	}
	
	public int calNumWebWord(String[] words){
		int num = 0;
		//String[] words = sentence.split("\t")[0].split(" ");
		//String[] words = sentence.toLowerCase().split(" ");
		for(int i = 0;i<words.length;i++){
			if(webWords.contains(words[i])){
				num++;
			}
		}
		return num;
	}
	
	public int calNumRelatedWord(String sentence, String target_word, HashSet<String> list){
		int num = 0;
		String[] words = sentence.split("\t")[0].split(" ");
		//String[] words = sentence.toLowerCase().split(" ");
		for(int i = 0;i<words.length;i++){
			if(list.contains(words[i])){
				num++;
			}
		}
		return num;
	}
	
	
	public static void readTop100RelatedWordList_MI(String target_word,HashSet<String> relatedwords, Database db){
		try{
			String query = "select related_word_base from liuliu."+target_word
			+"_relatedwords where webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0 order by mutual_information desc limit 500";
			//previously, it's 20, which is not correct.
			ResultSet rs = db.execute(query);
			while(rs.next()){
				String related_word= rs.getString("related_word_base");
				relatedwords.add(related_word);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void readTop100RelatedWordList(String target_word,HashSet<String> relatedwords, Database db){
		try{
			String query = "select related_word_base from liuliu."+target_word
			+"_relatedwords where webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0 order by new_measure desc limit 100";
			//previously, it's 20, which is not correct.
			ResultSet rs = db.execute(query);
			while(rs.next()){
				String related_word= rs.getString("related_word_base");
				relatedwords.add(related_word);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	

	public boolean checkContentWords(String[] words, Database db){
		int num=0;
		try{
			for(int i = 0;i<words.length;i++){
				String query = "select baseform from liuliu.word_base_pos where word = '"+words[i].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
				ResultSet rs = db.execute(query);
				String base;
				if(rs.next()){ // we only choose the most frequently used baseform
					base = rs.getString("baseform");
				}else{
					base = words[i].toLowerCase();
				}
				if(base.matches("[a-z]+")&&(!functionWords.contains(base))){
					num++;
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		
		if(num>=3)
			return true;
		else
			return false;
	}	
	
	
	public boolean checkWordLevel(String target_word, String[] words, RelatedWords rw , Database db){
		int num = 0;
		int num_above2 = 0;
		for(int i = 0;i<words.length;i++){
			if((!words[i].equalsIgnoreCase(target_word))&&words[i].toLowerCase().matches("[a-zA-Z]+")){
				String base = rw.getMostFrequentBase(words[i], db);
				if(base==null)
					base = words[i].toLowerCase();	
				if(functionWords.contains(base)){
					continue;
				}
				int level = rw.judgeWordLevel(base, db);
				if(level>23 || (level > 2 && level < 20)){
					num_above2++;
				}
			}
		}
		if(num_above2<2)
			return true;
		else
			return false;
	}
	
	public boolean checkWebspecific(String[] words){
		int number = calNumWebWord(words);
		if(number>=1){
			return false;
		}
		return true;
	}
	
/*	public boolean checkCapitalize(String[] words){
		int number = 0;
		for(int i = 0;i<words.length;i++){
			if(words[i].length()<1){
				number++;
				continue;
			}
			//48-57: number 0-9
			//65-90: capitaized letters: A-Z
			if((words[i].charAt(0)>47 &&words[i].charAt(0)<58) || (words[i].charAt(0)>64 &&words[i].charAt(0)<91 ) ){
				number++;
			}
			if(!words[i].matches("[a-zA-Z0-9]+"))
				number++;
		}
		if(number>=4)
			return false;
		return true;
	}
*/	
	public boolean checkCapitalize(String[] words){
		
		for(int i = 0;i<=words.length-4;i++){
			int number = 0;
			for(int j = i;j<i+4;j++){	
				if(words[j].length()<1){
						number++;
						continue;
					}
				//48-57: number 0-9
				//65-90: capitalized letters: A-Z
				if((words[j].charAt(0)>47 &&words[j].charAt(0)<58) || (words[j].charAt(0)>64 &&words[j].charAt(0)<91 ) ){
					number++;
					continue;
				}
				if(!words[j].matches("[a-zA-Z0-9]+"))
					number++;
			}
			if(number>=4)
				return false;
		}
		return true;
	}
	public boolean checkTaboo(String[] words){
		for(int i = 0;i<words.length;i++){
			if(controlWords.contains(words[i])) //set for control words
				return false;
			if(tabooWords.contains(words[i]))
				return false;
			if(i<words.length-3){
				if(tabooWords.contains(words[i]+" "+words[i+1]+" "+words[i+2]+" "+words[i+3]))
					return false;
			}
			if(i<words.length-1){
				if(tabooWords.contains(words[i]+" "+words[i+1])){
					return false;
				}
			}
			
		}
		return true;
	}
	
	public boolean checkPunctuations(String[] words){
		for(int i = 0;i<words.length;i++){
			if(specialPunctuations.contains(words[i]))
				return false;
			if(words[i].length()>1&&words[i].contains("."))
				return false;
		}
		return true;
	}
	
	public boolean checkPronouns(String[] words){
		for(int i = 0;i<words.length;i++){
			if(relativePronouns.contains(words[i]))
				return false;
		}
		return true;
	}
	
	public boolean checkSpecialVerbs(String[] words){
		
			if(specialVerbs.contains(words[words.length-1]))
				return false;
		
		return true;
	}
	
	public boolean checkStartEnd(String[] words){
		if((words[0].equalsIgnoreCase("</S>")||(words[0].charAt(0)>64 &&words[0].charAt(0)<91))||(words[words.length-1].equalsIgnoreCase("</S>")||words[words.length-1].equals(".")||words[words.length-1].equals("!")||words[words.length-1].equals("?")))
			return true;
		return false;
	}

	/**
	 * @param words: context candidates
	 * @return true if the candidate starts from a start five-gram in Google corpus 
	 * 		   and ends with an end five-gram in Google Corpus
	 * @author liuliu
	 * @version 1.0
	 * @since 2009/06/03
	 */
	public int check_StartEnd5gram(String[] words, Database db){
			boolean head = false;
			boolean tail = false;
		try{
			//check start five-grams
			String current_start = "";
			if(words[0].equalsIgnoreCase("<S>")){//It starts from a right start five-gram 
				head = true;
			}else{
				current_start = "<S>";
				for(int i = 0;i<4;i++){
					current_start+=" "+words[i];
				}
				String query = "select five_gram from liuliu.startsymbol_all_5grams"
					+" where five_gram = '"+current_start+"' limit 1";
				ResultSet rs = db.execute(query);
				if(rs.next()){
					head = true;
				}else{
					head = false;
				}
			}
			
			//check end five-grams
			String current_end = "";
			int length = words.length;
			if(words[length-1].equalsIgnoreCase("</S>")){//It ends with a right ending five-gram
				tail = true;
			}else{
				for(int i = length-4;i<length;i++){
					current_end+=words[i]+" ";
				}
				current_end+="</S>";
				String query = "select five_gram from liuliu.endsymbol_5grams"
					+" where five_gram = '"+current_end+"' limit 1";
				ResultSet rs = db.execute(query);
				if(rs.next()){
					tail = true;
				}else
					tail = false;
			}
			
			
		}catch(SQLException e){
			e.printStackTrace();
		}
		if(head==true){
			if(tail==true)
				return 3; // complete
			else 
				return 1; //head complete
		}else{
			if(tail==true)
				return 2; //tail complete
			else
				return 0;  //not complete
		}			
	}
	
	
	
	public boolean checkGrammar(String sentence){
		return grammar.checkGrammar(sentence);
	}
	
	public boolean checkPOS(String sentence, String target_word, String target_pos, Database db){
		return pos.tag(sentence, target_word, target_pos, db);
	}
	
	public boolean checkPOSDictStory(String sentence, String target_word, String target_pos, Database db){
		return pos.tagDictStory(sentence, target_word, target_pos, db);
	}
	
	/* disable current NER filter*/
	public boolean checkNER(String sentence){
		return ner.tag(sentence);
	}
	
	
	public ArrayList<String> checkNERinBatch(ArrayList<String> sentences_toner){
		try{
		
				NER_Callable n = new NER_Callable();
				n.setSentences(sentences_toner);
				ExecutorService exec = Executors.newCachedThreadPool();
				Future<ArrayList<String>> results = exec.submit(n);
				ArrayList<String> nerResults = results.get();
				return nerResults;
			}catch(InterruptedException e){
				e.printStackTrace();
				return null;
			}catch(ExecutionException e){
				e.printStackTrace();
				return null;
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
	}
	
	/**
	 * @param sentence: sentence to check
	 * @return: whether this sentence satisfies with our criteria
	 * criteria: 
	  * not candidates, but could be extended
	 * (1) contain at least three content words, including target word
	 * (2) contain at least one related words (this criteria satisfies as the beginning five-grams contains at least a related word)
	 */
	public boolean filters_whethercandidate_count_filters(int[][] HF, int level, String sentence, Database db){
		String[] words = sentence.split("\t")[0].split(" ");
		
		//Holistic filters
		if(!checkStartEnd(words)){
			HF[level-5][0]++;
			if(rn.nextDouble()<0.01){
				System.out.println("HF0: "+sentence);
			}
			return false;
		}
			
		if(!checkSpecialVerbs(words)){
			HF[level-5][1]++;
			//if(rn.nextDouble()<0.01){
				System.out.println("HF1: "+sentence);
			//}
			return false;
		}
		
		if(!checkContentWords(words, db)){
			HF[level-5][2]++;
			if(rn.nextDouble()<0.01){
				System.out.println("HF2: "+sentence);
			}
			return false;
		}
		
		return true;
	}
	
	public boolean filters_whethercandidate(String sentence, String target_word, String target_pos, Database db){
		String[] words = sentence.split("\t")[0].split(" ");
		
		//Holistic filters
		/*use this filter separately*/
		if(!checkStartEnd(words)){
			return false;
		}
			
		if(!checkSpecialVerbs(words)){
			return false;
		}
		
		if(!checkContentWords(words, db)){
			return false;
		}
		
		if(!checkGrammar(sentence)){
			return false;
		}
		
		/*July 10th: diable POS filter, since Moddy seems not consider different POS a problem*/
//		if(!checkPOS(sentence, target_word, target_pos, db)){
//			return false;
//		}
		
//		if(!checkNER(sentence)){
//			return false;
//		}
		
		return true;
	}
	
	public boolean filters_whethercandidate_StartEnd(String sentence, String target_word, String target_pos, Database db){
		String[] words = sentence.split("\t")[0].split(" ");
		
		//Holistic filters
		/*use this filter separately*/
//		if(!checkStartEnd(words)){
//			return false;
//		}
			
		if(!checkSpecialVerbs(words)){
			return false;
		}
		
		if(!checkContentWords(words, db)){
			return false;
		}
		
		if(!checkGrammar(sentence)){
			return false;
		}
		
		/*July 10th: diable POS filter, since Moddy seems not consider different POS a problem*/
//		if(!checkPOS(sentence, target_word, target_pos, db)){
//			return false;
//		}
		
//		if(!checkNER(sentence)){
//			return false;
//		}
		
		return true;
	}
	
	/**
	 * @param sentence: sentence to check
	 * @return: whether this sentence satisfies with our criteria
	 * criteria: 
	 *
	 * not candidates, couldn't be extended
	 * (1) contain more than five(contain five) capitalized words
	 * (2) contain caplitalized words and numbers
	 * (3) contain taboo words -> taboo words are checked when we expand the five-grams
	 * (4) contain special punctuations
	 * (5) contain more than three webspecific words
	 * (6) contain more than 2 words(include 2) above word level 2
	 */
	public boolean fitlers_whetherextend_count_filters(int[][] MNF, int[][] MSF, int level, String target_word, String[] words, RelatedWords rw , Database db){
		//monotonically necessary filters
		String sentence = "";
		for(int i = 0;i<words.length;i++){
			sentence+=words[i]+" ";
		}
		if(!checkTaboo(words)){
			MNF[level-5][1]++;
			
				System.out.println("MNF1: "+sentence);
			
			return false;
		}
		if(!checkPunctuations(words)){
			MNF[level-5][2]++;
			if(rn.nextDouble()<0.01){
				System.out.println("MNF2: "+sentence);
			}
			return false;
		}
		if(!checkWebspecific(words)){
			MNF[level-5][3]++;
			
				System.out.println("MNF3: "+sentence);
			return false;
		}
		if(!checkPronouns(words)){
			MNF[level-5][4]++;
			//if(rn.nextDouble()<0.01){
				System.out.println("MNF4: "+sentence);
			//}
			return false;
		}
		
		//monotonically sufficient filters
		if(!checkCapitalize(words)){
			MSF[level-5][0]++;
			if(rn.nextDouble()<0.01){
				System.out.println("MNF5: "+sentence);
			}
			return false;
		}
		if(!checkWordLevel(target_word, words, rw, db)){
			MSF[level-5][1]++;
			if(rn.nextDouble()<0.01){
				System.out.println("MNF6: "+sentence);
			}
			return false;
		}
		
		return true;
	}
	
	public boolean fitlers_whetherextend(String target_word, String[] words, RelatedWords rw , Database db){
		//monotonically necessary filters
		if(!checkTaboo(words)){
			return false;
		}
		if(!checkPunctuations(words)){
			return false;
		}
		if(!checkWebspecific(words)){
			return false;
		}
		if(!checkPronouns(words)){
			return false;
		}
		
		//monotonically sufficient filters
		if(!checkCapitalize(words)){
			return false;
		}
		if(!checkWordLevel(target_word, words, rw, db)){
			return false;
		}
		
		return true;
	}
	
		
	public void cluster_start5grams(ArrayList<String> fivegrams, Database db, int number){
		
		try{	
			ArrayList<SentenceCluster> currentscs = new ArrayList<SentenceCluster>();
			for(int k = 0;k<fivegrams.size();k++){
				//for each five-gram, the first step is to check whether this five-gram could parse the filters			
				String line = fivegrams.get(k);
					int numberOfclusters = currentscs.size();
					int numSelectedClusters = 0;
					for(int i = 0;i<numberOfclusters;i++){
						
						//compare the new sentence with each cluster
						SentenceCluster sc = currentscs.get(i);
						HashSet<String> scCommonWords = sc.getCommonWordsHash();  //get the common words of the cluster
						//compare the sentence with the common words, and calculate the number of words in common
						int numOfCommon = 0;
						String newCommonWords = "";
						String[] words = line.split("\t")[0].toLowerCase().split(" ");
						//here, we should compare lowercase, base forms of the words
						//So first, we need to change the words in sentence into their baseforms
						
						for(int j = 0;j<words.length;j++){
							String query = "select baseform from liuliu.word_base_pos where word = '"+words[j].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
							ResultSet rs = db.execute(query);
							String base;
							if(rs.next()){ // we only choose the most frequently used baseform
								base = rs.getString("baseform");
							}else{
								base = words[j].toLowerCase();
							}
							
							if(scCommonWords.contains(base)){
								newCommonWords+=base+" ";
								numOfCommon++;
							}
						}
						
						//judge whether the sentence belongs to the cluster
						//if the number of common words is above the threshold, then it is
						//otherwise, try the next cluster
						if(numOfCommon>=number){
							sc.addSentence(line);
							//sc.setCommonWordsString(newCommonWords.trim());
							sc.setCommonWordsHash(newCommonWords.trim());
							currentscs.set(i, sc);
							numSelectedClusters++;
							//if we require that a sentence can and only can be clustered into one cluster
							//then here we break
							//otherwise, we continue checking whether this sentence can be clustered into other cluster or not
							break;
							
						}else{
							//try the next cluster
						}
					}
					
					//if the senentence doesn't belong to any cluster
					//add a new cluster to the final results
					if(numSelectedClusters==0){
						SentenceCluster sc2 = new SentenceCluster();
							sc2.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
							String commonWords[] = line.split("\t")[0].toLowerCase().split(" ");
							String commonWordsHash = "";
							for(int i = 0;i<commonWords.length;i++){
								String word = commonWords[i];
								//exclusion: not punctuations, not <S> and </S>
								if(word.matches("[a-z]+")){
									String query = "select baseform from liuliu.word_base_pos where word = '"+word.replaceAll("'", "\\\\'")+"' order by frequency desc";
		 							ResultSet rs = db.execute(query);
		 							if(rs.next()){
		 								String base = rs.getString("baseform");
		 								commonWordsHash +=base+" ";
		 							}else{
		 								commonWordsHash +=word.toLowerCase()+" ";
		 							}
								}
							}
							sc2.setCommonWordsHash(commonWordsHash);
						currentscs.add(sc2);
					}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
			
		}
	
	
	/**
	 * @param fivegrams
	 * @param db
	 * @param number
	 * @author liuliu
	 * @time 2009/06/02
	 * @purpose: try the new clustering method with inverted indexing
	 */
	public void cluster_start5grams_speedUp(ArrayList<String> fivegrams, Database db, int number){
		Hashtable<String, ArrayList<Integer>> word_cluster = new Hashtable<String, ArrayList<Integer>>();
		try{	
			ArrayList<SentenceCluster> currentscs = new ArrayList<SentenceCluster>();
			for(int i = 0;i<fivegrams.size();i++){
				//for each five-gram, the first step is to check whether this five-gram could parse the filters			
				String line = fivegrams.get(i);
				String[] words = line.split("\t")[0].toLowerCase().split(" ");
				ArrayList<String> base_words = new ArrayList<String>();
				//get the content words;
				for(int j = 0;j<words.length;j++){
					String query = "select baseform from liuliu.word_base_pos where word = '"+words[j].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
					ResultSet rs = db.execute(query);
					String base;
					if(rs.next()){ // we only choose the most frequently used baseform
						base = rs.getString("baseform");
					}else{
						base = words[j].toLowerCase();
					}
					//for start five-grams, we also consider the function words
//					if(functionWords.contains(base))
//						continue;
//					else
					base_words.add(base);
				}
				
//				if(content_words.size()<number){
//					number = content_words.size();
//				}
				
				int numberOfclusters = currentscs.size();
				int putToClusterIndex = -1;
				Hashtable<Integer, Integer> cluster_number = new Hashtable<Integer, Integer>();
				for(int j = 0;j<base_words.size();j++){
					int flag = 0; // 1 indicate we find the cluster to put the sentence
					
					if(word_cluster.containsKey(base_words.get(j))){
						ArrayList<Integer> related_clusters = word_cluster.get(base_words.get(j));
						for(int k = 0;k<related_clusters.size();k++){
							int cluster_index = related_clusters.get(k);
							if(cluster_number.containsKey(cluster_index)){
								int numOfWordsInCluster = cluster_number.get(cluster_index)+1;
								if(numOfWordsInCluster>=number){
									//to do
									//the cluster with index cluster_index is the one to put this sentence
									putToClusterIndex = cluster_index;
									flag = 1;
									break;
								}
								else
									cluster_number.put(cluster_index,numOfWordsInCluster);
							}else{
								cluster_number.put(cluster_index,1);
								if(number<=1)//only contain one content word
								{
									putToClusterIndex = cluster_index;
									flag = 1;
									break;
								}
							}
						}
						
					}
					if(flag==1)
						break;						
				}
				
				//judge whether we find a cluster to put the sentence in
				if(putToClusterIndex!=-1){//we find one cluster
					SentenceCluster sc = currentscs.get(putToClusterIndex);
					sc.addSentence(line);
					currentscs.set(putToClusterIndex, sc);
				}else{// we don't find any cluster to put the sentence
					//set the first three content words as the common words
					//create a new cluster
					String commonWords = "";
					//update the word_cluster inverted indexing
					
					for(int j=0;j<number;j++){
						commonWords+=base_words.get(j)+" ";
						if(word_cluster.containsKey(base_words.get(j))){
							ArrayList<Integer> related_clusters = word_cluster.get(base_words.get(j));
							related_clusters.add(numberOfclusters);
							word_cluster.put(base_words.get(j), related_clusters);
						}else{
							ArrayList<Integer> related_clusters = new ArrayList<Integer>();
							related_clusters.add(numberOfclusters);
							word_cluster.put(base_words.get(j), related_clusters);
						}
					}
					SentenceCluster sc = new SentenceCluster();
						sc.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
						sc.setCommonWordsHash(commonWords.trim());
						currentscs.add(sc);
					
					
				}
				
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
			
	}
		
		
		/**
		 * @param oldfilename: sentences examples before clustering
		 * @param newfilename: clustered sentence examples
		 * @param number: the number of words which are in common
		 * @param db
		 */
		public void cluster(String oldfilename, String newfilename, int number, Database db){
			try{
				File fold = new File(oldfilename);
				FileReader fr = new FileReader(fold);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while((line=br.readLine())!=null){
					if(scs.size()!=0)//which means there are several clusters now	
					{
						int numberOfclusters = scs.size();
						int numSelectedClusters = 0;
						for(int i = 0;i<numberOfclusters;i++){
							
							//compare the new sentence with each cluster
							SentenceCluster sc = scs.get(i);
							HashSet<String> scCommonWords = sc.getCommonWordsHash();  //get the common words of the cluster
							//compare the sentence with the common words, and calculate the number of words in common
							int numOfCommon = 0;
							int numOfNotCommon = 0;
							String newCommonWords = "";
							String[] words = line.split("\t")[0].toLowerCase().split(" ");
							//here, we should compare lowercase, base forms of the words
							//So first, we need to change the words in sentence into their baseforms
							
							for(int j = 0;j<words.length;j++){
								String query = "select baseform from liuliu.word_base_pos where word = '"+words[j].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
								ResultSet rs = db.execute(query);
								String base;
								if(rs.next()){ // we only choose the most frequently used baseform
									base = rs.getString("baseform");
								}else{
									base = words[j].toLowerCase();
								}
								
								if(scCommonWords.contains(base)){
									newCommonWords+=base+" ";
									numOfCommon++;
								}else{
									numOfNotCommon++;
								}
							}
							
							//judge whether the sentence belongs to the cluster
							//if the number of common words is above the threshold, then it is
							//otherwise, try the next cluster
							if(numOfCommon>=number){
								sc.addSentence(line);
								sc.setCommonWordsString(newCommonWords.trim());
								sc.setCommonWordsHash(newCommonWords.trim());
								scs.set(i, sc);
								numSelectedClusters++;
								//if we require that a sentence can and only can be clustered into one cluster
								//then here we break
								//otherwise, we continue checking whether this sentence can be clustered into other cluster or not
								break;
								
							}else{
								//try the next cluster
							}
						}
						
						//if the senentence doesn't belong to any cluster
						//add a new cluster to the final results
						if(numSelectedClusters==0){
							SentenceCluster sc2 = new SentenceCluster(number);
	 						sc2.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
	 						String commonWords[] = line.split("\t")[0].toLowerCase().split(" ");
	 						String commonWordsString = "";
	 						String commonWordsHash = "";
	 						for(int i = 0;i<commonWords.length;i++){
	 							String word = commonWords[i].toLowerCase();
	 							//exclusion: not punctuations, not <S> and </S>
	 							if(word.matches("[a-z]+")){
	 								String query = "select baseform from liuliu.word_base_pos where word = '"+word.replaceAll("'", "\\\\'")+"' order by frequency desc";
	 	 							ResultSet rs = db.execute(query);
	 	 							
	 	 							if(rs.next()){
	 	 								String base = rs.getString("baseform");
	 	 								commonWordsString +=base+" ";
	 	 								commonWordsHash +=base+" ";
	 	 							}else{
	 	 								commonWordsString +=word.toLowerCase()+" ";
	 	 	 							commonWordsHash +=word.toLowerCase()+" ";
	 	 							}
	 							}
	 						}
	 						sc2.setCommonWordsHash(commonWordsHash);
	 						sc2.setCommonWordsString(commonWordsString);
							scs.add(sc2);
						}
						
					}else{//There is no clusters at all. The first sentence in the file
						int commonWordsNumber = 5;   
						SentenceCluster sc = new SentenceCluster(commonWordsNumber);
						sc.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
							String commonWords[] = line.split("\t")[0].toLowerCase().split(" ");
							String commonWordsString = "";
							String commonWordsHash = "";
							for(int i = 0;i<commonWords.length;i++){
								String word = commonWords[i].toLowerCase();
								if(word.matches("[a-z]+")){
	 								String query = "select baseform from liuliu.word_base_pos where word = '"+word.replaceAll("'", "\\\\'")+"' order by frequency desc";
	 	 							ResultSet rs = db.execute(query);
	 	 							
	 	 							if(rs.next()){
	 	 								String base = rs.getString("baseform");
	 	 								commonWordsString +=base+" ";
	 	 								commonWordsHash +=base+" ";
	 	 							}else{
	 	 								commonWordsString +=word.toLowerCase()+" ";
	 	 	 							commonWordsHash +=word.toLowerCase()+" ";
	 	 							}
	 							}
							}
							sc.setCommonWordsHash(commonWordsHash);
							sc.setCommonWordsString(commonWordsString);
						scs.add(sc);
					}
					
				}
				
				//write sentences in each cluster into the new file
				File fnew = new File(newfilename);
				FileWriter fw = new FileWriter(fnew);
				BufferedWriter bw = new BufferedWriter(fw);
				for(int i = 0;i<scs.size();i++){
					SentenceCluster sc = scs.get(i);
					bw.write("--------------------------------Cluster "+i+"-----------------------------------\n");
					bw.write(sc.getCommonWordsHash().toString()+"\n");
					
					for(int j = 0;j<sc.getNumSentence();j++){
						bw.write(sc.getSentence(j)+"\n");
					}
					bw.write("\n");
				}
				bw.close();
				
			}catch(IOException e){
				e.printStackTrace();
			}catch(SQLException e){
				
			}
			
		}

		
		
		
		/**
		 * @param oldfilename: file with context candidates
		 * @param newfilename: file with clustered results
		 * @param number: number of content words 
		 * @param db
		 * 
		 * @author liuliu
		 * @time 2009/06/01
		 * @purpose: try the new clustering method with inverted indexing
		 */
		public void cluster_content_words(String oldfilename, String newfilename, int number, Database db){
			Hashtable<String, ArrayList<Integer>> word_cluster = new Hashtable<String, ArrayList<Integer>>();
			//word_cluster is the inverted indexing for words
			// word -> arraylist of cluster numbers containing the word
			try{
				File fold = new File(oldfilename);
				FileReader fr = new FileReader(fold);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while((line=br.readLine())!=null){
					
					
					String[] words = line.split("\t")[0].toLowerCase().split(" ");
					ArrayList<String> content_words = new ArrayList<String>();
					//get the content words;
					for(int j = 0;j<words.length;j++){
						String query = "select baseform from liuliu.word_base_pos where word = '"+words[j].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
						ResultSet rs = db.execute(query);
						String base;
						if(rs.next()){ // we only choose the most frequently used baseform
							base = rs.getString("baseform");
						}else{
							base = words[j].toLowerCase();
						}
						if(functionWords.contains(base))
							continue;
						else
							content_words.add(base);
					}
					
					if(content_words.size()<number){
						number = content_words.size();
					}
					
					int numberOfclusters = scs.size();
					int putToClusterIndex = -1;
					Hashtable<Integer, Integer> cluster_number = new Hashtable<Integer, Integer>();
					for(int j = 0;j<content_words.size();j++){
						int flag = 0; // 1 indicate we find the cluster to put the sentence
						
						if(word_cluster.containsKey(content_words.get(j))){
							ArrayList<Integer> related_clusters = word_cluster.get(content_words.get(j));
							for(int k = 0;k<related_clusters.size();k++){
								int cluster_index = related_clusters.get(k);
								if(cluster_number.containsKey(cluster_index)){
									int numOfWordsInCluster = cluster_number.get(cluster_index)+1;
									if(numOfWordsInCluster>=number){
										//to do
										//the cluster with index cluster_index is the one to put this sentence
										putToClusterIndex = cluster_index;
										flag = 1;
										break;
									}
									else
										cluster_number.put(cluster_index,numOfWordsInCluster);
								}else{
									cluster_number.put(cluster_index,1);
									if(number<=1)//only contain one content word
									{
										putToClusterIndex = cluster_index;
										flag = 1;
										break;
									}
								}
							}
							
						}
						if(flag==1)
							break;						
					}
					
					//judge whether we find a cluster to put the sentence in
					if(putToClusterIndex!=-1){//we find one cluster
						SentenceCluster sc = scs.get(putToClusterIndex);
						sc.addSentence(line);
						scs.set(putToClusterIndex, sc);
					}else{// we don't find any cluster to put the sentence
						//set the first three content words as the common words
						//create a new cluster
						String commonWords = "";
						//update the word_cluster inverted indexing
						
						for(int j=0;j<number;j++){
							commonWords+=content_words.get(j)+" ";
							if(word_cluster.containsKey(content_words.get(j))){
								ArrayList<Integer> related_clusters = word_cluster.get(content_words.get(j));
								related_clusters.add(numberOfclusters);
								word_cluster.put(content_words.get(j), related_clusters);
							}else{
								ArrayList<Integer> related_clusters = new ArrayList<Integer>();
								related_clusters.add(numberOfclusters);
								word_cluster.put(content_words.get(j), related_clusters);
							}
						}
						SentenceCluster sc = new SentenceCluster();
 						sc.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
 						sc.setCommonWordsHash(commonWords.trim());
						scs.add(sc);
						
						
					}
					
				}
				
				//write sentences in each cluster into the new file
				File fnew = new File(newfilename);
				FileWriter fw = new FileWriter(fnew);
				BufferedWriter bw = new BufferedWriter(fw);
				for(int i = 0;i<scs.size();i++){
					SentenceCluster sc = scs.get(i);
					bw.write("--------------------------------Cluster "+i+"-----------------------------------\n");
					bw.write(sc.getCommonWordsHash().toString()+"\n");
					
					for(int j = 0;j<sc.getNumSentence();j++){
						bw.write(sc.getSentence(j)+"\n");
					}
					bw.write("\n");
				}
				bw.close();
				scs.clear();
			}catch(IOException e){
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		/**
		 * @param oldfilename: file with context candidates
		 * @param newfilename: file with clustered results
		 * @param number: number of content words 
		 * @param db
		 * 
		 * @author liuliu
		 * @time 2009/06/01
		 * @purpose: try the new clustering method with inverted indexing
		 * 		     gradually decide the common words for each cluster
		 */
		public void cluster_content_words_temp(String oldfilename, String newfilename, int number, Database db){
			Hashtable<String, ArrayList<Integer>> word_cluster = new Hashtable<String, ArrayList<Integer>>();
			//word_cluster is the inverted indexing for words
			// word -> arraylist of cluster numbers containing the word
			try{
				File fold = new File(oldfilename);
				FileReader fr = new FileReader(fold);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while((line=br.readLine())!=null){
					
					
					String[] words = line.split("\t")[0].toLowerCase().split(" ");
					ArrayList<String> content_words = new ArrayList<String>();
					//get the content words;
					for(int j = 0;j<words.length;j++){
						String query = "select baseform from liuliu.word_base_pos where word = '"+words[j].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
						ResultSet rs = db.execute(query);
						String base;
						if(rs.next()){ // we only choose the most frequently used baseform
							base = rs.getString("baseform");
						}else{
							base = words[j].toLowerCase();
						}
						if(functionWords.contains(base))
							continue;
						else
							content_words.add(base);
					}
					
					if(content_words.size()<number){
						number = content_words.size();
					}
					
					int numberOfclusters = scs.size();
					int putToClusterIndex = -1;
					
					Hashtable<Integer, Integer> cluster_number = new Hashtable<Integer, Integer>(); //key: cluster index; value: number of words assigned to this cluster
					Hashtable<Integer, HashSet<String>> cluster_words = new Hashtable<Integer, HashSet<String>>();
					int maxNum = Integer.MIN_VALUE;  //maximum number of common words between the sentence and one cluster
					int max_cluster_index = -1;		 //index of the cluster
					for(int j = 0;j<content_words.size();j++){
						if(word_cluster.containsKey(content_words.get(j))){
							ArrayList<Integer> related_clusters = word_cluster.get(content_words.get(j));
							for(int k = 0;k<related_clusters.size();k++){
								int cluster_index = related_clusters.get(k);
								if(cluster_number.containsKey(cluster_index)){
									int numOfWordsInCluster = cluster_number.get(cluster_index)+1;
									cluster_number.put(cluster_index,numOfWordsInCluster);
									HashSet<String> common_words = cluster_words.get(cluster_index);
									common_words.add(content_words.get(j));
									cluster_words.put(cluster_index, common_words);
									
									if(numOfWordsInCluster>maxNum){
										maxNum = numOfWordsInCluster;
										max_cluster_index = cluster_index;
									}
								}else{
									cluster_number.put(cluster_index,1);
									HashSet<String> common_words = new HashSet<String>();
									common_words.add(content_words.get(j));
									cluster_words.put(cluster_index, common_words);
									if(maxNum<1){
										maxNum = 1;
										max_cluster_index = cluster_index;
									}
								}
							}	
						}				
					}
					
					//The cluster with index max_cluster_index is the one the sentence should belong to
					if(maxNum>=number){
						putToClusterIndex = max_cluster_index;
					}
					
					//judge whether we find a cluster to put the sentence in
					if(putToClusterIndex!=-1){//we find one cluster
						SentenceCluster sc = scs.get(putToClusterIndex);
						sc.addSentence(line);
						//get previous common words; change the inverted indexing
						HashSet<String> previous_commonWords = sc.getCommonWordsHash();
						HashSet<String> new_commonWords = cluster_words.get(putToClusterIndex);
						for(String word:previous_commonWords){
							if(!new_commonWords.contains(word)){
								ArrayList<Integer> related_clusters = word_cluster.get(word);
								int index_in_list = related_clusters.indexOf(putToClusterIndex);
								related_clusters.remove(index_in_list);
								word_cluster.put(word, related_clusters);
							}
						}
						
						//set the new common words
						String commonWords = "";
						for(String word:cluster_words.get(putToClusterIndex)){
							commonWords+=word+" ";
						}
						sc.setCommonWordsHash(commonWords.trim());
						scs.set(putToClusterIndex, sc);
						
					}else{// we don't find any cluster to put the sentence
						//set all the content words in this sentence as the common words
						//create a new cluster
						String commonWords = "";
						//update the word_cluster inverted indexing
						
						for(int j=0;j<content_words.size();j++){
							commonWords+=content_words.get(j)+" ";
							if(word_cluster.containsKey(content_words.get(j))){
								ArrayList<Integer> related_clusters = word_cluster.get(content_words.get(j));
								related_clusters.add(numberOfclusters);
								word_cluster.put(content_words.get(j), related_clusters);
							}else{
								ArrayList<Integer> related_clusters = new ArrayList<Integer>();
								related_clusters.add(numberOfclusters);
								word_cluster.put(content_words.get(j), related_clusters);
							}
						}
						SentenceCluster sc = new SentenceCluster();
 						sc.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
 						sc.setCommonWordsHash(commonWords.trim());
						scs.add(sc);
							
					}

				}
				
				//write sentences in each cluster into the new file
				File fnew = new File(newfilename);
				FileWriter fw = new FileWriter(fnew);
				BufferedWriter bw = new BufferedWriter(fw);
				for(int i = 0;i<scs.size();i++){
					SentenceCluster sc = scs.get(i);
					bw.write("--------------------------------Cluster "+i+"-----------------------------------\n");
					bw.write(sc.getCommonWordsHash().toString()+"\n");
					
					for(int j = 0;j<sc.getNumSentence();j++){
						bw.write(sc.getSentence(j)+"\n");
					}
					bw.write("\n");
				}
				bw.close();
				scs.clear();
			}catch(IOException e){
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		
		/**
		 * @param oldfilename: sentences examples before clustering
		 * @param newfilename: clustered sentence examples
		 * @param number: the number of words which are in common
		 * @param db
		 */
		public void cluster_without_functionwords(String oldfilename, String newfilename, int number, Database db){
			try{
				File fold = new File(oldfilename);
				FileReader fr = new FileReader(fold);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while((line=br.readLine())!=null){
					
					
					String[] words = line.split("\t")[0].toLowerCase().split(" ");
//					check whether this example ending with "be", "can" or "will";
//					if(specialverb.contains(words[4].toLowerCase()))
//						break;
					
						int numberOfclusters = scs.size();
						int numSelectedClusters = 0;
						for(int i = 0;i<numberOfclusters;i++){
							
							//compare the new sentence with each cluster
							SentenceCluster sc = scs.get(i);
							HashSet<String> scCommonWords = sc.getCommonWordsHash();  //get the common words of the cluster
							//compare the sentence with the common words, and calculate the number of words in common
							int numOfCommon = 0;
							int numOfNotCommon = 0;
							String newCommonWords = "";
							
							//here, we should compare lowercase, base forms of the words
							//So first, we need to change the words in sentence into their baseforms
							
							for(int j = 0;j<words.length;j++){
								String query = "select baseform from liuliu.word_base_pos where word = '"+words[j].toLowerCase().replaceAll("'", "\\\\'")+"' order by frequency desc";
								ResultSet rs = db.execute(query);
								String base;
								if(rs.next()){ // we only choose the most frequently used baseform
									base = rs.getString("baseform");
								}else{
									base = words[j].toLowerCase();
								}
								if(functionWords.contains(base))
									continue;
								if(scCommonWords.contains(base)){
									newCommonWords+=base+" ";
									numOfCommon++;
								}
							}
							
							//judge whether the sentence belongs to the cluster
							//if the number of common words is above the threshold, then it is
							//otherwise, try the next cluster
							if(numOfCommon>=number){
								sc.addSentence(line);
								sc.setCommonWordsHash(newCommonWords.trim());
								scs.set(i, sc);
								numSelectedClusters++;
								//if we require that a sentence can and only can be clustered into one cluster
								//then here we break
								//otherwise, we continue checking whether this sentence can be clustered into other cluster or not
								break;
								
							}else{
								//try the next cluster
							}
						}
						
						//if the senentence doesn't belong to any cluster
						//add a new cluster to the final results
						if(numSelectedClusters==0){
							SentenceCluster sc2 = new SentenceCluster(number);
	 						sc2.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
	 						String commonWords[] = line.split("\t")[0].toLowerCase().split(" ");
	 						String commonWordsHash = "";
	 						for(int i = 0;i<commonWords.length;i++){
	 							String word = commonWords[i];
	  							if(word.matches("[a-z]+")){
	 								String query = "select baseform from liuliu.word_base_pos where word = '"+word.replaceAll("'", "\\\\'")+"' order by frequency desc";
	 	 							ResultSet rs = db.execute(query);
	 	 							String base;
	 	 							if(rs.next()){
	 	 								base = rs.getString("baseform");
	 	 								
	 	 							}else{
	 	 								base = word.toLowerCase();
	 	 							}
	 	 							if(functionWords.contains(base)){
	 	 								continue;
	 	 							}else{
	 	 								commonWordsHash +=base+" ";
	 	 							}
	 	 							
	 							}
	 						}
	 						sc2.setCommonWordsHash(commonWordsHash.trim());
	 						scs.add(sc2);
						}
				}
				
				//write sentences in each cluster into the new file
				File fnew = new File(newfilename);
				FileWriter fw = new FileWriter(fnew);
				BufferedWriter bw = new BufferedWriter(fw);
				for(int i = 0;i<scs.size();i++){
					SentenceCluster sc = scs.get(i);
					bw.write("--------------------------------Cluster "+i+"-----------------------------------\n");
					bw.write(sc.getCommonWordsHash().toString()+"\n");
					
					for(int j = 0;j<sc.getNumSentence();j++){
						bw.write(sc.getSentence(j)+"\n");
					}
					bw.write("\n");
				}
				bw.close();
				
			}catch(IOException e){
				e.printStackTrace();
			}catch(SQLException e){
				
			}
			
		}

		//Score funciton 1: average statistics
		
		public void findGoodExample(Database db, String target_word, String cluster_example_file, String good_example_file, String wholefile){
			try{
				HashSet<String> relatedWords = new HashSet<String>();
				//readRelatedWordList(target_word, relatedWords, related_word_file);
				readTop100RelatedWordList(target_word, relatedWords, db);
				File f = new File(cluster_example_file);
				FileReader fr = new FileReader(f);
				BufferedReader br= new BufferedReader(fr);
				File fexample = new File(good_example_file);
				FileWriter fw = new FileWriter(fexample);
				BufferedWriter bw = new BufferedWriter(fw);
				
				FileWriter fwhole = new FileWriter(wholefile, false);
				
				String line = "";
				SentenceCluster scwhole = new SentenceCluster();
				Hashtable<String, Float> whole_sentence_score = new Hashtable<String, Float>();
				while ((line = br.readLine())!=null){
					//handle each cluster
					if(line.contains("--------")){
						bw.write(line+"\n");
						line = br.readLine(); //this is the common words
						bw.write(line+"\n");
						Hashtable<String, Float> sentence_score = new Hashtable<String, Float>();
						int maxfrequency = 0;
						SentenceCluster sc = new SentenceCluster();
						while((line = br.readLine()).length()>1){
//							web specific words, the less the better
							String[] words = line.split("\t")[0].split(" ");
							int numWebWord = calNumWebWord(words);
							if(numWebWord>=3) //The sentence is too web specific
								continue;
							sc.addSentence(line);
							
							scwhole.num_sentence++;
							//sentence length, the longer the better
							sc.sentence_length+= calSentenceLength(line);
							scwhole.sentence_length+= calSentenceLength(line);
							//content words, the more the better
							sc.num_contentword+= calNumContentWord(line);
							scwhole.num_contentword+= calNumContentWord(line);
							//related words, the more the better
							sc.num_relatedword+= calNumRelatedWord(line, target_word, relatedWords);
							scwhole.num_relatedword+= calNumRelatedWord(line, target_word, relatedWords);
							
							//sentence frequency
							sc.average_frequency+=Integer.valueOf(line.split("\t")[1]);
							scwhole.average_frequency+=Integer.valueOf(line.split("\t")[1]);
							
						}
						sc.sentence_length/= sc.num_sentence;
						//content words, the more the better
						sc.num_contentword/= sc.num_sentence;
						//related words, the more the better
						sc.num_relatedword/= sc.num_sentence;
						//sentence frequency
						sc.average_frequency/=sc.num_sentence;
						
						for(int i = 0;i<sc.num_sentence;i++){
							//sentence length, the longer the better
							float score = 0;
							int sentenceLength = calSentenceLength(sc.getSentence(i));
							//content words, the more the better
							int numContentWord = calNumContentWord(sc.getSentence(i));
							//related words, the more the better
							int numRelatedword = calNumRelatedWord(sc.getSentence(i), target_word, relatedWords);
							//sentence frequency
							int frequency =Integer.valueOf(sc.getSentence(i).split("\t")[1]);
							
							
							float contentwordscore = 0;
							float relatedwordscore = 0;
							
							
							if(sc.num_contentword==0){
								contentwordscore = 0;
							}else{
								contentwordscore = (float)numContentWord/sc.num_contentword;
							}
							
							if(sc.num_relatedword==0){
								relatedwordscore = 0;
							}else{
								relatedwordscore = (float)numRelatedword/sc.num_relatedword;
							}
							
							score = (float)sentenceLength/sc.sentence_length+contentwordscore
							+relatedwordscore+(float)frequency/sc.average_frequency; //equal weights
							sentence_score.put(sc.getSentence(i), (float)score/(float)4);
						}
						Map.Entry[] entries = SortHashtableByValue.sort(sentence_score);
						
						for(int i = 0;i<entries.length;i++){
							bw.write(entries[i].getKey().toString().split("\t")[0]+"\t"+entries[i].getValue()+"\n");
							if(i<1){
								scwhole.addSentence((String)entries[i].getKey());
								scwhole.num_sentence--;
							}
						}
						bw.write("\n");
						
					}
					
				}
				bw.close();
				
				if(scwhole.num_sentence==0)
					return;
				scwhole.sentence_length/= scwhole.num_sentence;
				//content words, the more the better
				scwhole.num_contentword/= scwhole.num_sentence;
				//related words, the more the better
				scwhole.num_relatedword/= scwhole.num_sentence;
				//sentence frequency
				scwhole.average_frequency/=scwhole.num_sentence;
				
				for(int i = 0;i<scwhole.sentences.size();i++){
					float score = 0;
					int sentenceLength = calSentenceLength(scwhole.getSentence(i));
					//content words, the more the better
					int numContentWord = calNumContentWord(scwhole.getSentence(i));
					//related words, the more the better
					int numRelatedword = calNumRelatedWord(scwhole.getSentence(i), target_word, relatedWords);
					//sentence frequency
					int frequency =Integer.valueOf(scwhole.getSentence(i).split("\t")[1]);
					
					float contentwordscore = 0;
					float relatedwordscore = 0;
					
					
					if(scwhole.num_contentword==0){
						contentwordscore = 0;
					}else{
						contentwordscore = (float)numContentWord/scwhole.num_contentword;
					}
					
					if(scwhole.num_relatedword==0){
						relatedwordscore = 0;
					}else{
						relatedwordscore = (float)numRelatedword/scwhole.num_relatedword;
					}
					
					score = (float)sentenceLength/scwhole.sentence_length+contentwordscore+relatedwordscore+(float)frequency/scwhole.average_frequency; //equal weights
					whole_sentence_score.put(scwhole.getSentence(i), (float)score/(float)4);
				
				}
				Map.Entry[] entries = SortHashtableByValue.sort(whole_sentence_score);
				
				for(int i = 0;i<entries.length;i++){
					fwhole.write(entries[i].getKey().toString().split("\t")[0]+"\t"+entries[i].getValue()+"\n");
				}
				fwhole.write("\n");
				fwhole.close();
				
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		
		
		
		/**
		 * @param key
		 * @param max
		 * @param hash
		 * @return the number of numbers which are bigger than key
		 */
		private int FindNumber(int key, int max, Hashtable<Integer, Integer> hash){
			int index = 0;
			for(int i = max;i>key;i--){
				if(hash.containsKey(i))
					index+=hash.get(i);
			}
			return index;
		}
		
		private int FindLargest(int[] numbers){
			int max = -1;
			for(int i = 0;i<numbers.length;i++){
				if(numbers[i]>max)
					max = numbers[i];
			}
			return max;
		}
		
		
		/**
		 * @param db
		 * @param target_word
		 * @param cluster_example_file
		 * @param good_example_file
		 * @param wholefile
		 * @since 2009/07/20
		 * change preferences features to constraints, such as require each of the feature to rank in top n
		 * as long as a sentence satisfies the above constraints, it's a good sentence
		 * new score function: by rank
		 */
		public void findGoodExample_constraints(Database db, String target_word, String cluster_example_file, String good_example_file, String wholefile){
			try{
				HashSet<String> relatedWords = new HashSet<String>();
				//readRelatedWordList(target_word, relatedWords, related_word_file);
				readTop100RelatedWordList(target_word, relatedWords, db);
				File f = new File(cluster_example_file);
				FileReader fr = new FileReader(f);
				BufferedReader br= new BufferedReader(fr);
				File fexample = new File(good_example_file);
				FileWriter fw = new FileWriter(fexample);
				BufferedWriter bw = new BufferedWriter(fw);
				
				FileWriter fwhole = new FileWriter(wholefile, false);
				
				String line = "";
				SentenceCluster scwhole = new SentenceCluster();
				Hashtable<String, Float> whole_sentence_score = new Hashtable<String, Float>();
				while ((line = br.readLine())!=null){
					//handle each cluster
					if(line.contains("--------")){
						bw.write(line+"\n");
						line = br.readLine(); //this is the common words
						bw.write(line+"\n");
						Hashtable<String, Double> sentence_score = new Hashtable<String, Double>();
						int maxfrequency = 0;
						SentenceCluster sc = new SentenceCluster();
						ArrayList<Integer> sentence_length = new ArrayList<Integer>();
						ArrayList<Integer> sentence_frequency = new ArrayList<Integer>();
						ArrayList<Integer> num_contentword = new ArrayList<Integer>();
						ArrayList<Integer> num_relatedword = new ArrayList<Integer>();
						int maxLength = 0;
						int minLength = 100;
						int maxContentWord = 0;
						int minContentWord = 100;
						int maxRelatedWord = 0;
						int minRelatedWord = 100;
						
						//sentence length
						Hashtable<Integer, Integer> length_count = new Hashtable<Integer, Integer>();
						//number of content words
						Hashtable<Integer, Integer> content_count = new Hashtable<Integer, Integer>();
						//number of related words
						Hashtable<Integer, Integer> related_count = new Hashtable<Integer, Integer>();
						
						while((line = br.readLine()).length()>1){
//							web specific words, the less the better
							String[] words = line.split("\t")[0].split(" ");
							int numWebWord = calNumWebWord(words);
							if(numWebWord>=3) //The sentence is too web specific
								continue;
							sc.addSentence(line);
							
							//sentence length, the longer the better
							int current_sentence_length = calSentenceLength(line);
							int current_num_contentword = calNumContentWord(line);
							int current_num_relatedword = calNumRelatedWord(line, target_word, relatedWords);
							
							if(current_sentence_length<minLength)
								minLength = current_sentence_length;
							if(current_sentence_length>maxLength)
								maxLength = current_sentence_length;
							
							if(current_num_contentword>maxContentWord)
								maxContentWord = current_num_contentword;
							if(current_num_contentword<minContentWord)
								minContentWord = current_num_contentword;
							
							
							if(current_num_relatedword<minRelatedWord)
								minRelatedWord = current_num_relatedword;
							if(current_num_relatedword>maxRelatedWord)
								maxRelatedWord = current_num_relatedword;
							
							sentence_length.add(current_sentence_length);
							num_contentword.add(current_num_contentword);
							num_relatedword.add(current_num_relatedword);
							sentence_frequency.add(Integer.valueOf(line.split("\t")[1]));
							
							
							// add to hashtable
							if(length_count.containsKey(current_sentence_length)){
								length_count.put(current_sentence_length, length_count.get(current_sentence_length)+1);
							}else{
								length_count.put(current_sentence_length, 1);
							}
							
							if(content_count.containsKey(current_num_contentword)){
								content_count.put(current_num_contentword, content_count.get(current_num_contentword)+1);
							}else{
								content_count.put(current_num_contentword, 1);
							}
							
							if(related_count.containsKey(current_num_relatedword)){
								related_count.put(current_num_relatedword, related_count.get(current_num_relatedword)+1);
							}else{
								related_count.put(current_num_relatedword, 1);
							}
							
						}
							
						Integer[] frequencyRank = new Integer[sentence_frequency.size()];
						sentence_frequency.toArray(frequencyRank);

						Arrays.sort(frequencyRank);
						
						int size = frequencyRank.length;
						
						for(int i = 0;i<sc.num_sentence;i++){
							//sentence length, the longer the better
							float score = 0;
							int sentenceLength = sentence_length.get(i);
							//content words, the more the better
							int numContentWord = num_contentword.get(i);
							//related words, the more the better
							int numRelatedWord = num_relatedword.get(i);
							//sentence frequency
							int  frequency =sentence_frequency.get(i);
					
						
							//Get the rank of each feature
							int rankLength = FindNumber(sentenceLength, maxLength, length_count);
							int rankContent = FindNumber(numContentWord, maxContentWord, content_count);
							int rankRelated = FindNumber(numRelatedWord, maxRelatedWord, related_count);
							int rankFrequency = frequencyRank.length-Arrays.binarySearch(frequencyRank,  new Integer(frequency))-1;
							
							int[] ranks = {rankLength, rankContent, rankRelated, rankFrequency};
							
							Arrays.sort(ranks);
							double rank = ranks[3]*Math.pow(size, 3)+ranks[2]*Math.pow(size, 2)+ranks[1]*Math.pow(size, 1)+ranks[0];
							sentence_score.put(sc.getSentence(i), rank);
						}
						Map.Entry[] entries = SortHashtableByValue.sort(sentence_score);
						
						for(int i = entries.length-1;i>=0;i--){
							bw.write(entries[i].getKey().toString().split("\t")[0]+"\t"+entries[i].getValue()+"\n");
							if(i<1){
								scwhole.addSentence((String)entries[i].getKey());
							}
						}
						bw.write("\n");
					}
				}
				bw.close();
				
				if(scwhole.num_sentence==0)
					return;
				ArrayList<Integer> sentence_length = new ArrayList<Integer>();
				ArrayList<Integer> sentence_frequency = new ArrayList<Integer>();
				ArrayList<Integer> num_contentword = new ArrayList<Integer>();
				ArrayList<Integer> num_relatedword = new ArrayList<Integer>();
				int maxLength = 0;
				int minLength = 100;
				int maxContentWord = 0;
				int minContentWord = 100;
				int maxRelatedWord = 0;
				int minRelatedWord = 100;
				
				Hashtable<Integer, Integer> length_count = new Hashtable<Integer, Integer>();
				//number of content words
				Hashtable<Integer, Integer> content_count = new Hashtable<Integer, Integer>();
				//number of related words
				Hashtable<Integer, Integer> related_count = new Hashtable<Integer, Integer>();
				
				for(int i = 0;i<scwhole.sentences.size();i++){
					float score = 0;
					int current_sentence_length = calSentenceLength(scwhole.getSentence(i));
					//content words, the more the better
					int current_num_contentword = calNumContentWord(scwhole.getSentence(i));
					//related words, the more the better
					int current_num_relatedword = calNumRelatedWord(scwhole.getSentence(i), target_word, relatedWords);
					//sentence frequency
					
					sentence_length.add(current_sentence_length);
					num_contentword.add(current_num_contentword);
					num_relatedword.add(current_num_relatedword);
					sentence_frequency.add(Integer.valueOf(scwhole.getSentence(i).split("\t")[1]));
					
					if(current_sentence_length<minLength)
						minLength = current_sentence_length;
					if(current_sentence_length>maxLength)
						maxLength = current_sentence_length;
					
					if(current_num_contentword>maxContentWord)
						maxContentWord = current_num_contentword;
					if(current_num_contentword<minContentWord)
						minContentWord = current_num_contentword;
					
					
					if(current_num_relatedword<minRelatedWord)
						minRelatedWord = current_num_relatedword;
					if(current_num_relatedword>maxRelatedWord)
						maxRelatedWord = current_num_relatedword;
					
					
					// add to hashtable
					if(length_count.containsKey(current_sentence_length)){
						length_count.put(current_sentence_length, length_count.get(current_sentence_length)+1);
					}else{
						length_count.put(current_sentence_length, 1);
					}
					
					if(content_count.containsKey(current_num_contentword)){
						content_count.put(current_num_contentword, content_count.get(current_num_contentword)+1);
					}else{
						content_count.put(current_num_contentword, 1);
					}
					
					if(related_count.containsKey(current_num_relatedword)){
						related_count.put(current_num_relatedword, related_count.get(current_num_relatedword)+1);
					}else{
						related_count.put(current_num_relatedword, 1);
					}
					
				}
				Integer[] frequencyRank = new Integer[sentence_frequency.size()];
				sentence_frequency.toArray(frequencyRank);

				Arrays.sort(frequencyRank);

				int size = frequencyRank.length;
				
				Hashtable<String, Double> sentence_score = new Hashtable<String, Double>();
				
				for(int i = 0;i<scwhole.num_sentence;i++){
					//sentence length, the longer the better
					float score = 0;
					int sentenceLength = sentence_length.get(i);
					//content words, the more the better
					int numContentWord = num_contentword.get(i);
					//related words, the more the better
					int numRelatedWord = num_relatedword.get(i);
					//sentence frequency
					int frequency =sentence_frequency.get(i);
			
					//Get the rank of each feature
					int rankLength = FindNumber(sentenceLength, maxLength, length_count);
					int rankContent = FindNumber(numContentWord, maxContentWord, content_count);
					int rankRelated = FindNumber(numRelatedWord, maxRelatedWord, related_count);
					int rankFrequency = frequencyRank.length-Arrays.binarySearch(frequencyRank,  new Integer(frequency))-1;
					
					int[] rankswhole = {rankLength, rankContent, rankRelated, rankFrequency};
					Arrays.sort(rankswhole);
					double rank = rankswhole[3]*Math.pow(size, 3)+rankswhole[2]*Math.pow(size, 2)+rankswhole[1]*Math.pow(size, 1)+rankswhole[0];
					
					sentence_score.put(scwhole.getSentence(i), rank);
					
				}
				Map.Entry[] entries = SortHashtableByValue.sort(sentence_score);
				
				for(int i = entries.length-1;i>=0;i--){
					fwhole.write(entries[i].getKey().toString().split("\t")[0]+"\t"+entries[i].getValue()+"\n");
				}
				fwhole.write("\n");
				fwhole.close();
				
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		
		
		
	/*
	 * The following six functions are used at the very beginning 
	 * when we try to generate context and don't know exactly what criteria should we use
	 * 
	 * */
	
	public void createRelatedWordList(String target_word, Database db, String filename){
		try{
			String query = "select related_word from liuliu.word_relation_google where target_word = '"+target_word+"' and freq_co_occurrence > 0 order by new_measure desc limit 100";
			ResultSet rs = db.execute(query);
			
			File f = new File(filename);
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			while(rs.next()){
				String related_word= rs.getString("related_word");
				bw.write(related_word+"\n");
			}
			bw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void readRelatedWordList(String target_word, HashSet<String> list, String filename){
		try{
			
			File f = new File(filename);
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			int i = 0;
			while((line = br.readLine())!=null){
				list.add(line);
				i++;
				if(i>=10)
					break;
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param target_word
	 * @param post_process_file
	 * @param word_level_file
	 * @param rw
	 * @param db
	 * @return contexts which satisfy the word level criteria
	 */
	public void restrictWordLevel(String target_word, String post_process_file, String word_level_file, RelatedWords rw, Database db){
		try{
			
			File f = new File(post_process_file);
			FileReader fr = new FileReader(f);
			BufferedReader br= new BufferedReader(fr);
			File fexample = new File(word_level_file);
			FileWriter fw = new FileWriter(fexample);
			BufferedWriter bw = new BufferedWriter(fw);
			String line = "";
			String toWrite = "";
			int i = 0;
			while ((line = br.readLine())!=null){
				//handle each cluster
				toWrite = "";
				if(line.contains("--------")){
					toWrite+="-----------"+i+"-----------"+"\n";
					line = br.readLine(); //this is the common words
					toWrite+=line+"\n";
					
					while((line = br.readLine()).length()>1){
						if(judgeSentencelevel(target_word, line, rw, db)){
						
							toWrite+=line.trim()+"\n";
						}
					}
					if(toWrite.split("\n").length>2){ //if there are sentences left in this cluster
						bw.write(toWrite);
						bw.write("\n");
						i++;
					}
				
				}
				
			}
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	
	public boolean judgeSentencelevel(String target_word, String line, RelatedWords rw , Database db){
		int num = 0;
		
		int num_above2 = 0;
		
		String[] words = line.split("\t")[0].toLowerCase().split(" ");
		for(int i = 0;i<words.length;i++){
			if((!words[i].equalsIgnoreCase(target_word))&&words[i].toLowerCase().matches("[a-z]+")){
				String base = rw.getMostFrequentBase(words[i], db);
				if(base==null)
					base = words[i];
				
				if(functionWords.contains(base)){
					continue;
				}
				int level = rw.judgeWordLevel(base, db);
				if(level>23 || (level > 2 && level < 20)){
					num_above2++;
				}
			}
		}
		if(num_above2<2)
			return true;
		else
			return false;
	}
	
	
	
	/**
	 * @param anc
	 * @param google
	 * @param specific
	 * @return web specific words according to Google unigram and ANC unigram
	 */
	public static void findWebSpecificWords(String anc, String google, String specific){
		try{
			File fanc = new File(anc);
			File fgoogle = new File(google);
			FileReader franc = new FileReader(fanc);
			FileReader frgoogle = new FileReader(fgoogle);
			BufferedReader branc = new BufferedReader(franc);
			BufferedReader brgoogle  = new BufferedReader(frgoogle);
			
			File fspecific = new File(specific);
			FileWriter fw = new FileWriter(fspecific);
			BufferedWriter bw = new BufferedWriter(fw);
			
			String line = "";
			//HashSet<String> hashanc = new HashSet<String>();
			Hashtable<String,Long> hashanc = new Hashtable<String, Long>();
			while((line = branc.readLine())!=null)
			{	
				hashanc.put(line.split("\t")[0], Long.valueOf(line.split("\t")[1]));
			}
			while((line = brgoogle.readLine())!=null){
				if(!hashanc.containsKey(line.split("\t")[0])){
					bw.write(line.split("\t")[0]+"\n");
				}else{					
					if(Long.valueOf(line.split("\t")[1])/hashanc.get(line.split("\t")[0])>2000000){
						bw.write(line.split("\t")[0]+"\n");
					}
				}
			}
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	
	
	/**
	 * @param filename
	 * @param newfilename
	 * Used for named entity recognition
	 */
	public void proprocess_NER(String filename, String newfilename){
		try{
			File fold = new File(filename);
			FileReader fr = new FileReader(fold);
			BufferedReader br = new BufferedReader(fr);
			File fnew = new File(newfilename);
			FileWriter fw = new FileWriter(fnew);
			BufferedWriter bw = new BufferedWriter(fw);
			String line = "";
			while((line = br.readLine())!=null){
				line = line.replaceAll("<S> ", "").replaceAll(" </S>", "");
				bw.write(line+" "+"999999"+"\n");
			}
			bw.close();
		}catch(IOException e){
			
		}
	}
	
	public void postprocess_NER(String filenamewithS, String filenameafterNER, String newfilename){
		try{
			File fwithS = new File(filenamewithS);
			FileReader frwithS = new FileReader(fwithS);
			BufferedReader brwithS = new BufferedReader(frwithS);
			
			File fNER = new File(filenameafterNER);
			FileReader frNER = new FileReader(fNER);
			BufferedReader brNER = new BufferedReader(frNER);
			
			File fnew = new File(newfilename);
			FileWriter fw = new FileWriter(fnew);
			BufferedWriter bw = new BufferedWriter(fw);
			String line = "";
			line = brNER.readLine();
			String[] sentences = line.split(" 999999/O ");
			for(int j = 0;j<sentences.length;j++){
				String line2 = brwithS.readLine();
				String[] words = sentences[j].split(" ");
				int flag = 0;
				for(int i = 1;i<words.length-1;i++){
					if((words[i].charAt(0)>64 &&words[i].charAt(0)<91 ))
					{
						if(words[i].split("/")[1].equalsIgnoreCase("O")){
							flag = 1;
							break;
						}
					}				
				}
				if(flag == 0){
					bw.write(line2+"\n");
				}
			}
			bw.close();
		}catch(IOException e){
			
		}
	}
	
	

	
	
	/**
	 * @param args
	 * 
	 */


}

