package wiki_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.tartarus.snowball.ext.englishStemmer;

public class Search {

	public void searchQuery(String query) {
		ArrayList<String> tokens = pruneQuery(query);
		ArrayList <String> allResults = new ArrayList<String>();

		for(int i=0; i<tokens.size(); i++) {
			String result = searchToken('C', tokens.get(i), 1);
			System.out.println(result);
			
			if(result != null) {
				allResults.add(result);
			}
		}
	}

	private String searchToken(char currLevel, String key, int fileCount) {
		//fileCount = ((fileCount-1)*Globals.levelLimit) + fileCount;
		
		if(currLevel == Globals.topLevel) {
			return fetchResult(key, fileCount);
		}

		//for(int i=1; i<=fileCount; i++) {
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
		//}
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
		query = query.replaceAll("[^a-zA-Z ]", " ");//spaces not removed
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
