package PseudoRFSearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;

public class PseudoRFRetrievalModel {

	MyIndexReader ixreader;
	int collectionLength = 0;
	
	public PseudoRFRetrievalModel(MyIndexReader ixreader)throws IOException 
	{
		this.ixreader=ixreader;
		for ( int i = 0; i < ixreader.getIndexTotalCount(); i++ ) {// indexReader.getDocNum()= 503473
			collectionLength += ixreader.docLength(i); // get cLength
        }
	}
	
	/**
	 * Search for the topic with pseudo relevance feedback in 2017 spring assignment 4. 
	 * The returned results (retrieved documents) should be ranked by the score (from the most relevant to the least).
	 * 
	 * @param aQuery The query to be searched for.
	 * @param TopN The maximum number of returned document
	 * @param TopK The count of feedback documents
	 * @param alpha parameter of relevance feedback model
	 * @return TopN most relevant document, in List structure
	 */
	public List<Document> RetrieveQuery( Query aQuery, int TopN, int TopK, double alpha) throws Exception {	
		// this method will return the retrieval result of the given Query, and this result is enhanced with pseudo relevance feedback
		// (1) you should first use the original retrieval model to get TopK documents, which will be regarded as feedback documents
		// (2) implement GetTokenRFScore to get each query token's P(token|feedback model) in feedback documents
		// (3) implement the relevance feedback model for each token: combine the each query token's original retrieval score P(token|document) with its score in feedback documents P(token|feedback model)
		// (4) for each document, use the query likelihood language model to get the whole query's new score, P(Q|document)=P(token_1|document')*P(token_2|document')*...*P(token_n|document')
		
		//probability P(qi | D) and P(qi | feedback documents)
		//1-α is used as the coefficient for P(qi | feedback documents);  
		//get P(token|feedback documents)
		HashMap<String,Double> TokenRFScore=GetTokenRFScore(aQuery,TopK);
		
		
		// sort all retrieved documents from most relevant to least, and return TopN
		//List<Document> results = new ArrayList<Document>();
		//return results;
		String query = aQuery.GetQueryContent();
		String[] token = query.split(" ");
		double [] collectionFreqs = new double[token.length]; 
		int [][] postingList;
		
		// docId, HashMap<Term, Times>
		Map<Integer, HashMap<String, Integer>> docx = new HashMap<Integer, HashMap<String, Integer>>(); 

		for (int i=0;i<token.length;i++ ) {
			collectionFreqs[i] = (double)  ixreader.CollectionFreq(token[i]);
			postingList = ixreader.getPostingList(token[i]); // postinglist
			
			if(postingList != null){
				for(int j = 0; j < postingList.length; j++){
					int docId = postingList[j][0];
					if(docx.containsKey(docId)){
						docx.get(docId).put(token[i], postingList[j][1]);
					}else{
						HashMap<String, Integer> termFreqs = new HashMap<String, Integer>();
						termFreqs.put(token[i], postingList[j][1]); // inside hashmap
						docx.put(docId, termFreqs); // double hashmap
					}
				}
			}
		}
		
		long mu = 2000;
		List<Document> documents = new ArrayList<>();
		
		for ( int i = 0; i < ixreader.getIndexTotalCount(); i++ ) { 
			int docLength = ixreader.docLength(i);
			double score = 1; // based on 1
			for(int k=0;k<token.length;k++){
				int collectionWordsDocFreq = 0;
				double collectionFreq = collectionFreqs[k];
				if ( collectionFreq == 0 ){
                	continue; // token not in index
                }
				if(docx.containsKey(i) && docx.get(i).containsKey(token[k])){ // prevent not found.
					collectionWordsDocFreq = docx.get(i).get(token[k]);
                }
				score = score * ( ( alpha * ( ( collectionWordsDocFreq + mu*( collectionFreq/collectionLength ) ) / ( docLength + mu )) )+ ( (1-alpha) * TokenRFScore.get(token[k]))); 
			}
			//store documents score
			//System.out.println(score);
            Document aDocument = new Document(Integer.toString(i), ixreader.getDocno(i), score); 
			documents.add(aDocument);
		}
		
		// compare each array list and reverse array list
        Collections.sort(documents, Collections.reverseOrder(new Comparator<Document>(){
        	@Override
        	public int compare(Document d1, Document d2) {
        		return new Double(d1.score()).compareTo(new Double(d2.score()));
        	}   
        	}));
        
		List<Document> documents1 = new ArrayList<>();
        // only get Top N result 
        for (int t=0;t<TopN;t++){
        	documents1.add(documents.get(t));
        }
        return documents1;
	}
	
