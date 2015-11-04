package prepareDatabase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import util.Database;
import util.TimeStat;

public class ForTrigrams {

	HashSet<String> keywords; //Tier 2 target words
	Hashtable<String, String> keywords_trigrams;
	String dir;
	public ForTrigrams(String filename){
		keywords = new HashSet<String>();
		keywords_trigrams = new Hashtable<String, String>();
		dir = ".//result//trigram//";
		initialKeys(filename);
	}
	
	private void initialKeys(String filename){
		File f = new File(filename);
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				keywords.add(line.toLowerCase());
				keywords_trigrams.put(line, "");
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param db: database
	 * @param target_word
	 * @this function is used to create the 5grams table for target words and load data into the tables
	 */
	public void create3gramTable(Database db, String target_word){
		String query_create = "CREATE TABLE `liuliu`.`"+target_word+"_3grams` ("+
						  "`id` int(11) NOT NULL auto_increment,"+
						  "`trigram` varchar(128) binary NOT NULL default '',"+
						  "`frequency` bigint(20) NOT NULL default '0',"+
						  "`gram1` varchar(64) binary NOT NULL default '',"+
						  "`gram2` varchar(64) binary NOT NULL default '',"+
						  "`gram3` varchar(64) binary NOT NULL default '',"+
						  "`level_gram1` tinyint default '0',"+
						  "`level_gram2` tinyint default '0',"+
						  "`level_gram3` tinyint default '0',"+
						  "`freq_gram1` bigint(20) default '0',"+
						  "`freq_gram2` bigint(20) default '0',"+
						  "`freq_gram3` bigint(20) default '0',"+
						  "PRIMARY KEY  (`trigram`),"+
						  "KEY `id` (`id`)"+
						") ENGINE=MyISAM DEFAULT CHARSET=latin1";
		String query_load = "LOAD DATA LOCAL INFILE 'D:/LiuLiu/Listen/Research Resource/Google5gram/3gramExamples/"+target_word+".txt' "+
							"INTO TABLE liuliu."+target_word+"_3grams FIELDS TERMINATED BY '\t' (trigram, frequency)";
		
		db.executeUpdate(query_create);
		db.executeUpdate(query_load);
	}
	
	public void createAll3gramTable(Database db){
		String query_create = "CREATE TABLE `liuliu.word_trigrams_google` ("+
						  "`target_word` varchar(32) NOT NULL default '',"+
						  "`trigram` varchar(128) binary NOT NULL default '',"+
						  "`frequency` bigint(20) NOT NULL default '0',"+
						  "`gram1` varchar(64) binary NOT NULL default '',"+
						  "`gram2` varchar(64) binary NOT NULL default '',"+
						  "`gram3` varchar(64) binary NOT NULL default '',"+
						  "KEY `target_word` (`target_word`),"+
						  "KEY `trigram` (`trigram`)"+
						") ENGINE=MyISAM DEFAULT CHARSET=latin1";
//		String query_load = "LOAD DATA LOCAL INFILE 'D:/LiuLiu/Listen/Research Resource/Google5gram/3gramExamples/"+target_word+".txt' "+
//							"INTO TABLE liuliu."+target_word+"_3grams FIELDS TERMINATED BY '\t' (trigram, frequency)";
		
		db.executeUpdate(query_create);
		//db.executeUpdate(query_load);
	}
	
	/**
	 * @param filename
	 */
	public void readGoogle3grams(String filename){
		try{
			int lastnumber = Integer.parseInt(filename.substring(filename.length()-1, filename.length()));
//			File googlefile = new File(filename);
//			FileReader fr = new FileReader(googlefile);
//			BufferedReader br = new BufferedReader(fr);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir+"all_words_trigrams.txt", true), "UTF-8"));
			
			String line = "";
			Hashtable<String, Integer> wordflag = new Hashtable<String, Integer>(); 
			while((line = br.readLine())!=null){
				String word_count[] = line.split("\t");
				
				String words[] = word_count[0].split(" ");
				wordflag.clear();
				//wordflag is used to avoiding add a 5gram two times if it contains two same target words
				for(int i = 0;i<3;i++){
					String currentword = words[i].toLowerCase();
					
					if(keywords.contains(currentword)&&(!wordflag.containsKey(currentword))){ //this is a target word
						wordflag.put(currentword, 1);
						//bw.write(line+"\n");
						bw.write(currentword+"\t"+line+"\t"+words[0]+"\t"+words[1]+"\t"+words[2]+"\n");
						//System.out.println(currentword+"\t"+line+"\t"+words[0]+"\t"+words[1]+"\t"+words[2]+"\n");
						//keywords_trigrams.put(currentword, keywords_trigrams.get(currentword)+line+"\n");
					}
				}
			}
			br.close();
			bw.close();
			//if((lastnumber+3)%10==0){
			//	writeText();
			//}
		}catch(IOException e){
			e.printStackTrace();
		}		
	}
	
	public void writeText(){
		try{
			Enumeration e = keywords_trigrams.keys();
			while(e.hasMoreElements()){
				String keyword = (String)e.nextElement();
				String content = keywords_trigrams.get(keyword);
				//FileWriter fw = new FileWriter(dir+keyword+"_53.txt", true);
				//BufferedWriter bw = new BufferedWriter(fw);
				
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir+keyword+"_53.txt", true), "UTF-8"));
				
				bw.write(content);
				bw.close();
				keywords_trigrams.put(keyword, ""); //clear the content
			}
			System.out.println("Finish...");
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	
	public void loadGoogle3grams(){
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ForTrigrams ft = new ForTrigrams(".//input//Tier2.txt");
		TimeStat ts = new TimeStat();
		Database db = new Database();
		ts.start();
		
		/*1. write trigrams of all the target words in a file*/
		/*
		for(int i = 0;i<98;i++){
			if(i<10){
				ft.readGoogle3grams("H:\\Google5gram\\3grams\\3gm-000"+i);
			}else{
				ft.readGoogle3grams("H:\\Google5gram\\3grams\\3gm-00"+i);
			}
			Float timecost = ts.getElapsedTimeSeconds();
			System.out.println(i+"\t"+timecost);
			ts.start();
		}
		Float timecost = ts.getElapsedTimeSeconds();
		System.out.println(timecost);
		*/
		
		
		/*2. Create a table for all the trigrams*/
		ft.createAll3gramTable(db);
	}

}
