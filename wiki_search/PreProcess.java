package wiki_search;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.tartarus.snowball.ext.englishStemmer;

public class PreProcess {
	static int count = 0;
	Map <String, Integer> map;
	Map <String, Integer> titleMap;
	static TreeMap <String, ArrayList<Integer>> treeMap = new TreeMap<String, ArrayList<Integer>>();
	//treemap contains key: id,countBody, countTitle, id,countBody, countTitle,....

	int processPage(String id, String title, String body, boolean isLast) {
		if(isLast) {
			writeToFile();
			treeMap.clear();
			return 0;
		}
		body = cleanBody(body);
		tokenizeBody(body);
		tokenizeTitle(title);
		addToTreeMap(id);
		count++;

		if((count > 0 && count % 100 == 0)) {
			writeToFile();
			treeMap.clear();
		}

		return 0;
	}

	private void writeToFile() {
		String path = Globals.outFilePath + new Double(Math.ceil((count*1.0)/100)).intValue();
		Iterator<Entry<String, ArrayList<Integer>>> it = treeMap.entrySet().iterator();
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");

			while(it.hasNext()) {
				Map.Entry<String, ArrayList<Integer>> pair = it.next();
				String key = pair.getKey();
				ArrayList <Integer> value = pair.getValue();
				StringBuilder toWrite= new StringBuilder(key+":");

				for(int i=0; i<value.size(); i+=3) {
					toWrite.append(value.get(i)+",");
					toWrite.append(value.get(i+1)+",");
					toWrite.append(value.get(i+2)+",");
				}

				//toWrite.append('$');
				writer.println(toWrite);
			}

			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		Globals.docCount = count;
	}

	private void addToTreeMap(String id) {
		Iterator<Entry<String, Integer>> it = map.entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<String, Integer> pair = it.next();
			String key = (String) pair.getKey();
			int valueBody = (int) pair.getValue();
			int valueTitle = 0;

			if(titleMap.containsKey(key)) {
				valueTitle = titleMap.get(key);
				titleMap.remove(key);
			}

			if(treeMap.containsKey(key)) {
				treeMap.get(key).add(Integer.parseInt(id));
				treeMap.get(key).add(valueBody);
				treeMap.get(key).add(valueTitle);
			}
			else {
				ArrayList <Integer> temp = new ArrayList<Integer>();
				temp.add(Integer.parseInt(id));
				temp.add(valueBody);
				temp.add(valueTitle);
				treeMap.put(key, temp);
			}
		}

		it = titleMap.entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<String, Integer> pair = it.next();
			String key = (String) pair.getKey();
			int valueBody = 0;
			int valueTitle = (int) pair.getValue();
			ArrayList <Integer> temp = new ArrayList<Integer>();
			temp.add(Integer.parseInt(id));
			temp.add(valueBody);
			temp.add(valueTitle);
			treeMap.put(key, temp);
		}
	}

	private void tokenizeTitle(String title) {
		//TODO do not remove content inside braces
		title = title.toLowerCase();
		title = title.replaceAll("\\{.*\\}", "");
		title = title.replaceAll("\\[.*\\]", "");
		title = title.replaceAll("\\(.*\\)", "");
		title = title.replaceAll("[^a-zA-Z0-9 ]", " ");
		titleMap = new HashMap<String, Integer>();
		StringTokenizer tokenizer = new StringTokenizer(title);
		englishStemmer stemmer = new englishStemmer();

		while(tokenizer.hasMoreElements()) {
			stemmer.setCurrent(tokenizer.nextToken());
			stemmer.stem();
			//tokens_title.add(stemmer.getCurrent());
			String curr = stemmer.getCurrent();

			if(!titleMap.containsKey(curr)) {
				titleMap.put(curr, 1);
			}
			else {
				titleMap.put(curr, titleMap.get(curr)+1);
			}
		}
	}

	private String cleanBody(String body) {
		//TODO can be improved
		body = body.toLowerCase();
		body = body.replaceAll("\\{\\{.*?\\}\\}", "");
		body = body.replaceAll("[^a-zA-Z ]", " ");//spaces not removed
		return body;
	}

	private void tokenizeBody(String body) {
		map = new HashMap<String, Integer>();
		StringTokenizer tokenizer = new StringTokenizer(body);
		englishStemmer stemmer = new englishStemmer();

		while(tokenizer.hasMoreTokens()) {
			String curr = tokenizer.nextToken();

			if(!Globals.stopwords.contains(curr) && curr.length() <= 15) {
				stemmer.setCurrent(curr);
				stemmer.stem();
				curr = stemmer.getCurrent();

				if(!map.containsKey(curr)) {
					map.put(curr, 1);
				}
				else {
					map.put(curr, map.get(curr)+1);
				}
			}
		}
	}
}