	public HashMap<String,Double> GetTokenRFScore(Query aQuery,  int TopK) throws Exception
	{
		// for each token in the query, you should calculate token's score in feedback documents: P(token|feedback documents)
		// use Dirichlet smoothing
		// save <token, score> in HashMap TokenRFScore, and return it
		HashMap<String,Double> tokenRFScore=new HashMap<String,Double>();
		
		
		String query = aQuery.GetQueryContent();
		String[] token = query.split(" ");
		double [] collectionFreqs = new double[token.length]; 
		int [][] postingList;
		
		// docId, HashMap<Term, Times>
		Map<Integer, HashMap<String, Integer>> docx = new HashMap<Integer, HashMap<String, Integer>>(); 

		for (int i=0;i<token.length;i++ ) {
			collectionFreqs[i] = (double)  ixreader.CollectionFreq(token[i]);
			postingList = ixreader.getPostingList(token[i]); // postinglist
			
			if(postingList != null){
				for(int j = 0; j < postingList.length; j++){
					int docId = postingList[j][0];
					if(docx.containsKey(docId)){
						docx.get(docId).put(token[i], postingList[j][1]);
					}else{
						HashMap<String, Integer> termFreqs = new HashMap<String, Integer>();
						termFreqs.put(token[i], postingList[j][1]); // inside hashmap
						docx.put(docId, termFreqs); // double hashmap
					}
				}
			}
		}
		
		long mu = 2000;
		List<Document> documents = new ArrayList<>();
		
		for ( int i = 0; i < ixreader.getIndexTotalCount(); i++ ) { 
			int docLength = ixreader.docLength(i);
			double score = 1; // based on 1
			for(int k=0;k<token.length;k++){
				int collectionWordsDocFreq = 0;
				double collectionFreq = collectionFreqs[k];
				if ( collectionFreq == 0 ){
                	continue; // token not in index
                }
				if(docx.containsKey(i) && docx.get(i).containsKey(token[k])){ // prevent not found.
					collectionWordsDocFreq = docx.get(i).get(token[k]);
                }
				score = score * ( ( collectionWordsDocFreq + mu*( collectionFreq/collectionLength ) ) / ( docLength + mu ) ); 
			}
			//store documents score
			//System.out.println(score);
            Document aDocument = new Document(Integer.toString(i), ixreader.getDocno(i), score); 
			documents.add(aDocument);
		}
		
		// compare each array list and reverse array list
        Collections.sort(documents, Collections.reverseOrder(new Comparator<Document>(){
        	@Override
        	public int compare(Document d1, Document d2) {
        		return new Double(d1.score()).compareTo(new Double(d2.score()));
        	}   
        	}));
        
		List<Document> documents1 = new ArrayList<>();
        // only get Top N result 
        for (int t=0;t<TopK;t++){
        		documents1.add(documents.get(t));
        }
		
        HashSet<String> rfId=new HashSet <String> (); // store top 100 document docid

        // get Top K for feedback
        for (int t=0;t<TopK;t++){
        		documents1.add(documents.get(t));
        		String temp = documents.get(t).docid(); // put top 100 docid into hashset 
        		rfId.add(temp);
        }
		
        Double collectionLengthTop100 = 0.0;
        
        // get top 100 collection length
        for(String str : rfId) {
        		int docId = Integer.parseInt(str);
        		int dLength = ixreader.docLength(docId);
        		collectionLengthTop100 = collectionLengthTop100 + dLength; // sum the top 100 collection length
        }
        
        // pseudoRF find top 100 P(token|feedback model) 
        for (int q=0;q<token.length;q++ ) {

			postingList = ixreader.getPostingList(token[q]); // postinglist
			Double tokenFreq = 0.0; // token freauency
			Double tokenFreqTotal = 0.0; // total token freauency 
			if(postingList != null){
				for (int j = 0; j < postingList.length; j++) {
					int docid = postingList[j][0];
					if(rfId.contains(String.valueOf(docid))){ 
						tokenFreq = tokenFreq+postingList[j][1];
					}
					tokenFreqTotal = tokenFreqTotal+postingList[j][1]; 
				}		
			}
			Double scoreResult = (tokenFreq+(mu*(tokenFreqTotal/collectionLength)))/(collectionLengthTop100+mu);
			tokenRFScore.put(token[q], scoreResult); 

		}
		return tokenRFScore;
	}
	
	
}