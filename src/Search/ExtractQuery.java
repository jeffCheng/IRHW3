package Search;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import Classes.Path;
import Classes.Query;
import Classes.Stemmer;

public class ExtractQuery {
	
	ArrayList<String>  topicId = new ArrayList<String>();
	ArrayList<String> titleInfo = new ArrayList<String>();
	int quertyIndex = 0;

	public ExtractQuery() throws IOException {
		//you should extract the 4 queries from the Path.TopicDir
		//NT: the query content of each topic should be 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming.
		//NT: you can simply pick up title only for query, or you can also use title + description + narrative for the query content.
		
		// get stopword list
		HashSet<String> stopWordSet = new HashSet<String>();
    	FileInputStream stopwordfile = new FileInputStream(Path.StopwordDir); 
    	BufferedReader reader1= new BufferedReader(new InputStreamReader(stopwordfile));    
		String line1 = reader1.readLine();
		while (line1 != null){
			stopWordSet.add(line1); // add to hashset
			line1 = reader1.readLine(); // go to next line
		}
		reader1.close();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Path.TopicDir))); 
		String line = reader.readLine();
		while(line!= null){
			//get Num
			if(line.indexOf( "<num>" ) != -1){
				//System.out.println(line);
				String queryid = line.substring(14);
				//System.out.println(queryid); // query ID
				topicId.add(queryid);
			}
			
			if( line.indexOf( "<title>" )!= -1){
				
				String content = line.substring(7); // start after "<title>"
				//tokenized
				String[] tokenizerWord = content.replaceAll("[\\pP‘’“”]", "").split(" "); // split word
				String resultWords ="";
				
				for(String word:tokenizerWord){
					//lowercase
					word = word.toLowerCase();
					//remove stop words
					if(!stopWordSet.contains(word)){
						// stemming
						String str="";
						char[] charArray = word.toCharArray(); // change String to Char[] 
						Stemmer stemming = new Stemmer();
						stemming.add(charArray, charArray.length); // call add() in stemmer
						stemming.stem(); // call stem() to do stemming 
						str = stemming.toString(); // change back to String 
						resultWords = resultWords+ " "+str; // sum of results
					}
					
				}
				//System.out.println(resultWords);
				titleInfo.add(resultWords);
			}
			line = reader.readLine();
		}
		reader.close();
	}
	
	public boolean hasNext()
	{
		if(quertyIndex<topicId.size()){
			quertyIndex++;
			return true;
		}
		return false;
	}
	
	public Query next()
	{
		Query query = new Query();
		query.SetQueryContent(titleInfo.get(quertyIndex-1)); // q-1 because q++ before here
		query.SetTopicId(topicId.get(quertyIndex-1));
		return query;
	}
}
