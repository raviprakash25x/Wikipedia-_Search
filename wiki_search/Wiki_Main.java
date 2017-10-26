package wiki_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Wiki_Main {

	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Invalid args! Enter xml file path and output Folder");
			System.exit(1);
		}
		Globals.outFilePath = args[1]+"//";
		Globals.populateStopWords();

		while(true) {
			int choice = menu();
			
			switch (choice) {
			case 1:
				Globals.xml_file_path = args[0];
				generateIndex();
				break;

			case 2:
				handleSearch();
				break;
			case 0:System.exit(0);
			default: System.out.println("Invalid option!");
			}
		}
	}

	private static void handleSearch() {
		try {
			populateGlobals();
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("\nEnter Search query: ");
			String query = br.readLine();
			Search search = new Search();
			search.searchQuery(query);
		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	private static void populateGlobals() {
		File docCntFile = new File(Globals.outFilePath+"docCount");
		File maxLevelFile = new File(Globals.outFilePath+"maxLevel");
		try {
			BufferedReader br = new BufferedReader(new FileReader(docCntFile));
			String line = br.readLine();
			br.close();
			Globals.docCount = Integer.parseInt(line);
			br = new BufferedReader(new FileReader(maxLevelFile));
			line = br.readLine();
			Globals.maxLevel = line.charAt(0);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void dumpDocCount() {
		File countFile = new File(Globals.outFilePath+"docCount");
		try {
			PrintWriter writer = new PrintWriter(countFile, "UTF-8");
			writer.println(Globals.docCount);
			writer.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	private static int menu(){
		System.out.println("1. Generate Index\n2. Search\n0. Exit\n");
		int choice = 2;

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Enter your choice: ");
			choice = Integer.parseInt(br.readLine());
		}catch(Exception e) {
			System.out.println("Incorrect input!");
			menu();
		}

		return choice;
	}

	private static void generateIndex() {
		Engine engine = new Engine();
		engine.parseXML();
		engine.mergeIndexes();
		engine.createLevels();
		dumpDocCount();
	}
}
