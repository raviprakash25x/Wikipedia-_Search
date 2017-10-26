package wiki_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.tartarus.snowball.ext.englishStemmer;

public class Search {

	public void searchQuery(String query) {
		ArrayList<String> tokens = pruneQuery(query);
		ArrayList <List <String>> allResults = new ArrayList<List<String>>();

		for(int i=0; i<tokens.size(); i++) {
			String result = searchToken(Globals.maxLevel, tokens.get(i), 1);
			//System.out.println(result);

			if(result != null) {
				allResults.add(Arrays.asList(result.split("\\$")));
			}
		}

		if(allResults.size() == 0) {
			System.out.println("Search query returned 0 results!");
		}
		else {
			processResults(allResults);
		}
	}

	private void processResults(ArrayList<List<String>> allResults) {

		Map <String, Float> processedResults = new HashMap<String, Float>();

		for(int i=0; i<allResults.size(); i++) {

			for(int j=0; j<allResults.get(i).size(); j++) {
				String tokens[] = allResults.get(i).get(j).split(",");
				String docId = tokens[0];
				float tf_idf = Float.parseFloat(tokens[1]);

				if(processedResults.containsKey(docId)) {
					processedResults.put(docId, processedResults.get(docId)+tf_idf);
				}
				else {
					processedResults.put(docId, tf_idf);
				}
			}
		}

		Iterator<Entry<String, Float>> it = processedResults.entrySet().iterator();
		TreeMap <Float, ArrayList <String> > sortedMap = 
				new TreeMap<Float, ArrayList<String>>(Collections.reverseOrder());

		while(it.hasNext()) {
			Map.Entry<String, Float> pair = it.next();

			if(sortedMap.containsKey(pair.getValue())) {
				sortedMap.get(pair.getValue()).add(pair.getKey());
			}
			else {
				ArrayList <String> temp = new ArrayList<String>();
				temp.add(pair.getKey());
				sortedMap.put(pair.getValue(), temp);
			}

		}

		displayResults(sortedMap);
	}

	private void displayResults(TreeMap<Float, ArrayList<String>> sortedMap) {
		Iterator<Entry<Float, ArrayList <String> >> it = sortedMap.entrySet().iterator();
		int count = 0;

		while(count < 10 && it.hasNext()) {
			Map.Entry<Float, ArrayList <String> > pair = it.next();

			for(int i=0; i<pair.getValue().size() && count < 10; i++) {
				System.out.println(++count+ ". "+ pair.getValue().get(i));
			}
		}

		System.out.println();
	}

	private String searchToken(char currLevel, String key, int fileCount) {
		if(currLevel == Globals.topLevel) {
			return fetchResult(key, fileCount);
		}

		BufferedReader br;

		try {
			File file = new File(Globals.outFilePath+currLevel+fileCount);
			br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			int count = 1;

			while(line != null) {

				if(key.compareTo(line) <= 0) {
					count = ((fileCount-1)*Globals.levelLimit) + count;
					String result = searchToken(--currLevel, key, count);
					br.close();
					return result;
				}
				count++;
				line = br.readLine();
			}

			br.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private String fetchResult(String key, int fileCount) {
		try {
			File finalFile = new File(Globals.outFilePath+Globals.topLevel+fileCount);
			BufferedReader br = new BufferedReader(new FileReader(finalFile));
			String line = br.readLine();

			while(line != null) {

				if(key.compareTo(line) <= 0) {
					String tokens[] = line.split(":");

					if(tokens[0].equals(key)) {
						br.close();
						return tokens[1];
					}
					else
						break;
				}
				line = br.readLine();
			}
			br.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private ArrayList<String> pruneQuery(String query) {
		query = query.toLowerCase();
		query = query.replaceAll("[^a-zA-Z0-9 ]", " ");//spaces not removed
		query = query.trim();
		String tokens[] = query.split(" ");
		ArrayList <String> prunedTokens = new ArrayList<String>();
		englishStemmer stemmer = new englishStemmer();

		for(int i=0; i<tokens.length; i++) {

			if(!Globals.stopwords.contains(tokens[i])) {
				stemmer.setCurrent(tokens[i]);
				stemmer.stem();
				prunedTokens.add(stemmer.getCurrent());
			}
		}

		return prunedTokens;
	}

}
