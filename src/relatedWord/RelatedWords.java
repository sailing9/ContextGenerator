package relatedWord;

import util.SortHashtableByValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import util.Database;
import util.TimeStat;

public class RelatedWords {
	private static HashSet<String> tier2Words; 
	private static HashSet<String> stopWords;
	private static HashSet<String> webbyWords;
	private static HashSet<String> functionWords;
    public HashSet<String> uniformWords;
    
    
    /**
	 * read the tier 2 word into the hashtable
	 */
	public void readUniformWords(String filename){
		uniformWords = new HashSet<String>();
		try{
			File tier2 = new File(filename);
			FileReader fr = new FileReader(tier2);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			
			while((line=br.readLine())!=null){
				uniformWords.add(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
    
	/**
	 * read the tier 2 word into the hashtable
	 */
	public static void readTier2(String filename){
		tier2Words = new HashSet<String>();
		try{
			File tier2 = new File(filename);
			FileReader fr = new FileReader(tier2);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			
			while((line=br.readLine())!=null){
				tier2Words.add(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * read the stop word into the hashtable
	 */
	public static void readStop(String filename){
		stopWords = new HashSet<String>();
		try{
			File tier2 = new File(filename);
			FileReader fr = new FileReader(tier2);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			
			while((line=br.readLine())!=null){
				stopWords.add(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * read the tier 2 word into the hashtable
	 */
	public static void readWebbyWords(String filename, Database db){
		webbyWords = new HashSet<String>();
		try{
			File tier2 = new File(filename);
			FileReader fr = new FileReader(tier2);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			
			while((line=br.readLine())!=null){
				String webby = getMostFrequentBase(line, db);
				if(webby==null)
					webby = line;
				
				webbyWords.add(webby);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * read the tier 2 word into the hashtable
	 */
	public static void readFunctionWords(String filename){
		functionWords = new HashSet<String>();
		try{
			File tier2 = new File(filename);
			FileReader fr = new FileReader(tier2);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			
			while((line=br.readLine())!=null){
				if(line.contains("(")){
					continue;
				}
				
				if(line.contains("_")){
					line = line.replaceAll("_", " ");
				}
				functionWords.add(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @param target_word: the name of the table
	 * @param db: database
	 * @return: the table with target word and its related words
	 */
	public void createTargetTable(String target_word, Database db){
		String query = 
		"CREATE TABLE `liuliu`.`"+target_word+"_relatedWords` ("+
				 " `id` int(11) NOT NULL auto_increment,"+
				 " `target_word` varchar(64) NOT NULL default '',"+
				 " `related_word_base` varchar(64) NOT NULL default '',"+
				 " `word_level` int(6) NOT NULL,"+
				 " `related_words` varchar(255) NOT NULL default '',"+
				 " `freq_target_word` bigint(20) NOT NULL default '0',"+
				 " `freq_related_word` bigint(20) NOT NULL default '0',"+
				 " `freq_co_occurrence` bigint(20) NOT NULL default '0',"+
				 " `mutual_information` float default NULL,"+
				 " `new_measure` float default NULL,"+
				 " PRIMARY KEY  (`target_word`,`related_word_base`),"+
				 " KEY `id` (`id`)"+
				") ENGINE=MyISAM DEFAULT CHARSET=latin1";
	db.executeUpdate(query);
	}
	
	
	
	/**
	 * @param target_word
	 * @param db
	 * @return add unique key (related_word_base)
	 */
	public void addUniqueIndex(String target_word, Database db){
		String query = 
			"alter table liuliu."+target_word+"_relatedWords add unique(related_word_base)";
		db.executeUpdate(query);
	}
	
	/**
	 * @param target_word
	 * @param db
	 * @return add two columns: webby, function_word
	 */
	public void addWebFunction(String target_word, Database db){
		String query = 
			"alter table liuliu."+target_word+"_relatedWords add webby tinyint default 0 not null,"+
			" add function_word tinyint default 0 not null";
		db.executeUpdate(query);
	}
	
	/**
	 * @param target_word
	 * @param db
	 * @return add two columns: webby, function_word
	 */
	public void fillinWebFunction(String target_word, Database db){
		try{
			
			String querycount = "select count(*) from liuliu."+target_word+"_relatedwords"; //merge table 2 to google
			ResultSet rscount = db.execute(querycount);
			int count = 0;
			if(rscount.next()){
				count = rscount.getInt("count(*)");
				System.out.println(count);
				for(int i = 1;i<=count;i++){
					String query = "select related_word_base from liuliu."+target_word+"_relatedwords where id = " + i;
					ResultSet rs = db.execute(query);			
						if(rs.next()){
							int webby = 0;
							int function = 0;
							String related_word_base = rs.getString("related_word_base");
							if(webbyWords.contains(related_word_base)){
								webby = 1;
							}
							if(functionWords.contains(related_word_base)){
								function = 1;
							}
							if(webby==1||function==1){
								String queryupdate = "update liuliu."+target_word+"_relatedwords set webby = "+webby+", function_word = "+function+" where id = "+i;
								
								int row = db.executeUpdate(queryupdate);
							}
								
						}
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @param target_word: the name of the table
	 * @param db: database
	 * @return: the table with target word and its related words
	 */
	public void addColumetoTargetTable(String target_word, Database db){
		String query = 
		"alter TABLE `liuliu`.`"+target_word+"_relatedWords` add "+
				 " `freq_left_4` int(10) NOT NULL default '0',"+
				 " add `freq_left_3` int(10) NOT NULL default '0',"+
				 " add `freq_left_2` int(10) NOT NULL default '0',"+
				 " add `freq_left_1` int(10) NOT NULL default '0',"+
				 " add `freq_right_1` int(10) NOT NULL default '0',"+
				 " add `freq_right_2` int(10) NOT NULL default '0',"+
				 " add `freq_right_3` int(10) NOT NULL default '0',"+
				 " add `freq_right_4` int(10) NOT NULL default '0',"+
				 " add `freq_all_displacement` bigint(20) NOT NULL default '0'";
	db.executeUpdate(query);
	}
	
	
	
	/*
	 * Calcualte the new measure: P(x,y)-P(x)P(y)
	 * 
	 * */
	public static void calculateJackMeasure(Database db, String targetword){
		//sample
		try{
			String querycount = "select count(*) from liuliu."+targetword+"_relatedwords"; //merge table 2 to google
			ResultSet rscount = db.execute(querycount);
			int count = 0;
			long total = 1024908267229L;
			if(rscount.next()){
				count = rscount.getInt("count(*)");
				System.out.println(count);
				for(int i = 1;i<=count;i++){
					String query = "select freq_target_word, freq_related_word,freq_co_occurrence from liuliu.word_relation_google where id = " + i;
					
					ResultSet rs = db.execute(query);
					
						if(rs.next()){
							Long fx = rs.getLong("freq_target_word");
							Long fy = rs.getLong("freq_related_word");
							Long fxy = rs.getLong("freq_co_occurrence");
							
							if(fxy == 0)
								continue;  //ignore these rows currently
							double measure = 0.0;
							double temp = 0.0;
							
							measure = ((double)fxy)-(double)fx*(double)fy*6/(double)total;
							String queryupdate;
							
								queryupdate = "update liuliu.word_relation_google set new_measure = "+measure+" where id = "+i;
							
							int row = db.executeUpdate(queryupdate);
								
						}
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		
	}

	
	public void fillinFrequency(Database db, String target_word){
		try{
			String query_target = "select frequency from liuliu.word_frequency_google where word = '"+target_word+"'";
			ResultSet rs_target = db.execute(query_target);
			long freq_target_word = -1;
			if(rs_target.next()){
				freq_target_word = rs_target.getLong("frequency");
			}else{
				System.out.println("!!!!!!!!!!!!!!!!!"+target_word);
			}
			String querycount = "select count(*) from liuliu."+target_word+"_relatedwords"; //merge table 2 to google
			ResultSet rscount = db.execute(querycount);
			int count = 0;
			long total = 1024908267229L;
			long fx = freq_target_word;
			if(rscount.next()){
				count = rscount.getInt("count(*)");
				System.out.println(count);
				for(int i = 1;i<=count;i++){
					String query = "select related_words, freq_co_occurrence from liuliu."+target_word+"_relatedwords where id = " + i;
					ResultSet rs = db.execute(query);			
						if(rs.next()){
							Long fxy = rs.getLong("freq_co_occurrence");
							if(fxy == 0)
								continue;  //ignore these rows currently
							
							String related_words = rs.getString("related_words");
							String[] words = related_words.split(",");
							Long fy = 0l;
							for(int j = 0;j<words.length;j++){
								String query_related = "select frequency from liuliu.word_frequency_google where word = '"+words[j]+"'";
								ResultSet rs_related = db.execute(query_related);
								if(rs_related.next()){
									fy += rs_related.getLong("frequency");
								}
							}
							double measure = 0.0;
							double temp = 0.0;
							
							measure = ((double)fxy)-(double)fx*(double)fy*6/(double)total;
							String queryupdate;
							
							queryupdate = "update liuliu."+target_word+"_relatedwords set freq_target_word = "+fx+", freq_related_word = "+fy+", new_measure = "+measure+" where id = "+i;
							
							int row = db.executeUpdate(queryupdate);
								
						}
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @param word
	 * @param db
	 * @return if the word is in the table, return the most frequently used baseform of the word
	 * 		   otherwise, return null
	 */
	public static String getMostFrequentBase(String word, Database db){
		word = word.replaceAll("'", "\\\\'");
		String query = "select baseform from liuliu.word_base_pos where word like '"+word+"' order by frequency desc";
		String base = null;
		try{
			ResultSet rs = db.execute(query);
			if(rs.next()){ //only get the first baseform, which is the most frequent one
				base = rs.getString("baseform");
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return base;
	}
	
	
	/**
	 * @param word
	 * @param db
	 * @return the word level of the word
	 * 		   andrew_level: if the word is on andrew words list
	 * 		   20+grade: if the word is on appendix list but not andrew's list
	 * 		   100: if the word is not on the two lists
	 */
	public static int judgeWordLevel(String word, Database db){
		word = word.replaceAll("'","\\\\'");
		
		//get the base form of this word
		
		String query = "select grade_level, andrew_level from liuliu.word_level_appendix where word = '"+word+"'";
		int level = 100; //assume the word is not in the two lists
		try{
			ResultSet rs = db.execute(query);
			if(rs.next()){
				String level_string = rs.getString("andrew_level");
				if(level_string!=null){
					level = Integer.valueOf(level_string);
				}else{
					level_string = rs.getString("grade_level");
					level = Integer.valueOf(level_string);
					level = 20+level;
				}		
			}
			
		}catch(SQLException e){
			e.printStackTrace();
		}
		return level;
	}
	
	public void updatewordlevel(String target_word, Database db){
		
		String querycount = "select count(*) from liuliu."+target_word+"_relatedwords"; //merge table 2 to google
		ResultSet rscount = db.execute(querycount);
		int count = 0;
	try{
		if(rscount.next()){
			count = rscount.getInt("count(*)");
			System.out.println(count);
			for(int i = 1;i<=count;i++){
				String query = "select related_word_base from liuliu."+target_word+"_relatedwords where id = " + i;
				ResultSet rs = db.execute(query);			
					if(rs.next()){
						
						String related_word_base = rs.getString("related_word_base");
						int word_level = judgeWordLevel(related_word_base, db);
						String queryupdate;
						
						queryupdate = "update liuliu."+target_word+"_relatedwords set word_level = "+word_level+" where id = "+i;
						
						int row = db.executeUpdate(queryupdate);
							
					}
			}
		}
	}catch(SQLException e){
		e.printStackTrace();
	}
	}
	
	
	/**
	 * count the co-occurrence frequency for target word and its related words
	 * using the tricky way discussed with Jack, which is
	 * for a five gram (1, 2, 3, 4, 5), we only consider the following pairs(1, 2), (1, 3), (1, 4), (1, 5).
	 * and (1, 5), (2, 5), (3, 5), (4, 5)
	 * Because for pairs in other positions, we will consider it after the 5 gram window move ahead
	 * If the first word is a target word, then we will consider 2, 3, 4, 5 as its related words
	 * If the fifth word is a target word, then we will consider 1, 2, 3, 4 as its related words
	 */
	
	/*
	 * This time we need to consider the morphology of the related words
	 * */
	public static void readCooccurrenceGoogle(String target_word, Database db){
		String line = "";
		try{
			String filename = "D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\5gramExamples\\"+target_word+".txt";
			File googlefile = new File(filename);
			FileReader fr = new FileReader(googlefile);
			BufferedReader br = new BufferedReader(fr);
			
			while((line = br.readLine())!=null){
				String word_count[] = line.split("\t");
				long count = new Long(word_count[1]).longValue();
				String words[] = word_count[0].split(" ");
				
				//if the first word is a target word
				if(words[0].toLowerCase().equalsIgnoreCase(target_word)){
					for(int i = 1;i<5;i++){
						if(stopWords.contains(words[i].toLowerCase())) continue;
						if((words[i].toLowerCase()).matches("[a-z]+"))	{
							int row = 0;
							String related_word = words[i].toLowerCase();
							String related_word_base = getMostFrequentBase(related_word, db);
							if(related_word_base==null)
								related_word_base = related_word;
							int word_level = judgeWordLevel(related_word_base, db);
							String query = "select freq_co_occurrence, related_words from liuliu."+target_word+"_relatedwords where target_word = '"+target_word+"' and related_word_base = '"+related_word_base+"'";
							ResultSet rs = db.execute(query);
							if(rs.next()){
								long occurrencetime = count + rs.getLong("freq_co_occurrence");	
								String related_words = rs.getString("related_words");
								String[] existed_words = related_words.split(",");
								int j = 0;
								HashSet<String> h = new HashSet<String>();
								for(j = 0;j<existed_words.length;j++){
									h.add(existed_words[j]);
								}
								if(!h.contains(related_word)){//the current related word does not in the related words list
									related_words +=","+related_word;
								}
								query = "update liuliu."+target_word+"_relatedwords set freq_co_occurrence = '"+occurrencetime+"', related_words = '"+related_words+"' where target_word = '"+target_word+"' and related_word_base = '"+related_word_base+"'";
								row = db.executeUpdate(query);
							}else{
								query = "insert into liuliu."+target_word+"_relatedwords (target_word, related_word_base, word_level, related_words, freq_co_occurrence) values ('"+target_word+"', '"+related_word_base+"', "+word_level+", '"+related_word+"',"+count+")";
								row = db.executeUpdate(query);
							}	
						}
					}
				}
				
				//if the fifth word is a target word
				if(words[4].toLowerCase().equalsIgnoreCase(target_word)){
					for(int i = 0;i<4;i++){
						if(stopWords.contains(words[i].toLowerCase())) continue;
						if(words[i].toLowerCase().matches("[a-z]+")){
							int row = 0;
							String related_word = words[i].toLowerCase();
							String related_word_base = getMostFrequentBase(related_word, db);
							if(related_word_base==null)
								related_word_base = related_word;
							String query = "select freq_co_occurrence, related_words from liuliu."+target_word+"_relatedwords where target_word = '"+target_word+"' and related_word_base = '"+related_word_base+"'";
							ResultSet rs = db.execute(query);
								if(rs.next()){
									long occurrencetime = count + rs.getLong("freq_co_occurrence");	
									String related_words = rs.getString("related_words");
									String[] existed_words = related_words.split(",");
									int j = 0;
									HashSet<String> h = new HashSet<String>();
									for(j = 0;j<existed_words.length;j++){
										h.add(existed_words[j]);
									}
									if(!h.contains(related_word)){//the current related word does not in the related words list
										related_words +=","+related_word;
									}
									query = "update liuliu."+target_word+"_relatedwords set freq_co_occurrence = '"+occurrencetime+"', related_words = '"+related_words+"' where target_word = '"+target_word+"' and related_word_base = '"+related_word_base+"'";
									row = db.executeUpdate(query);
								}else{
									query = "insert into liuliu."+target_word+"_relatedwords (target_word, related_word_base, related_words, freq_co_occurrence) values ('"+target_word+"', '"+related_word_base+"', '"+related_word+"',"+count+")";
									row = db.executeUpdate(query);
								}	
							
						}
						
					}
					
					
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println(line);
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println(line);
		}
	}
	
	/**
	 * count the co-occurrence frequency for target word and its related words
	 * using the tricky way discussed with Jack, which is
	 * for a five gram (1, 2, 3, 4, 5), we only consider the following pairs(1, 2), (1, 3), (1, 4), (1, 5).
	 * and (1, 5), (2, 5), (3, 5), (4, 5)
	 * Because for pairs in other positions, we will consider it after the 5 gram window move ahead
	 * If the first word is a target word, then we will consider 2, 3, 4, 5 as its related words
	 * If the fifth word is a target word, then we will consider 1, 2, 3, 4 as its related words
	 */
	
	/*
	 * This time we need to consider the morphology of the related words
	 * We compute the co-occurrence frequency at different displacement from target word
	 * */
	public static void readCooccurrenceGoogle_withDisplacement(String target_word, Database db){
		String line = "";
		try{
			String filename = "D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\5gramExamples\\"+target_word+".txt";
			File googlefile = new File(filename);
			FileReader fr = new FileReader(googlefile);
			BufferedReader br = new BufferedReader(fr);
			
			while((line = br.readLine())!=null){
				String word_count[] = line.split("\t");
				long count = new Long(word_count[1]).longValue();
				String words[] = word_count[0].split(" ");
				
				//if the first word is a target word
				if(words[0].toLowerCase().equalsIgnoreCase(target_word)){
					for(int i = 1;i<5;i++){
						if(stopWords.contains(words[i].toLowerCase())) continue;
						if((words[i].toLowerCase()).matches("[a-z]+"))	{
							int row = 0;
									String related_word = words[i].toLowerCase();
									String related_word_base = getMostFrequentBase(related_word, db);
									if(related_word_base==null)
										related_word_base = related_word;
									String query = "select freq_right_"+i+" from liuliu."+target_word+"_relatedwords where related_word_base = '"+related_word_base+"'";
									ResultSet rs = db.execute(query);
									if(rs.next()){
										long occurrencetime = count + rs.getLong("freq_right_"+(i));	
										query = "update liuliu."+target_word+"_relatedwords set freq_right_"+i+"= '"+occurrencetime+"' where related_word_base = '"+related_word_base+"'";
										row = db.executeUpdate(query);
									}else{
										System.out.println(related_word_base);
									}	
								}
							}
				}
				
				//if the fifth word is a target word
				if(words[4].toLowerCase().equalsIgnoreCase(target_word)){
					for(int i = 0;i<4;i++){
						if(stopWords.contains(words[i].toLowerCase())) continue;
						if(words[i].toLowerCase().matches("[a-z]+")){
							int row = 0;
							String related_word = words[i].toLowerCase();
							String related_word_base = getMostFrequentBase(related_word, db);
							if(related_word_base==null)
								related_word_base = related_word;
							String query = "select freq_left_"+(4-i)+" from liuliu."+target_word+"_relatedwords where related_word_base = '"+related_word_base+"'";
							ResultSet rs = db.execute(query);
							if(rs.next()){
								long occurrencetime = count + rs.getLong("freq_left_"+(4-i));	
								query = "update liuliu."+target_word+"_relatedwords set freq_left_"+(4-i)+"= '"+occurrencetime+"' where related_word_base = '"+related_word_base+"'";
								row = db.executeUpdate(query);
							}else{
								System.out.println(related_word_base);
							}	
						}
						
					}
					
					
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println(line);
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println(line);
		}
	}
	
	
	/**
	 * count the co-occurrence frequency for target word and its related words
	 * using the tricky way discussed with Jack, which is
	 * for a five gram (1, 2, 3, 4, 5), we only consider the following pairs(1, 2), (1, 3), (1, 4), (1, 5).
	 * and (1, 5), (2, 5), (3, 5), (4, 5)
	 * Because for pairs in other positions, we will consider it after the 5 gram window move ahead
	 * If the first word is a target word, then we will consider 2, 3, 4, 5 as its related words
	 * If the fifth word is a target word, then we will consider 1, 2, 3, 4 as its related words
	 */
	
	/*
	 * This time we need to consider the morphology of the related words
	 * We compute the co-occurrence frequency at different displacement from target word
	 * Read the data from database
	 * */
	public static void readCooccurrenceGoogle_withDisplacement_database(String target_word, Database db){
		String line = "";
		try{
			String query1 = "select gram2, gram3, gram4, gram5, frequency from liuliu."+target_word+"_5grams where gram1 COLLATE latin1_swedish_ci = '"+target_word+"'";
			
			ResultSet rs1 = db.execute(query1);
			
			while(rs1.next()){
				
				String words[] = new String[4];
				words[0] = rs1.getString("gram2");
				words[1] = rs1.getString("gram3");
				words[2] = rs1.getString("gram4");
				words[3] = rs1.getString("gram5");
				long count = rs1.getLong("frequency");
				
					for(int i = 0;i<4;i++){
						if(stopWords.contains(words[i].toLowerCase())) continue;
						if((words[i].toLowerCase()).matches("[a-z]+"))	{
							int row = 0;
							String related_word = words[i].toLowerCase();
							String related_word_base = getMostFrequentBase(related_word, db);
							if(related_word_base==null)
								related_word_base = related_word;
							String query = "select freq_right_"+(i+1)+" from liuliu."+target_word+"_relatedwords where related_word_base = '"+related_word_base+"'";
							ResultSet rs = db.execute(query);
							if(rs.next()){
								long occurrencetime = count + rs.getLong("freq_right_"+(i+1));	
								query = "update liuliu."+target_word+"_relatedwords set freq_right_"+(i+1)+"= '"+occurrencetime+"' where related_word_base = '"+related_word_base+"'";
								row = db.executeUpdate(query);
							}else{
								System.out.println(related_word_base);
							}	
						}
					}
				}
				
				//if the fifth word is a target word
			String query2 = "select gram1, gram2, gram3, gram4, frequency from liuliu."+target_word+"_5grams where gram5 COLLATE latin1_swedish_ci = '"+target_word+"'";
			
			ResultSet rs2 = db.execute(query2);
			
			while(rs2.next()){
				
				String words[] = new String[4];
				words[0] = rs2.getString("gram4");
				words[1] = rs2.getString("gram3");
				words[2] = rs2.getString("gram2");
				words[3] = rs2.getString("gram1");
				long count = rs2.getLong("frequency");
				
					for(int i = 0;i<4;i++){
						if(stopWords.contains(words[i].toLowerCase())) continue;
						if((words[i].toLowerCase()).matches("[a-z]+"))	{
							int row = 0;
							String related_word = words[i].toLowerCase();
							String related_word_base = getMostFrequentBase(related_word, db);
							if(related_word_base==null)
								related_word_base = related_word;
							String query = "select freq_left_"+(i+1)+" from liuliu."+target_word+"_relatedwords where related_word_base = '"+related_word_base+"'";
							ResultSet rs = db.execute(query);
							if(rs.next()){
								long occurrencetime = count + rs.getLong("freq_left_"+(i+1));	
								query = "update liuliu."+target_word+"_relatedwords set freq_left_"+(i+1)+"= '"+occurrencetime+"' where related_word_base = '"+related_word_base+"'";
								row = db.executeUpdate(query);
							}else{
								System.out.println(related_word_base);
							}	
						}
					}
				}
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println(line);
		}
	}
	
	public void addAlphabetOrder(String target_word, Database db){
		String query = 
			"alter table liuliu."+target_word+"_5grams add alphabetOrder tinyint default 0 not null";
		db.executeUpdate(query);
	}
	
	public void addinAlphabetOrderList(String target_word, Database db){
		String query2 = 
			"alter table liuliu."+target_word+"_relatedwords add in_alphabetOrder_list tinyint default 0 not null";		
		db.executeUpdate(query2);
	}
	
	public void findAlphabetWords(String target_word, Database db){
		String line = "";
		try{
			String filename = "D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\5gramExamples\\"+target_word+".txt";
			File googlefile = new File(filename);
			FileReader fr = new FileReader(googlefile);
			BufferedReader br = new BufferedReader(fr);
			
			while((line = br.readLine())!=null){
				String word_count[] = line.split("\t");
				long count = new Long(word_count[1]).longValue();
				String words[] = word_count[0].split(" ");
				
				//if the first word is a target word
				if(words[0].toLowerCase().equalsIgnoreCase(target_word)){
					String gram1 = words[0];
					String gram2 = words[1];
					String gram3 = words[2];
					String gram4 = words[3];
					String gram5 = words[4];
					if(gram1.length()==0 ||gram2.length()==0 ||gram3.length()==0||gram4.length()==0 || gram5.length()==0){
				    	continue;
				    } 
					
					if(gram1.charAt(0)==gram2.charAt(0)
							&&gram1.charAt(0)==gram3.charAt(0)
							&&gram1.charAt(0)==gram4.charAt(0)
							&&gram1.charAt(0)==gram5.charAt(0)
							&&gram1.compareToIgnoreCase(gram2)<0
							&&gram2.compareToIgnoreCase(gram3)<0
							&&gram3.compareToIgnoreCase(gram4)<0
							&&gram4.compareToIgnoreCase(gram5)<0){
						String queryupdate_5grams = "update liuliu."+target_word+"_5grams set alphabetOrder = 1 where five_gram = '"+word_count[0].replaceAll("'", "\\\\'")+"'";
						int row = db.executeUpdate(queryupdate_5grams);
						
						String[] grambase = new String[5];
						String temp = getMostFrequentBase(gram1, db);
						grambase[0] = (temp!=null? temp:gram1);
						temp = getMostFrequentBase(gram2, db);
						grambase[1] = (temp!=null? temp:gram2);
						temp = getMostFrequentBase(gram3, db);
						grambase[2] = (temp!=null? temp:gram3);
						temp = getMostFrequentBase(gram4, db);
						grambase[3] = (temp!=null? temp:gram4);
						temp = getMostFrequentBase(gram5, db);
						grambase[4] = (temp!=null? temp:gram5);
						for(int j = 0;j<5;j++){
							String query_relatedwords = "update liuliu."+target_word+"_relatedwords set in_alphabetOrder_list = 1 where related_word_base = '"+grambase[j].replaceAll("'", "\\\\'")+"'";
							db.executeUpdate(query_relatedwords);
						}
					}	
				}
				
				//if the fifth word is a target word
				if(words[4].toLowerCase().equalsIgnoreCase(target_word)){
					String gram1 = words[0];
					String gram2 = words[1];
					String gram3 = words[2];
					String gram4 = words[3];
					String gram5 = words[4];
					if(gram1.length()==0 ||gram2.length()==0 ||gram3.length()==0||gram4.length()==0 || gram5.length()==0){
				    	continue;
				    } 
					
					if(gram1.charAt(0)==gram2.charAt(0)
							&&gram1.charAt(0)==gram3.charAt(0)
							&&gram1.charAt(0)==gram4.charAt(0)
							&&gram1.charAt(0)==gram5.charAt(0)
							&&gram1.compareToIgnoreCase(gram2)<0
							&&gram2.compareToIgnoreCase(gram3)<0
							&&gram3.compareToIgnoreCase(gram4)<0
							&&gram4.compareToIgnoreCase(gram5)<0){
						String queryupdate_5grams = "update liuliu."+target_word+"_5grams set alphabetOrder = 1 where five_gram = '"+word_count[0].replaceAll("'", "\\\\'")+"'";
						int row = db.executeUpdate(queryupdate_5grams);
						
						String[] grambase = new String[5];
						String temp = getMostFrequentBase(gram1, db);
						grambase[0] = (temp!=null? temp:gram1);
						temp = getMostFrequentBase(gram2, db);
						grambase[1] = (temp!=null? temp:gram2);
						temp = getMostFrequentBase(gram3, db);
						grambase[2] = (temp!=null? temp:gram3);
						temp = getMostFrequentBase(gram4, db);
						grambase[3] = (temp!=null? temp:gram4);
						temp = getMostFrequentBase(gram5, db);
						grambase[4] = (temp!=null? temp:gram5);
						for(int j = 0;j<5;j++){
							String query_relatedwords = "update liuliu."+target_word+"_relatedwords set in_alphabetOrder_list = 1 where related_word_base = '"+grambase[j].replaceAll("'", "\\\\'")+"'";
							db.executeUpdate(query_relatedwords);
						}
					}		
					
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println(line);
		}
	}
	
	/**
	 * @param target_word
	 * @param db
	 * @return: give a flag to related word: whether this word is in an alphabetic ordered list with target word
	 */
	public void findAlphabetWords_database(String target_word, Database db){
		try{
			String query1 = "select id,  gram1, gram2, gram3, gram4, gram5 from liuliu."+target_word+"_5grams where gram1 COLLATE latin1_swedish_ci = '"+target_word+"'";
			
			ResultSet rs1 = db.execute(query1);
			
			while(rs1.next()){
					String gram1 = rs1.getString("gram1").toLowerCase();
					String gram2 = rs1.getString("gram2").toLowerCase();
					String gram3 = rs1.getString("gram3").toLowerCase();
					String gram4 = rs1.getString("gram4").toLowerCase();
					String gram5 = rs1.getString("gram5").toLowerCase();
				    int id = rs1.getInt("id");
				    if(gram1.length()==0 ||gram2.length()==0 ||gram3.length()==0||gram4.length()==0 || gram5.length()==0){
				    	continue;
				    }   
					if(gram1.charAt(0)==gram2.charAt(0)
									&&gram1.charAt(0)==gram3.charAt(0)
									&&gram1.charAt(0)==gram4.charAt(0)
									&&gram1.charAt(0)==gram5.charAt(0)
									&&gram1.compareToIgnoreCase(gram2)<0
									&&gram2.compareToIgnoreCase(gram3)<0
									&&gram3.compareToIgnoreCase(gram4)<0
									&&gram4.compareToIgnoreCase(gram5)<0){
						String queryupdate_5grams = "update liuliu."+target_word+"_5grams set alphabetOrder = 1 where id = "+id;;
						int row = db.executeUpdate(queryupdate_5grams);
								
								String[] grambase = new String[5];
								String temp = getMostFrequentBase(gram1, db);
								grambase[0] = (temp!=null? temp:gram1);
								temp = getMostFrequentBase(gram2, db);
								grambase[1] = (temp!=null? temp:gram2);
								temp = getMostFrequentBase(gram3, db);
								grambase[2] = (temp!=null? temp:gram3);
								temp = getMostFrequentBase(gram4, db);
								grambase[3] = (temp!=null? temp:gram4);
								temp = getMostFrequentBase(gram5, db);
								grambase[4] = (temp!=null? temp:gram5);
								for(int j = 0;j<5;j++){
									if(grambase[j].equalsIgnoreCase("null")){
										System.out.println("hello");
									}
									String query_relatedwords = "update liuliu."+target_word+"_relatedwords set in_alphabetOrder_list = 1 where related_word_base = '"+grambase[j].replaceAll("'", "\\\\'")+"'";
									db.executeUpdate(query_relatedwords);
								}
							}	
			}
		
			String query2 = "select id,  gram1, gram2, gram3, gram4, gram5 from liuliu."+target_word+"_5grams where gram5 COLLATE latin1_swedish_ci = '"+target_word+"'";
			
			ResultSet rs2 = db.execute(query2);
			
			while(rs2.next()){
					String gram1 = rs2.getString("gram1").toLowerCase();
					String gram2 = rs2.getString("gram2").toLowerCase();
					String gram3 = rs2.getString("gram3").toLowerCase();
					String gram4 = rs2.getString("gram4").toLowerCase();
					String gram5 = rs2.getString("gram5").toLowerCase();
					int id = rs2.getInt("id");
					if(gram1.length()==0 ||gram2.length()==0 ||gram3.length()==0||gram4.length()==0 || gram5.length()==0){
				    	continue;
				    }   
					
					if(gram1.charAt(0)==gram2.charAt(0)
									&&gram1.charAt(0)==gram3.charAt(0)
									&&gram1.charAt(0)==gram4.charAt(0)
									&&gram1.charAt(0)==gram5.charAt(0)
									&&gram1.compareToIgnoreCase(gram2)<0
									&&gram2.compareToIgnoreCase(gram3)<0
									&&gram3.compareToIgnoreCase(gram4)<0
									&&gram4.compareToIgnoreCase(gram5)<0){
						String queryupdate_5grams = "update liuliu."+target_word+"_5grams set alphabetOrder = 1 where id = "+id;;
						int row = db.executeUpdate(queryupdate_5grams);
								
								String[] grambase = new String[5];
								String temp = getMostFrequentBase(gram1, db);
								grambase[0] = (temp!=null? temp:gram1);
								temp = getMostFrequentBase(gram2, db);
								grambase[1] = (temp!=null? temp:gram2);
								temp = getMostFrequentBase(gram3, db);
								grambase[2] = (temp!=null? temp:gram3);
								temp = getMostFrequentBase(gram4, db);
								grambase[3] = (temp!=null? temp:gram4);
								temp = getMostFrequentBase(gram5, db);
								grambase[4] = (temp!=null? temp:gram5);
								for(int j = 0;j<5;j++){
									String query_relatedwords = "update liuliu."+target_word+"_relatedwords set in_alphabetOrder_list = 1 where related_word_base = '"+grambase[j].replaceAll("'", "\\\\'")+"'";
									db.executeUpdate(query_relatedwords);
								}
							}	
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void findUniformWords(Database db, ArrayList<String> keywords_list){
		Hashtable<String, Integer> words_count = new Hashtable<String, Integer>();
		try{
			for(int i = 0;i<keywords_list.size();i++){
				String target_word = keywords_list.get(i);
				String query = "select related_word_base from liuliu."+target_word+"_relatedwords where webby = 0 and function_word = 0 and in_alphabetOrder_list = 0 order by new_measure limit 100";
				ResultSet rs = db.execute(query);
				while(rs.next()){
					String relatedword = rs.getString("related_word_base");
					if(words_count.containsKey(relatedword)){
						int count = words_count.get(relatedword)+1;
						words_count.put(relatedword, count);
					}else{
						words_count.put(relatedword, 1);
					}
				}
				
				
			}
			Map.Entry[] entries = SortHashtableByValue.sort(words_count);
			for(int i = 0;i<words_count.size();i++){
				if(i<200)
					//System.out.println(entries[i].getKey());
				System.out.println(i+"\t"+ entries[i].getKey()+"\t"+entries[i].getValue());
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	
	
	
	/**
	 * @param target_word
	 * @param db
	 * @param filename: the file contains the top related words of the target words
	 */
	public void create100RelatedWordList(String target_word, Database db, String filename){
		try{
			String query = "select related_word_base from liuliu."+target_word
			+"_relatedwords where webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0 and (word_level<=2 or (word_level>=20 and word_level<=23)) "+
			"order by new_measure desc limit 100";
			ResultSet rs = db.execute(query);
			
			File f = new File(filename);
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			while(rs.next()){
				String related_word= rs.getString("related_word_base");
				bw.write(related_word+"\n");
			}
			bw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void createTop10relatedwords(String target_word, Database db, String filename){
		try{
			String query = "select related_word_base from liuliu."+target_word
			+"_relatedwords where webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0 and (word_level<=2 or (word_level>=20 and word_level<=23)) "+
			"order by new_measure desc limit 10";
			ResultSet rs = db.execute(query);
			
			FileWriter fwhole = new FileWriter(filename, true);
			fwhole.write(target_word+"\n");
			while(rs.next()){
				String related_word= rs.getString("related_word_base");
				fwhole.write(related_word+"\t");
			}
			fwhole.write("\n\n");
			fwhole.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RelatedWords rw = new RelatedWords();
		Database db = new Database();
		rw.readStop("D:\\LiuLiu\\Listen\\Research Resource\\Stopwords\\stopwords.txt");
		rw.readWebbyWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\webSpecific\\webspecific.txt", db);
		rw.readFunctionWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\functionWords.txt");
		
		String relatedwords_folder = "D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\related_words_list\\";
		TimeStat ts = new TimeStat();
		ts.start();
		
		
//		for(int i = 0;i<gt.keywords_list.size();i++){
//			
//			String target_word = gt.keywords_list.get(i);
			//String target_word = "anxious";
			//String target_word = "abandon";
//			System.out.println(i+" "+ target_word);
//			rw.updatewordlevel(target_word, db);
			
//			//rw.createTargetTable(target_word, db);
//			rw.addUniqueIndex(target_word, db);
//			rw.addWebFunction(target_word, db);
//			rw.fillinWebFunction(target_word, db);
//			rw.addAlphabetOrder(target_word, db);
//			rw.findAlphabetWords_database(target_word, db);
//			rw.findAlphabetWords(target_word, db);
			
//			//rw.readCooccurrenceGoogle(target_word, db);
//		    rw.fillinFrequency(db, target_word);
			
//			rw.addColumetoTargetTable(target_word, db);
			//rw.readCooccurrenceGoogle_withDisplacement_database(target_word, db);
//			rw.readCooccurrenceGoogle_withDisplacement(target_word, db);
			
			//rw.create100RelatedWordList(target_word, db, relatedwords_folder+target_word+".txt");
//			rw.createTop10relatedwords(target_word, db, "D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\RELATED_WORDS\\Top10\\top10forall.txt");
//			System.out.println(ts.getElapsedTimeSeconds());
//			ts.start();
//		}
				
	
	}

}
