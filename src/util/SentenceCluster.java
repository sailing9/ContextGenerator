package util;

import java.util.ArrayList;
import java.util.HashSet;
public class SentenceCluster {

	public HashSet<String> common_words; //words and baseforms of words
	public String common_words_bases;   //"word1_base word2_base"
    public ArrayList<String> sentences;
    public int average_frequency;
    public int num_contentword;
    public int num_relatedword;
    public int sentence_length;
	public int num_sentence;
	/**
	 * @param args
	 */
	
	public SentenceCluster(){
		common_words = new HashSet<String>();
		sentences = new ArrayList<String>();
		common_words_bases = "";
		num_sentence = 0;
	}
	
	public SentenceCluster(int number){
		common_words = new HashSet<String>();
		sentences = new ArrayList<String>();
		common_words_bases = "";
		num_sentence = 0;
	}
	
	public void setCommonWordsHash(String words){
		String[] word = words.split(" ");
		common_words.clear();
		for(int i = 0;i<word.length;i++){
			common_words.add(word[i]);
		}
	}
	
	public void setCommonWordsString(String words){
		common_words_bases = words;
	}
	
	public void removeCommonWord(String word){
		common_words.remove(word);
	}
	
	public HashSet<String> getCommonWordsHash(){
		return common_words;
	}
	
	public String getCommonWordsString(){
		return common_words_bases;
	}
	
	public void addSentence(String sentence){
		sentences.add(sentence);
		num_sentence++;
	}
	
	public ArrayList<String> getSentences(){
		return sentences;
	}
	
	public String getSentence(int index){
		return sentences.get(index);
	}
	
	public int getNumSentence(){
		return num_sentence;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
