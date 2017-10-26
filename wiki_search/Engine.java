package wiki_search;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Engine {
	int parseXML() {
		try {
			File xmlFile = new File(Globals.xml_file_path);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			UserHandler userHandler = new UserHandler();
			saxParser.parse(xmlFile, userHandler);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void mergeIndexes() {
		File files[] = new File(Globals.outFilePath).listFiles();
		int fileCount = files.length; 

		while(fileCount > 1) {
			int outCount = 1;

			for(int i=1; (i+1)<=fileCount; i+=2) {
				mergeFiles(files[i-1], files[i], outCount);
				outCount++;
			}
			files = new File(Globals.outFilePath).listFiles();
			//Arrays.sort(files);
			fileCount = files.length;
		}
	}

	/**
	 * merges the indexes of two files
	 * @param file1
	 * @param file2
	 */
	private static void mergeFiles(File file1, File file2, int outCount) {
		File outFile = new File(Globals.outFilePath+"temp");

		try {
			PrintWriter writer = new PrintWriter(outFile, "UTF-8");
			BufferedReader br1 = new BufferedReader(new FileReader(file1));
			BufferedReader br2 = new BufferedReader(new FileReader(file2));
			String line1 = br1.readLine();
			String line2 = br2.readLine();

			while(line1 != null || line2 != null) {

				if(line1 != null && line2 != null) {
					//TODO can be optimised to split only newly updated line
					String lineSplit1[] = line1.split(":");
					String lineSplit2[] = line2.split(":");
					String key1 = lineSplit1[0];
					String key2 = lineSplit2[0];
					int comp = key1.compareTo(key2);

					if(comp == 0) {
						//merge
						String merged = key1 + ":" + lineSplit1[1] + lineSplit2[1];
						writer.println(merged);
						line1 = br1.readLine();
						line2 = br2.readLine();
					}
					else if(comp < 0) {
						writer.println(line1);
						line1 = br1.readLine();
					}
					else {
						writer.println(line2);
						line2 = br2.readLine();
					}

				}
				else if(line1 == null) {
					writer.println(line2);
					line2 = br2.readLine();
				}
				else {
					writer.println(line1);
					line1 = br1.readLine();
				}
			}

			br1.close();
			br2.close();
			writer.close();
			file1.delete();
			file2.delete();
			outFile.renameTo(file1);
			//delete old files and rename temp, close writer
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void createLevels() {
		char currLevel = Globals.topLevel;
		List<File> files = divideFiles(currLevel++);
		createOneLevel(currLevel, files);
	}

	private List<File> divideFiles(char currLevel) {
		File files[] = new File(Globals.outFilePath).listFiles();
		ArrayList<File> filesForNextLevel = new ArrayList<File>();

		try {
			//TODO this original file can be deleted as of no use after division
			BufferedReader br = new BufferedReader(new FileReader(files[0].getAbsolutePath()));
			String line = br.readLine();
			int fileCount = 1, lineCount = 0;
			PrintWriter writer = new PrintWriter(Globals.outFilePath+currLevel+fileCount, "UTF-8");
			filesForNextLevel.add(new File(Globals.outFilePath+currLevel+fileCount));

			while(line != null) {
				String toWrite = processLineForTfIdf(line); 
				writer.println(toWrite);
				lineCount++;

				if(lineCount % Globals.levelLimit == 0) {
					writer.close();
					writer = new PrintWriter(Globals.outFilePath+currLevel+(fileCount+1), "UTF-8");
					filesForNextLevel.add(new File(Globals.outFilePath+currLevel+(fileCount+1)));
					fileCount++;
				}

				line = br.readLine();
			}
			br.close();
			
			if(writer != null)
				writer.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return filesForNextLevel;
	}

	private String processLineForTfIdf(String line) {
		// TODO append tfidf to the line
		String longTokens[] = line.split(":");
		String key = longTokens[0];
		String tokens[] = longTokens[1].split(",");
		//TODO check for last comma effect
		int totalDocOccurence = tokens.length/3;
		
		TreeMap <Float, ArrayList<String>> treeMap = new TreeMap<Float, ArrayList <String>>(Collections.reverseOrder());
		
		for(int i=0; i<tokens.length-1; i+=3) {
			String docId = tokens[i];
			int freqBody = Integer.parseInt(tokens[i+1]);
			int freqTitle = Integer.parseInt(tokens[i+2]);
			Float tf_idf = getTfIdf(freqBody, freqTitle, totalDocOccurence);
			
			if(treeMap.containsKey(tf_idf)) {
				treeMap.get(tf_idf).add(docId);
			}
			else {
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(docId);
				treeMap.put(tf_idf, temp);
			}
		}
		
		Iterator<Entry<Float, ArrayList<String>>> it = treeMap.entrySet().iterator();
		String toWrite = key+":";
		
		while(it.hasNext()) {
			Map.Entry<Float, ArrayList<String>> pair = it.next();
			
			for(int i=0; i<pair.getValue().size(); i++) {
				toWrite += pair.getValue().get(i) + ',' + pair.getKey()+'$';
			}
		}
		
		return toWrite;
	}

	private Float getTfIdf(int freqBody, int freqTitle, int totalDocOccurence) {
		//Title given 10x importance
		int tf = freqBody + (freqTitle*10);
		Float idf =  (float) (Math.log(Globals.docCount)/(1f+totalDocOccurence));
		return tf*idf;
	}

	private void createOneLevel(char currLevel, List<File> files) {
		if(files.size() == 1) {
			writeMaxLevel(--currLevel);
			return;
		}
		
		List <File> filesForNextLevel = new ArrayList<File>();
		int fileCount = 1, keysCount = 1, outLinesCount = 0;
		try {
			PrintWriter writer = new PrintWriter(Globals.outFilePath+currLevel+fileCount, "UTF-8");
			filesForNextLevel.add(new File(Globals.outFilePath+currLevel+fileCount));

			for(int i=0; i<files.size(); i++) {

				BufferedReader br = new BufferedReader(new FileReader(files.get(i)));
				String line = br.readLine();

				while(line != null) {
					if(keysCount % Globals.levelLimit == 0) {
						if(currLevel > 'B')
							writer.println(line);
						else {
							writer.println(line.split(":")[0]);
						}
						outLinesCount++;
					}

					if(outLinesCount == Globals.levelLimit) {
						writer.close();
						outLinesCount = 0;
						writer = new PrintWriter(Globals.outFilePath+currLevel+(fileCount+1), "UTF-8");
						filesForNextLevel.add(new File(Globals.outFilePath+currLevel+(fileCount+1)));
						fileCount++;
					}

					line = br.readLine();
					keysCount++;
				}

				br.close();
			}
			
			if(writer != null) writer.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		createOneLevel(++currLevel, filesForNextLevel);
	}

	private void writeMaxLevel(char currLevel) {
		File maxLevelFile = new File(Globals.outFilePath+"maxLevel");
		try {
			PrintWriter writer = new PrintWriter(maxLevelFile, "UTF-8");
			writer.println(currLevel);
			writer.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
