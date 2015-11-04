package generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import java.util.HashMap;

import util.Database;
import util.SentenceCluster;
import util.SortHashtableByValue;
import util.TimeStat;
import relatedWord.RelatedWords;

/**
 * @author liuliu Generate context candidates Using 10 filters which defined in
 *         Filters.java
 */

/**
 * @author liuliu
 *
 */
/**
 * @author liuliu
 *
 */
public class Generator {

	Random rn;
	int duplicate;
	public int[][] MNF; // Monotonically necessary filter
	// MNF
	/*
	 * 0: start five-grams doesn't contain related words 1: contain taboo words
	 * 2: contain special punctuations or special symbols 3: contain web
	 * specific words 4: contain relative pronouns
	 */
	public int[][] MSF; // Monotonically sufficient filter
	// MSF
	/*
	 * 0: contain more than 4 capitalized words or numbers 1: contian more that
	 * 2 words above grade level 2
	 */
	public int[][] HF; // Holistic filter
	// HF
	/*
	 * 0: not start with <S> or capitalized word, or end with </S> or
	 * punctuations 1: end with modal or auxiliary words 2: contain less than
	 * three content words
	 */

	public int CF; // Competitive filter
	// CF: choose diverse five-grams

	public int[] nodes; // number of nodes in each level if not pruning

	/**
	 * @param args
	 */

	public Generator() {
		MNF = new int[5][5];
		MSF = new int[5][2];
		HF = new int[5][3];
		CF = 0;
		rn = new Random();
		nodes = new int[5];
		for (int i = 0; i < 5; i++) {
			nodes[i] = 0;
			for (int j = 0; j < 5; j++) {
				MNF[i][j] = 0;
				if (j < 2) {
					MSF[i][j] = 0;
					HF[i][j] = 0;
				}
			}
			HF[i][2] = 0;
		}
		duplicate = 0;
	};

	public void clearArray() {
		CF = 0;
		for (int i = 0; i < 5; i++) {
			nodes[i] = 0;
			for (int j = 0; j < 5; j++) {
				MNF[i][j] = 0;
				if (j < 2) {
					MSF[i][j] = 0;
					HF[i][j] = 0;
				}
			}
			HF[i][2] = 0;
		}
	}


	public String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
	
