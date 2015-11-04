package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
/**
 * @author liuliu
 * @since 2009/06/10
 * @version 1.1
 *	Involving threads
 */
public class GrammarChecker {
	private static GrammarChecker instance = null;
	private static MyProcess proc_checker = new MyProcess();  
	private static int counter; //count the number of parsed sentences

	public static GrammarChecker getInstance() {
		if (instance == null) instance = new GrammarChecker();
		return instance;
	}
	private GrammarChecker(){
		try{
			counter = 0;
			//proc_checker= new MyProcess();
			proc_checker.start("link41b_release.exe", "-batch");
			for(int i = 0;i<53;i++)
				proc_checker.readLine();
			proc_checker.write("!graphics\n");
			proc_checker.readLine();
		}catch(Exception e){
			e.printStackTrace();
			proc_checker.printErr();
		}
	}
	
	public void restart(){
		try{
			counter = 1;
			//proc_checker= new MyProcess();
			proc_checker.start("link41b_release.exe", "-batch");
			for(int i = 0;i<53;i++)
				proc_checker.readLine();
			proc_checker.write("!graphics\n");
			proc_checker.readLine();
		}catch(Exception e){
			e.printStackTrace();
			proc_checker.printErr();
		}
	}
	
	public void endGrammarChecker(){
		proc_checker.write("quit\n");
		proc_checker.destroy();
	}
	
	public boolean checkGrammar(String sentence){
		if(counter==10000){
			this.endGrammarChecker();
			this.restart();
		}else
			counter++;
			
		sentence = sentence.replaceAll("<S> ", "");
		sentence = sentence.replaceAll(" </S>", "");
		String line = "";
		sentence = sentence.split("\t")[0];
		sentence+="\n";
		
		proc_checker.write(sentence);
//		System.out.println(sentence);
//		System.out.println(counter);
		line = proc_checker.readLine();
		//System.out.println(line);
		if(line.charAt(6)=='c'){
			return true;
		}
		else if(line.charAt(6)=='e'){
			return false;
		}else{
			//need to restart program
			instance = null;
			this.getInstance();
			return false;
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		GrammarChecker gc = GrammarChecker.getInstance();
		for(int i = 0;i<1000;i++){
		System.out.println(gc.checkGrammar("But there has been a tremendous blessing in my life song \t15"));
		System.out.println(gc.checkGrammar("The Queen is most anxious to get rid of \t15"));
		System.out.println(gc.checkGrammar("I go go to school \t15"));
		System.out.println(gc.checkGrammar("I go go to school \t15"));
		System.out.println(gc.checkGrammar("What message of hope and courage in the face of increasing \t15"));

		System.out.println(gc.checkGrammar("I go go to school \t15"));
		System.out.println(gc.checkGrammar("I go go to school \t15"));
		System.out.println(gc.checkGrammar("I go go to school \t15"));
		System.out.println(gc.checkGrammar("I go go to school \t15"));
		}
		gc.endGrammarChecker();
	}
	
}
