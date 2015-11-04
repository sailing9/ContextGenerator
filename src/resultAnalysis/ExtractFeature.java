package resultAnalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import util.Database;
import generator.Filters;
/**
 * @author liuliu
 * prepare for learning the model using SVM
 * extract the values of all the features of each generated contexts
 *
 */
public class ExtractFeature {

	/**
	 * @param args
	 */
	
	public Hashtable<String, Integer> sentence_frequency;
	
	public ExtractFeature(){
		sentence_frequency = new Hashtable<String, Integer>();
	}
	
	public void loadAllSentenceFrequency(String FileDir){
		File dir = new File(FileDir);
		File[] files = dir.listFiles();
		for(File f:files){
			//System.out.println(f.getAbsolutePath());
			readFile(f.getAbsolutePath());
		}
	}
	
	public void readFile(String filename){
		try{
			File f = new File(filename);
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			
			while((line = br.readLine())!=null){
				String[] words = line.split("\t");
				if(sentence_frequency.containsKey(words[0])){
					System.out.println(filename);
					System.out.println(line);
				}else{
					sentence_frequency.put(words[0], Integer.parseInt(words[1]));
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void computeFeaturesAllWords(String FileDir, Filters rr, Database db){
		File dir = new File(FileDir);
		File[] files = dir.listFiles();
		for(File f:files){
			computeFeatures(f.getAbsolutePath(), rr, db);
		}
	}
	
	
	public void computeFeatures(String filename, Filters rr, Database db){
		try{
			File f = new File(filename);
			String[] filenames = f.getAbsolutePath().split("\\\\");
			String target_word = filenames[filenames.length-1].split("_")[0];
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			HashSet<String> related_word_list = new HashSet<String>();
			rr.readTop100RelatedWordList(target_word, related_word_list, db);
			int i = 0;
			while((line = br.readLine())!=null&&line.length()>1 && i<6){
				String[] words = line.split("\t");
				//computer sentence frequency
				if(sentence_frequency.containsKey(words[0])){
					System.out.print(words[0]+"\t"+sentence_frequency.get(words[0])+"\t");
				}else{
					//System.out.println(filename);
					System.out.println(line);
					continue;
				}
				//compute sentence length
				int length = rr.calSentenceLength(line);
				
				
				//compute content words
				int content_words = rr.calNumContentWord(line);
				//compute related words
				int related_words = rr.calNumRelatedWord(line, target_word, related_word_list);
				System.out.println(length+"\t"+content_words+"\t"+related_words);
				i++;
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void computeRelatedAllWords(String FileDir, Filters rr, Database db){
		File dir = new File(FileDir);
		File[] files = dir.listFiles();
		for(File f:files){
			computeRelatedness(f.getAbsolutePath(), rr, db);
		}
	}
	
	
	public void computeRelatedness(String filename, Filters rr, Database db){
		try{
			File f = new File(filename);
			String[] filenames = f.getAbsolutePath().split("\\\\");
			String target_word = filenames[filenames.length-1].split("_")[0];
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			HashSet<String> related_word_list = new HashSet<String>();
			rr.readTop100RelatedWordList_MI(target_word, related_word_list, db);
			int i = 0;
			while((line = br.readLine())!=null&&line.length()>1 && i<6){
				String[] words = line.split("\t")[0].split(" ");
				//find related words (top 100)
				String currentRelatedWords = "";
				double[] relatedness = new double[words.length];
				double average = 0.0;
				int not_NaN = 0;
				String newline = "";
				
				//get the position of target word
				int index = -1; //index of the target word in a sentence
				for(int j = 0;j<words.length;j++){
					if(words[j].equalsIgnoreCase(target_word)){
						index = j;
						break;
					}
				}
				
				for(int j = 0;j<words.length;j++){
					String base = getMostFrequentBase(words[j], db)==null?words[j].toLowerCase():getMostFrequentBase(words[j], db);	
					newline +=base+" ";
					//calcualte relatedness of each content word
					//relatedness[j]=computeNumericRelatedness(base, target_word, db);
					//relatedness[j]=computeMIRelatedness(base, target_word, db);
					int displacement = j-index;
					
					relatedness[j]=computeMIRelatedness_withDisplacement(displacement, base, target_word, db);
					//relatedness[j]=computeNumericRelatedness_withDisplacement(displacement, base, target_word, db);
					if(!Double.toString(relatedness[j]).equalsIgnoreCase("NaN")){
						average+=Math.log(Math.abs(relatedness[j]))*(relatedness[j]/Math.abs(relatedness[j]));
						not_NaN++;
					}
				}
				newline = newline.trim();
				
				//compute content words
				int content_words = rr.calNumContentWord(newline);
				//compute related words
				int related_words = rr.calNumRelatedWord(newline, target_word, related_word_list);
				//System.out.print(line+"\t"+content_words+"\t"+related_words+"\t"+((double)average/not_NaN));
				System.out.print(related_words);
				for(int j = 0;j<words.length;j++){
					//System.out.print("\t"+words[j]+"\t"+Math.log(Math.abs(relatedness[j]))*(relatedness[j]/Math.abs(relatedness[j])));
					
					if(!Double.toString(relatedness[j]).equalsIgnoreCase("NaN")){
					//	System.out.print(relatedness[j]+"\t");
					}
				}
				System.out.println();
				i++;
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void computeDictRelatedness(String filename, Filters rr, Database db){
		try{
			File f = new File(filename);
			
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			HashSet<String> related_word_list = new HashSet<String>();
			
			while((line = br.readLine())!=null&&line.length()>1 ){
				String target_word = line.split("\t")[0];
				rr.readTop100RelatedWordList(target_word, related_word_list, db);
				String[] words = line.split("\t")[1].split(" ");
				
				//find related words (top 100)
				String currentRelatedWords = "";
				double[] relatedness = new double[words.length];
				double average = 0.0;
				int not_NaN = 0;
				String newline = "";
				for(int j = 0;j<words.length;j++){
					String base = getMostFrequentBase(words[j], db)==null?words[j].toLowerCase():getMostFrequentBase(words[j], db);	
					newline +=base+" ";
					//calcualte relatedness of each content word
					relatedness[j]=computeNumericRelatedness(base, target_word, db);
					if(!Double.toString(relatedness[j]).equalsIgnoreCase("NaN")){
						average+=Math.log(Math.abs(relatedness[j]))*(relatedness[j]/Math.abs(relatedness[j]));
						not_NaN++;
					}
				}
				newline = newline.trim();
				
				//compute content words
				int content_words = rr.calNumContentWord(newline);
				//compute related words
				int related_words = rr.calNumRelatedWord(newline, target_word, related_word_list);
				System.out.print(line+"\t"+content_words+"\t"+related_words+"\t"+((double)average/not_NaN));
				for(int j = 0;j<words.length;j++){
					//if(!Double.toString(relatedness[j]).equalsIgnoreCase("NaN")){
						System.out.print("\t"+words[j]+"\t"+Math.log(Math.abs(relatedness[j]))*(relatedness[j]/Math.abs(relatedness[j])));
					//}
				}
				System.out.println();
				
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	public double computeNumericRelatedness(String word, String target_word, Database db){
		double relatedness = Double.NaN;
		try{
			String query = "select new_measure from liuliu."+target_word
			+"_relatedwords where related_word_base = '"+word+"' and webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0";
			ResultSet rs = db.execute(query);
			while(rs.next()){
				relatedness = rs.getDouble("new_measure");				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return relatedness;
		
	}
	
	public double computeMIRelatedness(String word, String target_word, Database db){
		double relatedness = Double.NaN;
		try{
			String query = "select mutual_information from liuliu."+target_word
			+"_relatedwords where related_word_base = '"+word+"' and webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0";
			ResultSet rs = db.execute(query);
			while(rs.next()){
				relatedness = rs.getDouble("mutual_information");				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return relatedness;
		
	}
	
	public double computeNumericRelatedness_withDisplacement(int displacement, String word, String target_word, Database db){
		double relatedness = Double.NaN;
		if(displacement==0)
			return relatedness;
		String name = "new_measure_";
		if(displacement < 0)
			name+="left_"+Math.abs(displacement);
		else
			name+="right_"+Math.abs(displacement);
		try{
			String query = "select "+name+" from liuliu."+target_word
			+"_relatedwords where related_word_base = '"+word+"' and webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0 and "+name+" is not NULL";
			ResultSet rs = db.execute(query);
			while(rs.next()){
				relatedness = rs.getDouble(name);				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return relatedness;
		
	}
	
	public double computeMIRelatedness_withDisplacement(int displacement, String word, String target_word, Database db){
		double relatedness = Double.NaN;
		if(displacement==0)
			return relatedness;
		String name = "mutual_information_";
		if(displacement < 0)
			name+="left_"+Math.abs(displacement);
		else
			name+="right_"+Math.abs(displacement);
		try{
			String query = "select "+name+" from liuliu."+target_word
			+"_relatedwords where related_word_base = '"+word+"' and webby = 0 and function_word = 0"+
			" and in_alphabetOrder_list = 0 and "+name+" is not NULL";
			ResultSet rs = db.execute(query);
			while(rs.next()){
				relatedness = rs.getDouble(name);				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return relatedness;
		
		
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
	public void splitTrainingTest(String filename, String training, String test){
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(training));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(test));
			String line = "";
			while((line = br.readLine())!=null){
				String[] words = line.split("\t");
				if(words[words.length-1].equalsIgnoreCase("t")){
					bw1.write(line+"\n");
				}else{
					bw2.write(line+"\n");
				}
			}
			bw1.close();
			bw2.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
		
	/**
	 * @param input: training data or test data in original format
	 * @param output: training data or test data with SVM input format
	 * @param type: training or test
	 */
	public void formatDataToSVMrank(String input, String output, String type){
		try{
			//input data format:
			//"sentences	qid frequency	length	content_words	related_word	machine_score	general score	good use of the words	context constraining	ease/difficulty of comprehending	tag"
			//output data format:
			//<line> .=. <target> qid:<qid> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
			//3 qid:1 1:1 2:1 3:0 4:0.2 5:0 # 1A
			
			BufferedReader br = new BufferedReader(new FileReader(input));
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(output+"\\general_"+type+".txt"));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(output+"\\gooduse_"+type+".txt"));
			BufferedWriter bw3 = new BufferedWriter(new FileWriter(output+"\\constraint_"+type+".txt"));
			BufferedWriter bw4 = new BufferedWriter(new FileWriter(output+"\\comprehensible_"+type+".txt"));
			
			String line = "";
			while((line = br.readLine())!=null){
				String[] words = line.split("\t");
				//write to general score trainning file
				bw1.write(words[11]+" qid:"+words[1]+" 1:"+words[3]+" 2:"+words[5]+" 3:"+words[7]+" 4:"+words[9]);
				bw1.newLine();
				bw2.write(words[12]+" qid:"+words[1]+" 1:"+words[3]+" 2:"+words[5]+" 3:"+words[7]+" 4:"+words[9]);
				bw2.newLine();
				bw3.write(words[13]+" qid:"+words[1]+" 1:"+words[3]+" 2:"+words[5]+" 3:"+words[7]+" 4:"+words[9]);
				bw3.newLine();
				bw4.write(words[14]+" qid:"+words[1]+" 1:"+words[3]+" 2:"+words[5]+" 3:"+words[7]+" 4:"+words[9]);
				bw4.newLine();
			}
			bw1.close();
			bw2.close();
			bw3.close();
			bw4.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	public void formatDataToSVMlight(String input, String output){
		try{
			BufferedReader br = new BufferedReader(new FileReader(input));
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			String line = "";
			while((line = br.readLine())!=null){
				String[] words = line.split(",");
				//classification
				if(words[12].endsWith("0"))
					words[12]="-1";
				//bw.write(words[12]+" 1:"+words[2]+" 2:"+words[3]+" 3:"+words[4]+" 4:"+words[5]);
				//regression
				bw.write(words[7]+" 1:"+words[2]+" 2:"+words[3]+" 3:"+words[4]+" 4:"+words[5]);
				
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void formatDataToWeka(String input, String output){
		try{
			BufferedReader br = new BufferedReader(new FileReader(input));
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			String line = "";
			while((line = br.readLine())!=null){
				String[] words = line.split(",");
				//write to general score trainning file
				//bw.write(words[2]+","+words[3]+","+words[4]+","+words[5]+","+words[12]);
				bw.write(words[3]+","+words[4]+","+words[5]+","+words[12]);
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void findformat(String input, String output){
		try{
			BufferedReader br = new BufferedReader(new FileReader(input));
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			String line = "";
			while((line = br.readLine())!=null){
				line = line.replaceAll(",", "\t");
				//write to general score trainning file
				//bw.write(words[2]+","+words[3]+","+words[4]+","+words[5]+","+words[12]);
				bw.write(line);
				bw.newLine();
			}
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public double computeMean_Arithmetic(ArrayList number){
		double mean = 0.0;
		for(int i = 0;i<number.size();i++)
			mean+=(Double)number.get(i);
		mean/=number.size();
		return mean;
	}
	
	public double computeMean_Geometric(ArrayList number){
		double mean = 1.0;
		for(int i = 0;i<number.size();i++)
			mean*=Math.pow((Double)number.get(i),(double)1/(double)number.size());
		if(Double.toString(mean).equalsIgnoreCase("NaN")){
			System.out.println("!!!!!!!!!!!!!!!"+number);
		}
		return mean;
	}
	
	public double computeMean_Harmonic(ArrayList number){
		double mean = 0.0;
		for(int i = 0;i<number.size();i++)
			mean+=(double)1/(Double)number.get(i);
		mean = Math.pow(mean, -1)*number.size();
		return mean;
	}
	
	public double computeMedian(ArrayList<Double> number){
		double median = 0.0;
		Double numbers[] = new Double[number.size()];
		number.toArray(numbers);
		Arrays.sort(numbers);
		if(numbers.length%2==0)
			return (numbers[numbers.length/2-1]+numbers[numbers.length/2])/(double)2;
		else
			return numbers[(numbers.length-1)/2];
	}
	
	public void computeMeanHighLow(String filename){
		try{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
		//	System.out.println("max"+"\t"+"min"+"\t"+"arithmeticMean"+"\t"+"geometricMean"+"\t"+"harmonicMean"+"\t"+"median");
			System.out.println("max"+"\t"+"min"+"\t"+"arithmeticMean"+"\t"+"harmonicMean"+"\t"+"median");
	
			while((line = br.readLine())!=null){
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				String[] numbers = line.split("\t");
				ArrayList<Double> numberlist = new ArrayList<Double>();
				for(int i = 0;i<numbers.length;i++){
					double current_number =Double.parseDouble(numbers[i]); 
					numberlist.add(current_number);
					if(current_number>max)
						max = current_number;
					if(current_number<min)
						min = current_number;		
				}
				double arithmeticMean = computeMean_Arithmetic(numberlist);
				//double geometricMean = computeMean_Geometric(numberlist);
				double harmonicMean = computeMean_Harmonic(numberlist);
				double median = computeMedian(numberlist);
				//System.out.println(max+"\t"+min+"\t"+arithmeticMean+"\t"+geometricMean+"\t"+harmonicMean+"\t"+median);
				System.out.println(max+"\t"+min+"\t"+arithmeticMean+"\t"+harmonicMean+"\t"+median);
				
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Filters rr = new Filters();
		Database db = new Database();
		rr.initialRemoveString("D:\\LiuLiu\\Listen\\Research Resource\\Punctuations\\punctuations.txt");
		rr.initialFunctionWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\functionWords.txt", db);
		rr.initialWebWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\webSpecific\\webspecific.txt", db);
		
		String fileWithFrequency = "D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\CONTEXT_GENERATION\\forevaluate\\Experiment_results_to_Moddy\\complete_sentences";
		String examplesFile = "D:\\LiuLiu\\Listen\\SourceSafe\\Documentation\\Experiments\\2008 Vocabulary\\Liu\\results\\context_generation_result_toModdy";
		
		ExtractFeature m = new ExtractFeature();
//		m.loadAllSentenceFrequency(fileWithFrequency);
//		m.computeFeaturesAllWords(examplesFile, rr, db);
//		String wholedata = "D:\\LiuLiu\\Listen\\Research Progress\\2009Spring\\Week12(3.30-4.3)\\model_text.txt";
//		String training = "D:\\LiuLiu\\Listen\\Research Progress\\2009Spring\\Week14(4.13-4.17)\\partition3\\train.txt";
//		String test = "D:\\LiuLiu\\Listen\\Research Progress\\2009Spring\\Week14(4.13-4.17)\\partition3\\test.txt";
//		String outputFolderSVM = "D:\\LiuLiu\\Listen\\Research Resource\\SVM\\ourModel";
		//m.splitTrainingTest(wholedata, training, test);
		//m.formatDataToSVMrank(training, outputFolderSVM, "train");
		//m.formatDataToSVMrank(test, outputFolderSVM, "test");
		
//		String dtreeinput = "D:\\LiuLiu\\Listen\\Research Progress\\2009Spring\\Week14(4.13-4.17)\\decisiontree.txt";
//		String dtreeoutput = "D:\\LiuLiu\\Listen\\Research Progress\\2009Spring\\Week14(4.13-4.17)\\sentence_svmr_train.txt";
		//m.formatDataToWeka(dtreeinput, dtreeoutput);
//		m.findformat(dtreeinput, "result");
//		m.formatDataToSVMlight(dtreeinput, dtreeoutput);
		
		//relatedness analysis
		//generated contexts
		//m.computeRelatedAllWords(examplesFile, rr, db);
		m.computeMeanHighLow(".\\input\\relatedness_MI_displacement.txt");
		System.out.println();
		m.computeMeanHighLow(".\\input\\relatedness_MI_nodisplacement.txt");
		System.out.println();
		m.computeMeanHighLow(".\\input\\relatedness_newmeasure_displacement.txt");
		System.out.println();
		m.computeMeanHighLow(".\\input\\relatedness_newmeasure_nodisplacement.txt");
		
//		m.computeMeanHighLow(".\\input\\trigram_frequency.txt");
//		System.out.println();
//		m.computeMeanHighLow(".\\input\\fivegram_frequency.txt");
		
		
		//dict
		String dictfile = "D:\\LiuLiu\\Listen\\Research Progress\\2009Summer\\week1(5.11-5.15)\\dict_examples.txt";
		//m.computeDictRelatedness(dictfile, rr, db);
		
	}

}