	/**
	 * @param target_word
	 *            : word in our target word list
	 * @param db
	 *            : Database
	 * @param threshold: the number of overlapping words
	 * @param startnum: the number of start five-grams
	 * @return five_gram: the highest frequent five gram which contains the
	 *         target word
	 * @author liuliu
	 * 
	 */
	public ArrayList<String> chooseStart5gram(int startnum, int threshold,
			Filters rr, RelatedWords rw, String target_word, Database db,
			HashSet<String> relatedwords) {
		ArrayList<String> five_grams = new ArrayList<String>();
		long frequency = Long.MAX_VALUE;
		try {
			ArrayList<SentenceCluster> currentscs = new ArrayList<SentenceCluster>();
			while (currentscs.size() < startnum + 1) {
				String query = "select five_gram, frequency from liuliu."
						+ target_word + "_5grams where frequency < "
						+ frequency + " order by frequency desc limit 200";
				ResultSet rs = db.execute(query);
				int flag = 0;
				while (rs.next()) {
					flag = 1;
					String current_five_gram = rs.getString("five_gram");
					frequency = rs.getLong("frequency");
					String[] words = current_five_gram.split(" ");
					// first, filter bad five-grams from set of start five-grams
					if (!rr.fitlers_whetherextend(target_word, words, rw, db))
						continue;
					int num_relatedwords = 0;
					int num_words = 0;
					String[] base = new String[5];
					for (int i = 0; i < 5; i++) {
						base[i] = rw.getMostFrequentBase(words[i], db) == null ? words[i]
								.toLowerCase()
								: rw.getMostFrequentBase(words[i], db);
						if (relatedwords.contains(base[i])) {
							num_relatedwords++;
						}
						if (base[i].matches("[a-zA-Z' 1-9]+")) {
							num_words++;
						} else {
							if (base[i].contains("<S>")
									|| base[i].contains("</S>"))
								num_words++;
						}
					}
					if (num_relatedwords == 0)
						continue; // this five-gram doesn't contain related
									// words, we don't choose it
					// We only use this heuristic in the start five-grams
					// choosing step
					if (num_words < 5) {
						continue;
					}
					// cluster this sentence;

					// for each five-gram, the first step is to check whether
					// this five-gram could parse the filters
					String line = current_five_gram + "\t" + frequency;
					int numberOfclusters = currentscs.size();
					int numSelectedClusters = 0;
					for (int i = 0; i < numberOfclusters; i++) {

						// compare the new sentence with each cluster
						SentenceCluster sc = currentscs.get(i);
						HashSet<String> scCommonWords = sc.getCommonWordsHash(); // get
																					// the
																					// common
																					// words
																					// of
																					// the
																					// cluster
						// compare the sentence with the common words, and
						// calculate the number of words in common
						int numOfCommon = 0;
						String newCommonWords = "";
						// here, we should compare lowercase, base forms of the
						// words so first, we need to change the words in sentence
						// into their baseforms

						for (int j = 0; j < words.length; j++) {
							if (scCommonWords.contains(base[j])) {
								newCommonWords += base[j] + " ";
								numOfCommon++;
							}
						}
						// judge whether the sentence belongs to the cluster
						// if the number of common words is above the threshold,
						// then it is
						// otherwise, try the next cluster
						if (numOfCommon >= threshold) {
							sc.addSentence(line);
							sc.setCommonWordsHash(newCommonWords.trim());
							currentscs.set(i, sc);
							numSelectedClusters++;
							// if we require that a sentence can and only can be
							// clustered into one cluster
							// then here we break
							// otherwise, we continue checking whether this
							// sentence can be clustered into other cluster or
							// not
							break;

						} else {
							// try the next cluster
						}
					}

					// if the senentence doesn't belong to any cluster
					// add a new cluster to the final results
					if (numSelectedClusters == 0) {
						SentenceCluster sc2 = new SentenceCluster();
						sc2.addSentence(line); // The sentence and the frequency
												// of the sentence. But common
												// words don't include
												// frequency.
						String commonWordsHash = "";
						for (int i = 0; i < 5; i++) {
							commonWordsHash += base[i] + " ";
						}
						sc2.setCommonWordsHash(commonWordsHash.trim());
						currentscs.add(sc2);
					}
					if (currentscs.size() > startnum)
						break;

				}
				if (flag == 0)
					break;

			}
			// now, get the representative for each cluster
			for (int i = 0; i < currentscs.size(); i++) {
				SentenceCluster sc = currentscs.get(i);
				five_grams.add(sc.getSentence(0));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return five_grams;
	}

	
	/**
	 * @param startnum: number of start five-grams
	 * @param threshold: number of words in common per cluster
	 * @param rr: 
	 * @param rw
	 * @param target_word
	 * @param db
	 * @param relatedwords
	 * @author liuliu
	 * @purpose: cluster start five-grams with an efficient way - sort of using inverted indexing
	 * @time: 2009/06/02
	 */
	public ArrayList<String> chooseStart5gramfrom3gram_speedUp(int startnum, int threshold,
			Filters rr, RelatedWords rw, String target_word, Database db,
			HashSet<String> relatedwords) {
		ArrayList<String> five_grams = new ArrayList<String>();
		Hashtable<String, ArrayList<Integer>> word_cluster = new Hashtable<String, ArrayList<Integer>>();
		long frequency = Long.MAX_VALUE;
		try {
			ArrayList<SentenceCluster> currentscs = new ArrayList<SentenceCluster>();
			HashSet<String> fiveGrams = new HashSet<String>();
			while (currentscs.size() < startnum + 1) {
				//choose start trigrams
				String queryTrigrams = "select trigram, frequency from liuliu.word_trigrams_google" +
						" where target_word = '"+target_word+"' and frequency < " +
								frequency+" order by frequency desc limit 200";
				ResultSet rsTrigrams = db.execute(queryTrigrams);
				int flag = 0;
				while (rsTrigrams.next()) {
					flag = 1;
				
					String current_trigram = rsTrigrams.getString("trigram");
					frequency = rsTrigrams.getLong("frequency");
					//query five-grams which contain this trigram
					
					current_trigram = current_trigram.replaceAll("'", "\\\\'");
					String query = "select five_gram, frequency from liuliu."
						+ target_word + "_5grams where five_gram like '%"+current_trigram+"%' order by frequency desc limit 1";
					ResultSet rs = db.execute(query);
					
					if(!rs.next())
						continue;
					
					String current_five_gram = rs.getString("five_gram");
					long fivegram_frequency = rs.getLong("frequency");
					
					if(fiveGrams.contains(current_five_gram))
						continue;
					
					fiveGrams.add(current_five_gram);
					String[] words = current_five_gram.split(" ");
					// first, filter bad five-grams from set of start five-grams
					if (!rr.fitlers_whetherextend(target_word, words, rw, db))
						continue;
					int num_relatedwords = 0;
					int num_words = 0;
					String[] base_words = new String[5];
					for (int i = 0; i < 5; i++) {
						base_words[i] = rw.getMostFrequentBase(words[i], db) == null ? words[i]
								.toLowerCase()
								: rw.getMostFrequentBase(words[i], db);
						if (relatedwords.contains(base_words[i])) {
							num_relatedwords++;
						}
						if (base_words[i].matches("[a-zA-Z' 1-9]+")) {
							num_words++;
						} else {
							if (base_words[i].contains("<S>")|| base_words[i].contains("</S>"))
								num_words++;
						}
					}
					if (num_relatedwords == 0)
						continue; // this five-gram doesn't contain related words, we don't choose it
					// We only use this heuristic in the start five-grams choosing step
					if (num_words < 5) {// we also restrict that all the five words must be "word" but not symbols or punctuations
						continue;
					}
					
					// cluster this five-gram;

					
					String line = current_five_gram + "\t" + fivegram_frequency;
					
					int numberOfclusters = currentscs.size();
					int putToClusterIndex = -1;
					Hashtable<Integer, Integer> cluster_number = new Hashtable<Integer, Integer>();
					for(int j = 0;j<base_words.length;j++){
						int flag_cluster = 0; // 1 indicate we find the cluster to put the sentence
						
						if(word_cluster.containsKey(base_words[j])){
							ArrayList<Integer> related_clusters = word_cluster.get(base_words[j]);
							for(int k = 0;k<related_clusters.size();k++){
								int cluster_index = related_clusters.get(k);
								if(cluster_number.containsKey(cluster_index)){
									int numOfWordsInCluster = cluster_number.get(cluster_index)+1;
									if(numOfWordsInCluster>=threshold){
										//to do
										//the cluster with index cluster_index is the one to put this sentence
										putToClusterIndex = cluster_index;
										flag_cluster = 1;
										break;
									}
									else
										cluster_number.put(cluster_index,numOfWordsInCluster);
								}else{
									cluster_number.put(cluster_index,1);
									if(threshold<=1)//only contain one content word
									{
										putToClusterIndex = cluster_index;
										flag_cluster = 1;
										break;
									}
								}
							}
							
						}
						if(flag_cluster==1)
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
						
						for(int j=0;j<threshold;j++){
							commonWords+=base_words[j]+" ";
							if(word_cluster.containsKey(base_words[j])){
								ArrayList<Integer> related_clusters = word_cluster.get(base_words[j]);
								related_clusters.add(numberOfclusters);
								word_cluster.put(base_words[j], related_clusters);
							}else{
								ArrayList<Integer> related_clusters = new ArrayList<Integer>();
								related_clusters.add(numberOfclusters);
								word_cluster.put(base_words[j], related_clusters);
							}
						}
						SentenceCluster sc = new SentenceCluster();
						sc.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
						sc.setCommonWordsHash(commonWords.trim());
						currentscs.add(sc);
						if (currentscs.size() > startnum)
							break;
												
					}
					
				}
				if (flag == 0)
					break;
			}
			// now, get the representative for each cluster
			for (int i = 0; i < currentscs.size(); i++) {
				SentenceCluster sc = currentscs.get(i);
				five_grams.add(sc.getSentence(0));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return five_grams;
	}
	
	
	public ArrayList<String> chooseStart5gram_speedUp(int startnum, int threshold,
			Filters rr, RelatedWords rw, String target_word, Database db,
			HashSet<String> relatedwords) {
		ArrayList<String> five_grams = new ArrayList<String>();
		Hashtable<String, ArrayList<Integer>> word_cluster = new Hashtable<String, ArrayList<Integer>>();
		long frequency = Long.MAX_VALUE;
		try {
			ArrayList<SentenceCluster> currentscs = new ArrayList<SentenceCluster>();
			while (currentscs.size() < startnum + 1) {
				String query = "select five_gram, frequency from liuliu."
						+ target_word + "_5grams where frequency < "
						+ frequency + " order by frequency desc limit 200";
				ResultSet rs = db.execute(query);
			
				int flag = 0;
				while (rs.next()) {
					flag = 1;
				
					String current_five_gram = rs.getString("five_gram");
					frequency = rs.getLong("frequency");
					String[] words = current_five_gram.split(" ");
					// first, filter bad five-grams from set of start five-grams
					if (!rr.fitlers_whetherextend(target_word, words, rw, db))
						continue;
					int num_relatedwords = 0;
					int num_words = 0;
					String[] base_words = new String[5];
					for (int i = 0; i < 5; i++) {
						base_words[i] = rw.getMostFrequentBase(words[i], db) == null ? words[i]
								.toLowerCase()
								: rw.getMostFrequentBase(words[i], db);
						if (relatedwords.contains(base_words[i])) {
							num_relatedwords++;
						}
						if (base_words[i].matches("[a-zA-Z' 1-9]+")) {
							num_words++;
						} else {
							if (base_words[i].contains("<S>")|| base_words[i].contains("</S>"))
								num_words++;
						}
					}
					if (num_relatedwords == 0)
						continue; // this five-gram doesn't contain related words, we don't choose it
					// We only use this heuristic in the start five-grams choosing step
					if (num_words < 5) {// we also restrict that all the five words must be "word" but not symbols or punctuations
						continue;
					}
					
					// cluster this five-gram;

					
					String line = current_five_gram + "\t" + frequency;
					
					int numberOfclusters = currentscs.size();
					int putToClusterIndex = -1;
					Hashtable<Integer, Integer> cluster_number = new Hashtable<Integer, Integer>();
					for(int j = 0;j<base_words.length;j++){
						int flag_cluster = 0; // 1 indicate we find the cluster to put the sentence
						
						if(word_cluster.containsKey(base_words[j])){
							ArrayList<Integer> related_clusters = word_cluster.get(base_words[j]);
							for(int k = 0;k<related_clusters.size();k++){
								int cluster_index = related_clusters.get(k);
								if(cluster_number.containsKey(cluster_index)){
									int numOfWordsInCluster = cluster_number.get(cluster_index)+1;
									if(numOfWordsInCluster>=threshold){
										//to do
										//the cluster with index cluster_index is the one to put this sentence
										putToClusterIndex = cluster_index;
										flag_cluster = 1;
										break;
									}
									else
										cluster_number.put(cluster_index,numOfWordsInCluster);
								}else{
									cluster_number.put(cluster_index,1);
									if(threshold<=1)//only contain one content word
									{
										putToClusterIndex = cluster_index;
										flag_cluster = 1;
										break;
									}
								}
							}
							
						}
						if(flag_cluster==1)
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
						
						for(int j=0;j<threshold;j++){
							commonWords+=base_words[j]+" ";
							if(word_cluster.containsKey(base_words[j])){
								ArrayList<Integer> related_clusters = word_cluster.get(base_words[j]);
								related_clusters.add(numberOfclusters);
								word_cluster.put(base_words[j], related_clusters);
							}else{
								ArrayList<Integer> related_clusters = new ArrayList<Integer>();
								related_clusters.add(numberOfclusters);
								word_cluster.put(base_words[j], related_clusters);
							}
						}
						SentenceCluster sc = new SentenceCluster();
						sc.addSentence(line); //The sentence and the frequency of the sentence. But common words don't include frequency.
						sc.setCommonWordsHash(commonWords.trim());
						currentscs.add(sc);
						if (currentscs.size() > startnum)
							break;
												
					}
					
				}
				if (flag == 0)
					break;
			}
			// now, get the representative for each cluster
			for (int i = 0; i < currentscs.size(); i++) {
				SentenceCluster sc = currentscs.get(i);
				five_grams.add(sc.getSentence(0));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return five_grams;
	}
	
	public ArrayList<String> chooseStart5gram_count_filters(int startnum,
			int threshold, Filters rr, RelatedWords rw, String target_word,
			Database db, HashSet<String> relatedwords) {
		ArrayList<String> five_grams = new ArrayList<String>();
		long frequency = Long.MAX_VALUE;
		try {
			ArrayList<SentenceCluster> currentscs = new ArrayList<SentenceCluster>();
			while (currentscs.size() < startnum + 1) {
				String query = "select five_gram, frequency from liuliu."
						+ target_word + "_5grams where frequency < "
						+ frequency + " order by frequency desc limit 200";
				ResultSet rs = db.execute(query);
				int flag = 0;
				while (rs.next()) {
					flag = 1;
					String current_five_gram = rs.getString("five_gram");
					frequency = rs.getLong("frequency");
					String[] words = current_five_gram.split(" ");

					int num_relatedwords = 0;
					int num_words = 0;
					String[] base = new String[5];
					for (int i = 0; i < 5; i++) {
						base[i] = rw.getMostFrequentBase(words[i], db) == null ? words[i]
								.toLowerCase()
								: rw.getMostFrequentBase(words[i], db);
						if (relatedwords.contains(base[i])) {
							num_relatedwords++;
						}
						if (base[i].matches("[a-zA-Z' 1-9]+")) {
							num_words++;
						} else {
							if (base[i].contains("<S>")
									|| base[i].contains("</S>"))
								num_words++;
						}
					}
					if (num_words < 5) { // we didn't consider this filter
						continue;
					}
					// zero, the number of grams in level 5 ++
					nodes[0]++;
					// first filter: related words
					if (num_relatedwords == 0) {
						MNF[0][0]++;
						if (rn.nextDouble() < 0.01) {
							System.out.println("MNF0: " + current_five_gram);
						}
						continue; // this five-gram doesn't contain related
									// words, we don't choose it
						// We only use this heuristic in the start five-grams
						// choosing step

					}
					// second filter: MNF and MSF
					if (!rr.fitlers_whetherextend_count_filters(MNF, MSF, 5,
							target_word, words, rw, db))
						continue;
					// cluster this sentence;

					// for each five-gram, the first step is to check whether
					// this five-gram could parse the filters
					String line = current_five_gram + "\t" + frequency;
					int numberOfclusters = currentscs.size();
					int numSelectedClusters = 0;
					for (int i = 0; i < numberOfclusters; i++) {

						// compare the new sentence with each cluster
						SentenceCluster sc = currentscs.get(i);
						HashSet<String> scCommonWords = sc.getCommonWordsHash(); // get
																					// the
																					// common
																					// words
																					// of
																					// the
																					// cluster
						// compare the sentence with the common words, and
						// calculate the number of words in common
						int numOfCommon = 0;
						String newCommonWords = "";
						// here, we should compare lowercase, base forms of the
						// words
						// So first, we need to change the words in sentence
						// into their baseforms

						for (int j = 0; j < words.length; j++) {
							if (scCommonWords.contains(base[j])) {
								newCommonWords += base[j] + " ";
								numOfCommon++;
							}
						}
						// judge whether the sentence belongs to the cluster
						// if the number of common words is above the threshold,
						// then it is
						// otherwise, try the next cluster
						if (numOfCommon >= threshold) {
							sc.addSentence(line);
							sc.setCommonWordsHash(newCommonWords.trim());
							currentscs.set(i, sc);
							numSelectedClusters++;
							// if we require that a sentence can and only can be
							// clustered into one cluster
							// then here we break
							// otherwise, we continue checking whether this
							// sentence can be clustered into other cluster or
							// not
							break;

						} else {
							// try the next cluster
						}
					}

					// if the senentence doesn't belong to any cluster
					// add a new cluster to the final results
					if (numSelectedClusters == 0) {
						SentenceCluster sc2 = new SentenceCluster();
						sc2.addSentence(line); // The sentence and the frequency
												// of the sentence. But common
												// words don't include
												// frequency.
						String commonWordsHash = "";
						for (int i = 0; i < 5; i++) {
							commonWordsHash += base[i] + " ";
						}
						sc2.setCommonWordsHash(commonWordsHash.trim());
						currentscs.add(sc2);
					}
					if (currentscs.size() > startnum)
						break;

				}
				if (flag == 0)
					break;

			}
			// now, get the representative for each cluster
			for (int i = 0; i < currentscs.size(); i++) {
				SentenceCluster sc = currentscs.get(i);
				five_grams.add(sc.getSentence(0));
				// competitive filter
				CF += sc.getNumSentence() - 1;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return five_grams;
	}

	public ArrayList<String> choosetypicalfivegrams(RelatedWords rw,
			String current_phrase, int condition, Database db,
			HashSet<String> relatedwords) {
		// condition 0:start, 1:left, 2:right
		ArrayList<String> typicalfivegrams = new ArrayList<String>();
		if (condition == 0) {
			String five_gram = "";
			String query = "select five_gram, frequency from liuliu."
					+ current_phrase
					+ "_5grams order by frequency desc limit 1000";
			ResultSet rs = db.execute(query);
			ArrayList<String> previous = new ArrayList<String>();
			long previous_frequency = 0;

			try {
				while (rs.next()) {
					String current_five_gram = rs.getString("five_gram");
					String[] words = current_five_gram.split(" ");
					int num_relatedwords = 0;
					for (int i = 0; i < 5; i++) {
						String base = rw.getMostFrequentBase(words[i], db) == null ? words[i]
								: rw.getMostFrequentBase(words[i], db);
						if (relatedwords.contains(base)) {
							num_relatedwords++;
						}
					}
					if (num_relatedwords == 0)
						continue; // this five-gram doesn't contain related
									// words, we don't choose it
					// We only use this heuristic in the start five-grams
					// choosing step

					long current_frequency = rs.getLong("frequency");
					// first, construct the potential same sentences set
					if (Math.abs(current_frequency - previous_frequency) < 10) {
						previous.add(current_five_gram + "\t"
								+ current_frequency);
						previous_frequency = current_frequency;
					} else {
						// finish construct the previous cluster
						// start to process
						// step 1: find five-grams which don't share 4 words
						// with each other
						// these independent five-grams should be chosen as
						// start symbol
						for (int i = 0; i < previous.size(); i++) {
							String[] ws = previous.get(i).split("\t");
							String leftfour = ws[0] + " " + ws[1] + " " + ws[2]
									+ " " + ws[3];
							String rightfour = ws[1] + " " + ws[2] + " "
									+ ws[3] + " " + ws[4];
							int flag = 0;
							for (int j = 0; j < previous.size(); j++) {
								if (i == j)
									continue;
								String temp = previous.get(j);
								if (temp.contains(leftfour)
										|| temp.contains(rightfour)) {
									flag = 1;
									continue;
								}
							}
							if (flag == 0) {
								// has no overlapping with five-grams in the
								// cluster

								five_gram += previous.get(i) + "\n"; // this one
																		// should
																		// be
																		// chosen
								previous.remove(i);
								i--;
							}
						}
						// start to process the left five-grams in the set:
						// previous
						// these five-grams should overlap with each other
						// we choose the one with the highest frequency
						if (previous.size() > 0)
							five_gram += previous.get(0);

						// we start a new previous set now.
						previous.clear();
						previous.add(current_five_gram + "\t"
								+ current_frequency);
						previous_frequency = current_frequency;
					}

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			five_gram = five_gram.substring(0, five_gram.length() - 1);
		}
		return typicalfivegrams;
	}

	/**
	 * expand the current phrase one gram to the left or one gram to the right
	 * 
	 * @param: target_word
	 * @param: current_phrase
	 * @param: left: if it is true, then expand the current phrase to left,
	 *         otherwise to right
	 * @param: count: the frequency (average frequency) for the phrase
	 */
	public void expand(int threshold, RelatedWords rw, Filters rr,
			Hashtable<String, Long> temp, long count, String target_word,
			String target_pos,
			String current_phrase, Database db,
			Hashtable<String, Long> candidates,
			ArrayList<String> candidatelist, boolean left) {
		
		String[] words = current_phrase.split(" ");
		int length = words.length;
		long frequency = Long.MAX_VALUE;
		int number = 0;
		try {
			if (left) { // expand to the left side
				for (int i = 0; i < 4; i++) {
					words[i] = words[i].trim();
					words[i] = words[i].replaceAll("'", "\\\\'");
				}

				while (number < threshold) {
					String query = "select gram1, frequency, level_gram1, level_gram2, level_gram3, level_gram4,"
							+ "level_gram5 from liuliu."
							+ target_word
							+ "_5grams where gram2 = '"
							+ words[0]
							+ "' and gram3 = '"
							+ words[1]
							+ "' and gram4 = '"
							+ words[2]
							+ "' and gram5 = '"
							+ words[3]
							+ "' and frequency < "
							+ frequency
							+ " order by frequency desc limit 20";
					ResultSet rs = db.execute(query);
					int flag = 0;
					while (rs.next()) {
						flag = 1;
						frequency = rs.getLong("frequency");
						long new_frequency = ((length - 5 + 1) * count + frequency)
								/ (length - 5 + 2);

						String head = rs.getString("gram1");
						String new_phrase = head + " " + current_phrase;
						if (rr.fitlers_whetherextend(target_word, new_phrase
								.split(" "), rw, db)) {
							temp.put(new_phrase, new_frequency);
							number++;
						} else {
							continue;
						}
						if (true) {
							if (rr.filters_whethercandidate(new_phrase + "\t"
									+ new_frequency, target_word, target_pos, db)) {
								if (new_phrase.substring(0, 4).equals("and ")
										|| new_phrase.substring(0, 4).equals(
												"but ")) {
									new_phrase = new_phrase.substring(4,
											new_phrase.length());
								}

								if (!candidates.containsKey(new_phrase)) {
									candidatelist.add(new_phrase + "\t"
											+ new_frequency);
									candidates.put(new_phrase, new_frequency);
								}
							}
						}
						if (number >= threshold)
							break;
					}
					if (flag == 0) { // if we cannot find any five-grams
										// overlapping with current phrase
						break;
					}
				}

			} else {
				for (int i = length - 4; i < length; i++) {
					words[i] = words[i].trim();
					words[i] = words[i].replaceAll("'", "\\\\'");
				}
				while (number < threshold) {
					String query = "select gram5, frequency, level_gram1, level_gram2, level_gram3, level_gram4,"
							+ "level_gram5 from liuliu."
							+ target_word
							+ "_5grams where gram1 = '"
							+ words[length - 4]
							+ "' and gram2 = '"
							+ words[length - 3]
							+ "' and gram3 = '"
							+ words[length - 2]
							+ "' and gram4 = '"
							+ words[length - 1]
							+ "' and frequency < "
							+ frequency
							+ " order by frequency desc limit 20";
					ResultSet rs = db.execute(query);
					int flag = 0;
					while (rs.next()) {
						flag = 1;
						frequency = rs.getLong("frequency");
						long new_frequency = ((length - 5 + 1) * count + frequency)
								/ (length - 5 + 2);
						String tail = rs.getString("gram5");
						String new_phrase = current_phrase + " " + tail;
						if (rr.fitlers_whetherextend(target_word, new_phrase
								.split(" "), rw, db)) {
							temp.put(new_phrase, new_frequency);
							number++;
						} else {
							continue;
						}

							if (rr.filters_whethercandidate(new_phrase + "\t"
									+ new_frequency, target_word, target_pos, db)) {
								if (new_phrase.substring(0, 4).equals("and ")
										|| new_phrase.substring(0, 4).equals(
												"but ")) {
									new_phrase = new_phrase.substring(4,
											new_phrase.length());
								}
								if (!candidates.containsKey(new_phrase)) {
									candidatelist.add(new_phrase + "\t"
											+ new_frequency);
									candidates.put(new_phrase, new_frequency);
								}
							}
						if (number >= threshold)
							break;
					}
					if (flag == 0) { // if we cannot find any five-grams
										// overlapping with current phrase
						break;
					}

				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * expand the current phrase one gram to the left or one gram to the right
	 * 
	 * @param: target_word
	 * @param: current_phrase
	 * @param: left: if it is true, then expand the current phrase to left,
	 *         otherwise to right
	 * @param: count: the frequency (average frequency) for the phrase
	 * @author liuliu
	 * @version 1.0
	 * @since 2009/06/03
	 */
	public void expand_withStartEnd(int threshold, RelatedWords rw, Filters rr,
			Hashtable<String, Long> temp, long count, String target_word,
			String target_pos,
			String current_phrase, Database db,
			Hashtable<String, Long> candidates,
			ArrayList<String> candidatelist, boolean left) {
		
		String[] words = current_phrase.split(" ");
		int length = words.length;
		long frequency = Long.MAX_VALUE;
		int number = 0;
		try {
			if (left) { // expand to the left side
				for (int i = 0; i < 4; i++) {
					words[i] = words[i].trim();
					words[i] = words[i].replaceAll("'", "\\\\'");
				}

				while (number < threshold) {
					String query = "select gram1, frequency, level_gram1, level_gram2, level_gram3, level_gram4,"
							+ "level_gram5 from liuliu."
							+ target_word
							+ "_5grams where gram2 = '"
							+ words[0]
							+ "' and gram3 = '"
							+ words[1]
							+ "' and gram4 = '"
							+ words[2]
							+ "' and gram5 = '"
							+ words[3]
							+ "' and frequency < "
							+ frequency
							+ " order by frequency desc limit 20";
					ResultSet rs = db.execute(query);
					int flag = 0;
					while (rs.next()) {
						flag = 1;
						frequency = rs.getLong("frequency");
						long new_frequency = ((length - 5 + 1) * count + frequency)
								/ (length - 5 + 2);

						String head = rs.getString("gram1");
						String new_phrase = head + " " + current_phrase;
						if (rr.fitlers_whetherextend(target_word, new_phrase
								.split(" "), rw, db)) {
							temp.put(new_phrase, new_frequency);
							number++;
						} else {
							continue;
						}
					
						/*TODO: add (1) start-end filter checking (disable the original start-end checker)
						 * 		     (2) extend the original candidates with start end five-grams
						 * */
						this.add_StartEnd_fivegrams(target_word, target_pos, new_phrase, 
								new_frequency, db, rr, rw, candidates, candidatelist);
						
						if (number >= threshold)
							break;
					}
					if (flag == 0) { // if we cannot find any five-grams
										// overlapping with current phrase
						break;
					}
				}

			} else {//expand to the right hand side
				for (int i = length - 4; i < length; i++) {
					words[i] = words[i].trim();
					words[i] = words[i].replaceAll("'", "\\\\'");
				}
				while (number < threshold) {
					String query = "select gram5, frequency, level_gram1, level_gram2, level_gram3, level_gram4,"
							+ "level_gram5 from liuliu."
							+ target_word
							+ "_5grams where gram1 = '"
							+ words[length - 4]
							+ "' and gram2 = '"
							+ words[length - 3]
							+ "' and gram3 = '"
							+ words[length - 2]
							+ "' and gram4 = '"
							+ words[length - 1]
							+ "' and frequency < "
							+ frequency
							+ " order by frequency desc limit 20";
					ResultSet rs = db.execute(query);
					int flag = 0;
					while (rs.next()) {
						flag = 1;
						frequency = rs.getLong("frequency");
						long new_frequency = ((length - 5 + 1) * count + frequency)
								/ (length - 5 + 2);
						String tail = rs.getString("gram5");
						String new_phrase = current_phrase + " " + tail;
						if (rr.fitlers_whetherextend(target_word, new_phrase
								.split(" "), rw, db)) {
							temp.put(new_phrase, new_frequency);
							number++;
						} else {
							continue;
						}
					
						//TODO
						this.add_StartEnd_fivegrams(target_word, target_pos, new_phrase, 
								new_frequency, db, rr, rw, candidates, candidatelist);
						if (number >= threshold)
							break;
					}
					if (flag == 0) { // if we cannot find any five-grams
										// overlapping with current phrase
						break;
					}

				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param target_word
	 * @param current_phrase: the phrase to be checked and extended
	 * @param current__frequency: frequency of current phrase
	 * @param db: database
	 * @param rr: filter
	 * @param candidates: hashtable of all the (candidates + frequency)
	 * @param candidatelist: arraylist of all the (candidates + frequency)
	 */
	public void add_StartEnd_fivegrams(String target_word, String target_pos, String current_phrase, long current_frequency, Database db, Filters rr,
			RelatedWords rw, Hashtable<String, Long> candidates,	ArrayList<String> candidatelist){
		//Check the start and end of current phrase
		
		int threshold = 5; //threshold of how many new phrases should be extended
		
		String[] words = current_phrase.split(" ");
		int length = words.length;
		for (int i = 0; i < words.length; i++) {
			words[i] = words[i].trim();
			words[i] = words[i].replaceAll("'", "\\\\'");
		}
		try{
			int complete = rr.check_StartEnd5gram(words, db);
			//check whether the sentence is a complete sentence
			if(complete==3){//complete
				current_phrase = current_phrase.replace("<S> ", "").replace(" </S>", "");
				if (rr.filters_whethercandidate_StartEnd(current_phrase + "\t"
						+ current_frequency, target_word, target_pos, db)) {
					//TODO: july 11, 10:18pm
					//current_phrase = current_phrase.replace(" !", "").replace(" ?", "").replace(".", "");
					if(!candidates.containsKey(current_phrase.toLowerCase())){
						candidatelist.add(current_phrase + "\t"	+ current_frequency);
						candidates.put(current_phrase.toLowerCase(), current_frequency);
					}else
						duplicate++;
				}
			}
			
			
			//Else, extend the phrase with start and end five-grams
			else{
				//extend with start five-grams
				ArrayList<String> addStartFiveGrams = new ArrayList<String>();
				if(complete!=1){//head is not complete
					int number_start = 0;
					long frequency = Long.MAX_VALUE;
					while (number_start < threshold) {
						String query = "select gram2, frequency from liuliu.startsymbol_all_5grams"
								+" where gram5 = '"
								+ words[2]
								+ "\r' and gram4 = '"
								+ words[1]
								+ "' and gram3 = '"
								+ words[0]
								+ "' and frequency < "
								+ frequency
								+ " order by frequency desc limit 10";
						ResultSet rs = db.execute(query);
						int flag = 0;
						while (rs.next()) {
							flag = 1;
							frequency = rs.getLong("frequency");
							long new_frequency = ((length - 5 + 1) * current_frequency + frequency)
									/ (length - 5 + 2);
		
							//String head = rs.getString("gram1")+" "+rs.getString("gram2");
							String head = rs.getString("gram2");
							String new_phrase = head + " " + current_phrase;
							if (!rr.fitlers_whetherextend(target_word, new_phrase
									.split(" "), rw, db)) {
								continue;
							} else {
								if(complete==2){//tail is complete
									new_phrase = new_phrase.replace(" </S>", "");
									if (rr.filters_whethercandidate_StartEnd(new_phrase + "\t"
											+ new_frequency, target_word, target_pos, db)) {
										if(!candidates.containsKey(new_phrase.toLowerCase())){
											candidatelist.add(new_phrase + "\t"	+ new_frequency);
											candidates.put(new_phrase.toLowerCase(), new_frequency);
											number_start++;
										}
									}
								}else{								
										addStartFiveGrams.add(new_phrase + "\t"	+ new_frequency);
										number_start++;
								}
							}
							if (number_start >= threshold)
								break;
						}
						if (flag == 0) { // if we cannot find any five-grams
							// overlapping with current phrase
							break;
						}
					}
				}
				//extend with end five-grams
				if(complete!=2){//tail is not complete
					if(complete==1)
						addStartFiveGrams.add(current_phrase+"\t"+current_frequency);
					for(int i = 0;i<addStartFiveGrams.size();i++){
						current_phrase = addStartFiveGrams.get(i).split("\t")[0];
						
						current_frequency = Long.valueOf(addStartFiveGrams.get(i).split("\t")[1]);
						int number_end = 0;
						long frequency = Long.MAX_VALUE;
						while (number_end < threshold) {
							String query = "select gram4, frequency from liuliu.endsymbol_5grams"
									+" where gram1 = '"
									+ words[length-3]
									+ "' and gram2 = '"
									+ words[length-2]
									+ "' and gram3 = '"
									+ words[length-1]
									+ "' and frequency < "
									+ frequency
									+ " order by frequency desc limit 10";
							ResultSet rs = db.execute(query);
							int flag = 0;
							while (rs.next()) {
								flag = 1;
								frequency = rs.getLong("frequency");
								long new_frequency = ((length - 5 + 1) * current_frequency + frequency)
										/ (length - 5 + 2);
			
								String tail = rs.getString("gram4");
								String last_word = tail;
								if(!last_word.matches("[a-zA-Z0-9!?.]+"))
									continue;
								String new_phrase = current_phrase+" "+tail;
								if (!rr.fitlers_whetherextend(target_word, new_phrase.split(" "), rw, db)) {
									continue;
								} else {
									if (rr.filters_whethercandidate_StartEnd(new_phrase + "\t"
											+ new_frequency, target_word, target_pos, db)) {
										new_phrase = new_phrase.replace("<S>", "");
										if(!candidates.containsKey(new_phrase.toLowerCase())){
											candidatelist.add(new_phrase + "\t"	+ new_frequency);
											candidates.put(new_phrase.toLowerCase(), new_frequency);
											number_end++;
										}else
											duplicate++;
									}
								}
								if (number_end >= threshold)
									break;
							}
							if (flag == 0) { // if we cannot find any five-grams
								// overlapping with current phrase
								break;
							}
						}	
					}
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	public void expand_count_filters(int level, int threshold, RelatedWords rw,
			Filters rr, Hashtable<String, Long> temp, long count,
			String target_word, String current_phrase, Database db,
			Hashtable<String, Long> candidates,
			ArrayList<String> candidatelist, boolean left) {
		// ArrayList<String> newphrases = new ArrayList<String>();

		String[] words = current_phrase.split(" ");
		int length = words.length;
		long frequency = Long.MAX_VALUE;
		int number = 0;
		try {
			if (left) { // expand to the left side
				for (int i = 0; i < 4; i++) {
					words[i] = words[i].trim();
					words[i] = words[i].replaceAll("'", "\\\\'");
				}

				while (number < threshold) {
					String query = "select gram1, frequency, level_gram1, level_gram2, level_gram3, level_gram4,"
							+ "level_gram5 from liuliu."
							+ target_word
							+ "_5grams where gram2 = '"
							+ words[0]
							+ "' and gram3 = '"
							+ words[1]
							+ "' and gram4 = '"
							+ words[2]
							+ "' and gram5 = '"
							+ words[3]
							+ "' and frequency < "
							+ frequency
							+ " order by frequency desc limit 20";
					ResultSet rs = db.execute(query);
					int flag = 0;
					while (rs.next()) {
						flag = 1;
						frequency = rs.getLong("frequency");
						long new_frequency = ((length - 5 + 1) * count + frequency)
								/ (length - 5 + 2);

						nodes[level - 5]++;
						String head = rs.getString("gram1");
						String new_phrase = head + " " + current_phrase;
						if (rr.fitlers_whetherextend_count_filters(MNF, MSF,
								level, target_word, new_phrase.split(" "), rw,
								db)) {
							temp.put(new_phrase, new_frequency);
							number++;
						} else {
							continue;
						}
						if (true) {
							if (rr.filters_whethercandidate_count_filters(HF,
									level, new_phrase + "\t" + new_frequency,
									db)) {
								if (new_phrase.substring(0, 4).equals("and ")
										|| new_phrase.substring(0, 4).equals(
												"but ")) {
									new_phrase = new_phrase.substring(4,
											new_phrase.length());
								}

								if (!candidates.containsKey(new_phrase)) {
									candidatelist.add(new_phrase + "\t"
											+ new_frequency);
									candidates.put(new_phrase, new_frequency);
								} else {
									nodes[level - 5]--;
								}
							}
						}
						if (number >= threshold)
							break;
					}
					if (flag == 0) { // if we cannot find any five-grams
										// overlapping with current phrase
						break;
					}
				}

			} else {
				for (int i = length - 4; i < length; i++) {
					words[i] = words[i].trim();
					words[i] = words[i].replaceAll("'", "\\\\'");
				}
				while (number < threshold) {
					String query = "select gram5, frequency, level_gram1, level_gram2, level_gram3, level_gram4,"
							+ "level_gram5 from liuliu."
							+ target_word
							+ "_5grams where gram1 = '"
							+ words[length - 4]
							+ "' and gram2 = '"
							+ words[length - 3]
							+ "' and gram3 = '"
							+ words[length - 2]
							+ "' and gram4 = '"
							+ words[length - 1]
							+ "' and frequency < "
							+ frequency
							+ " order by frequency desc limit 20";
					ResultSet rs = db.execute(query);
					int flag = 0;
					while (rs.next()) {
						flag = 1;
						frequency = rs.getLong("frequency");
						long new_frequency = ((length - 5 + 1) * count + frequency)
								/ (length - 5 + 2);
						String tail = rs.getString("gram5");
						String new_phrase = current_phrase + " " + tail;
						nodes[level - 5]++;
						if (rr.fitlers_whetherextend_count_filters(MNF, MSF,
								level, target_word, new_phrase.split(" "), rw,
								db)) {
							temp.put(new_phrase, new_frequency);
							number++;
						} else {
							continue;
						}

						// if(parsePhrase(db, word_level,
						// new_phrase,lgdictionary, lgparseoptions)){
						// candidates.put(new_phrase, new_frequency);
						// }
						if (true) {
							if (rr.filters_whethercandidate_count_filters(HF,
									level, new_phrase + "\t" + new_frequency,
									db)) {
								if (new_phrase.substring(0, 4).equals("and ")
										|| new_phrase.substring(0, 4).equals(
												"but ")) {
									new_phrase = new_phrase.substring(4,
											new_phrase.length());
								}
								if (!candidates.containsKey(new_phrase)) {
									candidatelist.add(new_phrase + "\t"
											+ new_frequency);
									candidates.put(new_phrase, new_frequency);
								} else {
									nodes[level - 5]--;
								}
							}
						}
						if (number >= threshold)
							break;
					}
					if (flag == 0) { // if we cannot find any five-grams
										// overlapping with current phrase
						break;
					}

				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/*
	public static void writeText(String filename, Hashtable candidates) {
		try {
			FileWriter fw = new FileWriter(
					"D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\CONTEXT_GENERATION\\forevaluate\\"
							+ filename + "_related.txt", false);
			Map.Entry[] entries = SortHashtableByValue.sort(candidates);
			for (int i = 0; i < entries.length; i++) {
				fw.write(entries[i].getKey() + "\t" + entries[i].getValue()
						+ "\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
*/
	
	public static void writeText(String filename, ArrayList<String> candidates, String suffix) {
		try {
			FileWriter fw = new FileWriter(".\\result\\"+ filename + "_"+suffix+".txt", true);
			for (int i = 0; i < candidates.size(); i++) {
				fw.write(candidates.get(i) + "\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getCandidates(int startnum, RelatedWords rw, Filters rr,
			HashSet<String> relatedwords, String target_word, String target_pos, 
			Database db) {
		
		Hashtable<String, Long> candidates = new Hashtable<String, Long>();
		ArrayList<String> candidatelist = new ArrayList<String>();
		//ArrayList<String> five_grams = chooseStart5gram(startnum, 4, rr, rw,target_word, db, relatedwords);
		ArrayList<String> five_grams = chooseStart5gram_speedUp(startnum, 4, rr, rw,target_word, db, relatedwords);
		
		// to do: write these five_grams
		int recursion = five_grams.size();
		// System.out.println(start_5grams);
		for (int r = 0; r < recursion; r++) {
			String start_5gram = five_grams.get(r).split("\t")[0];

			//TODO: add start-end checker and extender
			if (rr.filters_whethercandidate(five_grams.get(r), target_word, target_pos, db)) {
				String input = five_grams.get(r);
				if (start_5gram.substring(0, 4).equals("and ")
						|| start_5gram.substring(0, 4).equals("but ")) {

					candidatelist.add(input.substring(4, input.length()));
				} else {
					candidatelist.add(input);
				}
			}
			
			long frequency = Long.valueOf(five_grams.get(r).split("\t")[1]);
			String[] grams = start_5gram.split(" ");
			int position = 0;
			// find the position of the target word in this five gram
			for (int i = 0; i < 5; i++) {
				if (grams[i].equalsIgnoreCase(target_word)) {
					position = i;
					break;
				}
			}
			String current_phrase = start_5gram;

			// ArrayList<String> current_phrases = new ArrayList<String>();
			Hashtable<String, Long> current_phrases = new Hashtable<String, Long>();
			current_phrases.put(current_phrase, frequency);
			for (int i = 0; i <= 4 - position; i++) { // 4-position is the times
														// that the phrase can
														// extend to the left
														// side
				// for example, if position is 4, it means target word is the
				// last word in the 5 gram, so the phrase can only extend to the
				// right side
				// ArrayList<String> left_phrases = new ArrayList<String>();
				Hashtable<String, Long> left_phrases = new Hashtable<String, Long>();
				if (i != 0) { // i!=0 means
					int phrase_number = current_phrases.size();
					// ArrayList<String> temp = new ArrayList<String>();
					Hashtable<String, Long> temp = new Hashtable<String, Long>();
					for (Enumeration e = current_phrases.keys(); e
							.hasMoreElements();) {
						current_phrase = (String) e.nextElement();
						long count = current_phrases.get(current_phrase);
						// current_phrase = current_phrases.get(k);
						this.expand(startnum / 5, rw, rr, temp, count,
								target_word, target_pos, current_phrase, db, candidates,
								candidatelist, true);

					}
					current_phrases.clear();
					current_phrases = temp;

				}
				left_phrases = current_phrases;
				for (int j = 0; j < position; j++) { // position is the times
														// that the phrase can
														// expand to the right
														// side
					// ArrayList<String> temp = new ArrayList<String>();
					int phrase_number = current_phrases.size();
					// for(int k = 0;k<phrase_number;k++){
					// current_phrase = current_phrases.get(k);
					// temp.addAll(this.expand(target_word, current_phrase, db,
					// candidates, false, lgdictionary, lgparseoptions));
					// }
					Hashtable<String, Long> temp = new Hashtable<String, Long>();
					for (Enumeration e = current_phrases.keys(); e
							.hasMoreElements();) {
						current_phrase = (String) e.nextElement();
						long count = current_phrases.get(current_phrase);
						// current_phrase = current_phrases.get(k);
						this.expand(startnum / 5, rw, rr, temp, count,
								target_word, target_pos, current_phrase, db, candidates,
								candidatelist, false);

					}
					current_phrases.clear();
					current_phrases = temp;
				}
				current_phrases = left_phrases;
			}
		}
		//check NER using NER callable
		
		writeText(target_word, candidatelist, "candidate");
	}

	/**
	 * @param startnum
	 * @param rw
	 * @param rr
	 * @param relatedwords
	 * @param target_word
	 * @param target_pos
	 * @param db
	 * @author liuliu
	 * @version 1.0
	 * @since 2009/06/03
	 */
	public void getCandidates_withStartEnd(int startnum, RelatedWords rw, Filters rr,
			HashSet<String> relatedwords, String target_word, String target_pos, 
			Database db, boolean start_trigram) {
		
		Hashtable<String, Long> candidates = new Hashtable<String, Long>();
		ArrayList<String> candidatelist = new ArrayList<String>();
		//ArrayList<String> five_grams = chooseStart5gram(startnum, 4, rr, rw,target_word, db, relatedwords);
		
		ArrayList<String> five_grams = new ArrayList<String>();
		if(!start_trigram)
			five_grams = chooseStart5gram_speedUp(startnum, 4, rr, rw,target_word, db, relatedwords);
		else
			five_grams = chooseStart5gramfrom3gram_speedUp(startnum, 4, rr, rw,target_word, db, relatedwords);
		
		// to do: write these five_grams
		int recursion = five_grams.size();
		//System.out.println("Start five-grams: "+recursion);
		// System.out.println(start_5grams);
		for (int r = 0; r < recursion; r++) {
			//System.out.println(r);
			String start_5gram = five_grams.get(r).split("\t")[0];

			//TODO: add start-end checker and extender
			long frequency = Long.valueOf(five_grams.get(r).split("\t")[1]);
			
			this.add_StartEnd_fivegrams(target_word, target_pos, start_5gram, 
					frequency, db, rr, rw, candidates, candidatelist);
			
			
			String[] grams = start_5gram.split(" ");
			int position = 0;
			// find the position of the target word in this five gram
			for (int i = 0; i < 5; i++) {
				if (grams[i].equalsIgnoreCase(target_word)) {
					position = i;
					break;
				}
			}
			String current_phrase = start_5gram;

			// ArrayList<String> current_phrases = new ArrayList<String>();
			Hashtable<String, Long> current_phrases = new Hashtable<String, Long>();
			current_phrases.put(current_phrase, frequency);
			for (int i = 0; i <= 4 - position; i++) { // 4-position is the times
														// that the phrase can
														// extend to the left
														// side
				// for example, if position is 4, it means target word is the
				// last word in the 5 gram, so the phrase can only extend to the
				// right side
				// ArrayList<String> left_phrases = new ArrayList<String>();
				Hashtable<String, Long> left_phrases = new Hashtable<String, Long>();
				if (i != 0) { // i!=0 means
					int phrase_number = current_phrases.size();
					// ArrayList<String> temp = new ArrayList<String>();
					Hashtable<String, Long> temp = new Hashtable<String, Long>();
					for (Enumeration e = current_phrases.keys(); e
							.hasMoreElements();) {
						current_phrase = (String) e.nextElement();
						long count = current_phrases.get(current_phrase);
						// current_phrase = current_phrases.get(k);
						this.expand_withStartEnd(startnum / 10, rw, rr, temp, count,
								target_word, target_pos, current_phrase, db, candidates,
								candidatelist, true);

					}
					current_phrases.clear();
					current_phrases = temp;

				}
				left_phrases = current_phrases;
				for (int j = 0; j < position; j++) { // position is the times
														// that the phrase can
														// expand to the right
														// side
					// ArrayList<String> temp = new ArrayList<String>();
					int phrase_number = current_phrases.size();
					// for(int k = 0;k<phrase_number;k++){
					// current_phrase = current_phrases.get(k);
					// temp.addAll(this.expand(target_word, current_phrase, db,
					// candidates, false, lgdictionary, lgparseoptions));
					// }
					Hashtable<String, Long> temp = new Hashtable<String, Long>();
					for (Enumeration e = current_phrases.keys(); e
							.hasMoreElements();) {
						current_phrase = (String) e.nextElement();
						long count = current_phrases.get(current_phrase);
						// current_phrase = current_phrases.get(k);
						this.expand_withStartEnd(startnum / 10, rw, rr, temp, count,
								target_word, target_pos, current_phrase, db, candidates,
								candidatelist, false);

					}
					current_phrases.clear();
					current_phrases = temp;
				}
				current_phrases = left_phrases;
			}
		}
		
		System.out.println("hashtable size: "+candidates.size());
		System.out.println("number of duplications: "+duplicate);
		//check NER here
		ArrayList<String> nerResults = new ArrayList<String>();
		ArrayList<String> sentences_toner = new ArrayList<String>();
		String suffix = getDateTime();
		if(start_trigram)
			suffix = "3_"+suffix;
		else
			suffix = "5_"+suffix;
		for(int i = 0;i<candidatelist.size();i++){
			sentences_toner.add(candidatelist.get(i));
			if(i%100000==0||i==candidatelist.size()-1){
				//ner
			nerResults = rr.checkNERinBatch(sentences_toner);
			writeText(target_word, nerResults, suffix);
			sentences_toner.clear();
			nerResults.clear();
			}
		}
		
	}
	
	public void getCandidates_count_filters(int startnum, RelatedWords rw,
			Filters rr, HashSet<String> relatedwords, String target_word,
			Database db) {
		Hashtable<String, Long> candidates = new Hashtable<String, Long>();
		ArrayList<String> candidatelist = new ArrayList<String>();
		ArrayList<String> five_grams = chooseStart5gram_count_filters(startnum,
				4, rr, rw, target_word, db, relatedwords);

		// to do: write these five_grams
		int recursion = five_grams.size();
		// System.out.println(start_5grams);
		for (int r = 0; r < recursion; r++) {
			String start_5gram = five_grams.get(r).split("\t")[0];

			// -----------------------------------------------------------------------------------------------
			// In the first version which is commented in /**/, it is wrong.
			// five-grams which are not candidates should not be removed
			/*
			 * if(!rr.filters_whethercandidate_count_filters(HF, 5,
			 * five_grams.get(r), db)) continue; String input =
			 * five_grams.get(r);
			 * if(start_5gram.substring(0,4).equals("and ")||start_5gram
			 * .substring(0,4).equals("but ")){
			 * 
			 * candidatelist.add(input.substring(4, input.length())); }else{
			 * candidatelist.add(input); }
			 */
			if (rr.filters_whethercandidate_count_filters(HF, 5, five_grams
					.get(r), db)) {
				String input = five_grams.get(r);
				if (start_5gram.substring(0, 4).equals("and ")
						|| start_5gram.substring(0, 4).equals("but ")) {

					candidatelist.add(input.substring(4, input.length()));
				} else {
					candidatelist.add(input);
				}
			}
			// --------------------------------------------------------------------------------------------------
			long frequency = Long.valueOf(five_grams.get(r).split("\t")[1]);
			String[] grams = start_5gram.split(" ");
			int position = 0;
			// find the position of the target word in this five gram
			for (int i = 0; i < 5; i++) {
				if (grams[i].equalsIgnoreCase(target_word)) {
					position = i;
					break;
				}
			}
			String current_phrase = start_5gram;

			// ArrayList<String> current_phrases = new ArrayList<String>();
			Hashtable<String, Long> current_phrases = new Hashtable<String, Long>();
			current_phrases.put(current_phrase, frequency);
			for (int i = 0; i <= 4 - position; i++) { // 4-position is the times
														// that the phrase can
														// extend to the left
														// side
				// for example, if position is 4, it means target word is the
				// last word in the 5 gram, so the phrase can only extend to the
				// right side
				// ArrayList<String> left_phrases = new ArrayList<String>();
				Hashtable<String, Long> left_phrases = new Hashtable<String, Long>();
				if (i != 0) { // i!=0 means
					int phrase_number = current_phrases.size();
					// ArrayList<String> temp = new ArrayList<String>();
					Hashtable<String, Long> temp = new Hashtable<String, Long>();
					for (Enumeration e = current_phrases.keys(); e
							.hasMoreElements();) {
						current_phrase = (String) e.nextElement();
						long count = current_phrases.get(current_phrase);
						// current_phrase = current_phrases.get(k);
						this.expand_count_filters(5 + i, startnum / 5, rw, rr,
								temp, count, target_word, current_phrase, db,
								candidates, candidatelist, true);

					}
					current_phrases.clear();
					current_phrases = temp;

				}
				left_phrases = current_phrases;
				for (int j = 0; j < position; j++) { // position is the times
														// that the phrase can
														// expand to the right
														// side
					// ArrayList<String> temp = new ArrayList<String>();
					int phrase_number = current_phrases.size();
					Hashtable<String, Long> temp = new Hashtable<String, Long>();
					for (Enumeration e = current_phrases.keys(); e
							.hasMoreElements();) {
						current_phrase = (String) e.nextElement();
						long count = current_phrases.get(current_phrase);
						// current_phrase = current_phrases.get(k);
						this.expand_count_filters(5 + i + j + 1, startnum / 5,
								rw, rr, temp, count, target_word,
								current_phrase, db, candidates, candidatelist,
								false);

					}
					current_phrases.clear();
					current_phrases = temp;
				}
				current_phrases = left_phrases;
			}
		}
		// writeText(target_word, candidatelist);
		/*
		 * for(int j = 0;j<5;j++){ System.out.print(nodes[j]+"\t"); }
		 * System.out.println(); //MNF for(int j = 0;j<5;j++){ for(int i =
		 * 0;i<5;i++){ System.out.print(MNF[i][j]+"\t"); } System.out.println();
		 * } //MSF for(int j = 0;j<2;j++){ for(int i = 0;i<5;i++){
		 * System.out.print(MSF[i][j]+"\t"); } System.out.println(); } //HF
		 * for(int j = 0;j<3;j++){ for(int i = 0;i<5;i++){
		 * System.out.print(HF[i][j]+"\t"); } System.out.println(); }
		 * System.out.println(CF); System.out.println(); // // for(int i =
		 * 0;i<5;i++){ // System.out.print(i+5); // for(int j = 0;j<5;j++){ //
		 * System.out.print("\t"+MNF[i][j]); // } // for(int j = 0;j<2;j++){ //
		 * System.out.print("\t"+MSF[i][j]); // } // for(int j = 0;j<3;j++){ //
		 * System.out.print("\t"+HF[i][j]); // } //
		 * System.out.println("\t"+nodes[i]); // }
		 */
	}

	public static void readTop100RelatedWordList(String target_word,
			HashSet<String> relatedwords, Database db) {
		try {
			String query = "select related_word_base from liuliu."
					+ target_word
					+ "_relatedwords where webby = 0 and function_word = 0"
					+ " and in_alphabetOrder_list = 0 order by new_measure desc limit 100";
			ResultSet rs = db.execute(query);
			while (rs.next()) {
				String related_word = rs.getString("related_word_base");
				relatedwords.add(related_word);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TimeStat ts = new TimeStat();
		ts.start();
		Database db = new Database();
		Generator eg = new Generator();

		RelatedWords rw = new RelatedWords();
		Filters rr = new Filters();
		rr.initialRemoveString("D:\\LiuLiu\\Listen\\Research Resource\\Punctuations\\punctuations.txt");
		rr.initialFunctionWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\functionWords.txt", db);
		rr.initialWebWords("D:\\LiuLiu\\Listen\\Research Resource\\Google5gram\\webSpecific\\webspecific.txt", db);
		rr.initialspecialPunctuations();
		rr.initialtabooWords();
		rr.initialrelativePronouns();
		rr.initialSpecialVerbs();

		ArrayList<String> keywords_list = new ArrayList<String>();
		String[] words = "anxious, courage, declare, extinct, merchant, remarkable, slender, stout, suspicious, tremendous"
				.split(", ");

		for (int i = 0; i < words.length; i++)
			keywords_list.add(words[i]);

		// step 1: generate candidate contexts: using the 10 filters 
		// and grammar checker, pos tagger and named entity recognizer
//		for (int i = 0; i < 1; i++) {
//			String target_word = keywords_list.get(i);
//			HashSet<String> relatedwords = new HashSet<String>();
//			readTop100RelatedWordList(target_word, relatedwords, db);
//			System.out.println(target_word);
//			eg.getCandidates(100, rw, rr, relatedwords, target_word, "adj", db);
//			System.out.println(ts.getElapsedTimeSeconds());
//			ts.start();
//			eg.clearArray();
//		}
		
		// step 5: cluser and find good examples
		ts.start();
		for (int i = 0; i < 1; i++) {
			String target_word = keywords_list.get(i);
			System.out.println(target_word);
			// Step 1: cluster
			String candidate_file = ".\\result\\"+ target_word + "_candidate.txt";
			String cluster_file = ".\\result\\"+ target_word + "_cluster_oldway.txt";
			//rr.cluster_without_functionwords(candidate_file,cluster_file, 3, db);
			//rr.cluster(candidate_file,cluster_file, 4, db);
			rr.cluster_content_words(candidate_file,cluster_file, 3, db);
			rr.scs.clear();
			
			// step 2: score sentence and find good examples
//			String score_file = ".\\result\\"+ target_word + "_score.txt";
//			String example_file = ".\\result\\"+ target_word + "_example.txt";
//			rr.findGoodExample(
//							db,
//							target_word,
//							cluster_file,
//							score_file,
//							example_file);
			System.out.println(ts.getElapsedTimeSeconds());
			ts.start();
		}

	}

}
