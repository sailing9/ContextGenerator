package generator;

import util.POStagger;
import util.Database;
import java.io.*;

public class generate_incomplete {

	/**
	 * @param args
	 */
	
	public void check_end(String sentence, POStagger tagger, Database db){
		String[] words = sentence.split("\t")[0].split(" ");
		int length = words.length;
		String[] ending = {"because", "if", "when", "is", "any", "some"};
		for(int i = 0;i<6;i++){
			if(words[length-1].equalsIgnoreCase(ending[i])){
				System.out.println(sentence.split("\t")[0]+"\t"+ending[i]);
			}
		}if(words[length-1].equalsIgnoreCase("as")&&words[length-2].equalsIgnoreCase("such")){
			System.out.println(sentence.split("\t")[0]+"\t"+"such as");
		}
		if(tagger.tag(sentence, words[length-1], "verb",db))
		{
			System.out.println(sentence.split("\t")[0]+"\t"+"verb");
		}
		if(tagger.tag(sentence, words[length-1], "adj",db))
		{
			System.out.println(sentence.split("\t")[0]+"\t"+"adj");
		}
	}
	
	public void choose_incomplete(String filename, POStagger tagger, Database db){
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while((line=br.readLine())!=null){
				check_end(line, tagger, db);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		generate_incomplete gi = new generate_incomplete();
//		POStagger pt = POStagger.getInstance();
//		String filename = ".\\input\\after_grammarchecking\\";
//		gi.choose_incomplete(filename+"slender_filter.txt", pt);
//		gi.choose_incomplete(filename+"anxious_filter.txt", pt);
//		gi.choose_incomplete(filename+"courage_filter.txt", pt);
//		gi.choose_incomplete(filename+"declare_filter.txt", pt);
//		gi.choose_incomplete(filename+"extinct_filter.txt", pt);
//		gi.choose_incomplete(filename+"merchant_filter.txt", pt);
//		gi.choose_incomplete(filename+"stout_filter.txt", pt);
//		gi.choose_incomplete(filename+"remarkable_filter.txt", pt);
//		gi.choose_incomplete(filename+"suspicious_filter.txt", pt);
//		gi.choose_incomplete(filename+"tremendous_filter.txt", pt);
		
//		String filename = ".\\input\\before_grammarchecking\\";
//		gi.choose_incomplete(filename+"slender_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"anxious_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"courage_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"declare_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"extinct_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"merchant_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"stout_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"remarkable_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"suspicious_filter_related.txt", pt);
//		gi.choose_incomplete(filename+"tremendous_filter_related.txt", pt);
		
		Filters f = new Filters();
		Database db = new Database();
		f.initialFunctionWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\functionWords.txt", db);
		
		f.cluster_without_functionwords("tocluster.txt", "incomplete", 3, db);
	
	}

}
