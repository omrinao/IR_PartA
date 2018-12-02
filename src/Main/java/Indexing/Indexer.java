package Indexing;

import FileReading.Document;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Indexer implements Runnable {

    private int m_docsPerPartialPosting;
    private int _partialPostingCount;
    private int m_docsIndexed;
    private volatile boolean m_parsingDone;
    private boolean _stemmer;

    private final String PARTIAL_POSTING = "PartialPosting";
    private final String TXT = ".txt";
    private final String WORKING_DIRECTORY = System.getProperty("user.dir") + "\\";
    private final String STEMMER = "STEM";

    private BlockingQueue<Document> m_docsQueue;            // queue of document given from the parser
    public HashMap<String, TermData> _corpusDictionary;         // the total corpus dictionary
    private HashMap<String, TreeSet<PostingTermData>> _partialPosting;  // the partial posting data for the current documents

    //private TreeMap<String, TreeSet<PostingTermData>> _upperPartialPosting;//private HashMap<String, TermData> _upperCaseDictionary;


    public Indexer(int docsPerPartialPosting) {
        this.m_docsPerPartialPosting = docsPerPartialPosting;
        this.m_docsIndexed = 0;
        this.m_parsingDone = false;
        this._partialPostingCount = 0;

        this._corpusDictionary = new HashMap<>();
        //this._upperCaseDictionary = new HashMap<>();

        this._partialPosting = new HashMap<>();
        //this._upperPartialPosting = new TreeMap<>();
    }


    /**
     * method to get documents and index them properly in the dictionary
     * and in partial posting structure for later disk write
     */
    public void indexDocuments(){
        int partialIndexed = 0;
        while (true){
            Document d = null;
            try {

                d = m_docsQueue.take();

                if (d.getFinal()){
                    break;
                }

                d.setLength(d.getTermsMap().size()); // setting length of the document for later use

                LinkedHashMap<String, ArrayList<Integer>> docTerms = d.getTermsMap();
                for (String term: docTerms.keySet()) {//inserting to dictionary + partial posting

                    if (term.equals(term.toLowerCase())){ // it's a lower case term or non alphabetic term
                        if (_corpusDictionary.containsKey(term)){
                            TermData t = _corpusDictionary.get(term);
                            t.m_df = t.m_df+1;                        // adding to document frequency
                            t.m_totalTF += docTerms.get(term).size();   // adding to total tf count
                        }
                        else if (_corpusDictionary.containsKey(term.toUpperCase())) { // upper case term now observed with lower case!
                            TermData toLowerTerm = _corpusDictionary.remove(term.toUpperCase());
                            toLowerTerm.m_df += 1;
                            toLowerTerm.m_totalTF += docTerms.get(term).size();

                            _corpusDictionary.put(term.toUpperCase(), toLowerTerm);
                        }
                        else {  // adding a new term data to the dictionary
                            TermData newTerm = new TermData(1, docTerms.get(term).size());
                            _corpusDictionary.put(term, newTerm);
                        }


                        if (_partialPosting.containsKey(term)){ // lower case exist this partial posting
                            TreeSet<PostingTermData> postingTermData = _partialPosting.get(term);  // adding to partial posting
                            postingTermData.add(new PostingTermData(d.getDocNum(), docTerms.get(term)));
                        }
                        else if (_partialPosting.containsKey(term.toUpperCase())){ // upper case exist this partial posting
                            TreeSet<PostingTermData> toLowerPosting = _partialPosting.remove(term.toUpperCase());
                            toLowerPosting.add(new PostingTermData(d.getDocNum(), docTerms.get(term)));
                            _partialPosting.put(term, toLowerPosting);
                        }
                        else { // upper nor lower exist this partial posting
                            PostingTermData newPosting = new PostingTermData(d.getDocNum(), docTerms.get(term));
                            TreeSet<PostingTermData> treeSet = new TreeSet<>();
                            treeSet.add(newPosting);
                            _partialPosting.put(term, treeSet);
                        }
                    }

                    // it's an upper case term
                    else{
                        if (_corpusDictionary.containsKey(term.toLowerCase())){ // it's an upper case but have seen a lower case before
                            TermData t = _corpusDictionary.get(term.toLowerCase());
                            t.m_df += 1;
                            t.m_totalTF += docTerms.get(term).size();
                        }
                        else if (_corpusDictionary.containsKey(term)){ // have seen only upper case
                            TermData t = _corpusDictionary.get(term);
                            t.m_df += 1;
                            t.m_totalTF += docTerms.get(term).size();
                        }
                        else { // never seen before
                            TermData newTerm = new TermData(1, docTerms.get(term).size());
                            _corpusDictionary.put(term, newTerm);
                        }

                        if (_partialPosting.containsKey(term.toLowerCase())){ // it's an upper, but have seen a lower case this partial posting
                            TreeSet<PostingTermData> postingTreeSet = _partialPosting.get(term.toLowerCase());
                            postingTreeSet.add(new PostingTermData(d.getDocNum(), docTerms.get(term)));
                        }
                        else if (_partialPosting.containsKey(term)){ // have seen upper this partial posting
                            TreeSet<PostingTermData> postingTree = _partialPosting.get(term);
                            postingTree.add(new PostingTermData(d.getDocNum(), docTerms.get(term)));
                        }
                        else { // upper case and does'nt exist
                            PostingTermData termData = new PostingTermData(d.getDocNum(), docTerms.get(term));
                            TreeSet<PostingTermData> treeSet = new TreeSet<>();
                            treeSet.add(termData);
                            _partialPosting.put(term, treeSet);
                        }
                    }
                } // for end

                m_docsIndexed++; // needed for work report
                partialIndexed++;

                if (partialIndexed == m_docsPerPartialPosting){
                    writePartialPostings();
                    partialIndexed = 0;
                }

                //System.out.println("Indexed doc: " + d.getDocNum());
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        writePartialPostings(); // after finished all documents, empty the final partial posting

        System.out.println("FINISHED INDEXING at: " + java.time.LocalTime.now());
        IndexMerger merger = new IndexMerger(WORKING_DIRECTORY, _corpusDictionary);
        try {
            merger.mergePostings();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TreeMap<String, TermData> sorted = new TreeMap<>(_corpusDictionary);

        writeDictionary();
        System.out.println("Exiting Indexer");
    }

    /**
     * method to write the dictionary to disk
     */
    private void writeDictionary() {
        String path;
        if (_stemmer){
            path = WORKING_DIRECTORY + STEMMER + "TermsDictionary";
        }
        else{
            path = WORKING_DIRECTORY + "TermsDictionary";
        }

        try(ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(path));) {
            write.writeObject(_corpusDictionary);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * a method to write partial posting once needed
     * the method also truncate the existing partial posting objects
     */
    private void writePartialPostings() { /* this method get throws. need to handle */
        String partialName;

        if (_stemmer)
            partialName = WORKING_DIRECTORY + PARTIAL_POSTING  + _partialPostingCount + STEMMER + TXT;
        else
            partialName = WORKING_DIRECTORY + PARTIAL_POSTING  + _partialPostingCount + TXT;

        try {
            if (!_partialPosting.isEmpty())
                writePartialPosting(partialName);
        }
        catch (IOException e){
            /* do something here */
        }

        _partialPostingCount++;
        _partialPosting = new HashMap<>(); // maybe clear
    }


    /**
     * a method to write the lower case partial posting
     * @param partialName - the name of the partial posting
     * @throws IOException - if an error occurred during file execution
     */
    private void writePartialPosting(String partialName) throws IOException {
        try(
                BufferedWriter lowerWriter = new BufferedWriter(new PrintWriter(partialName))
        ){
            // init an ordered map with comperator for strings ignoring upper/lower cases
            TreeMap<String, TreeSet<PostingTermData>> orderedPartialPosting = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            orderedPartialPosting.putAll(_partialPosting);

            for (String termEntry : orderedPartialPosting.keySet()){
                String toWrite = createPostingTerm(termEntry, _partialPosting.get(termEntry));

                lowerWriter.append(toWrite);
            }

            lowerWriter.flush();
        }
        catch (IOException e){
            throw new IOException("lower partial posting error.", e);
        }
    }

    /**
     * a method to create the string of a given term based on his posting data set
     * @param termEntry - the term to be written
     * @param treeSet - the posting data of the set
     * @return string representing the posting of the given term
     */
    private String createPostingTerm(String termEntry, TreeSet<PostingTermData> treeSet) {
        StringBuilder toReturn = new StringBuilder(termEntry + ":");

        for (PostingTermData termData : treeSet){
            toReturn.append(termData._doc).append(" ");
            toReturn.append(termData._termOccurrences).append(" ");
            for (Integer location : termData._locations){
                toReturn.append(location).append(" ");
            }
            toReturn.append(",");
        }
        toReturn.append("\n");
        return toReturn.toString();
    }


    public void setDocsQueue(BlockingQueue<Document> docsQueue) {
        m_docsQueue = docsQueue;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        indexDocuments();
    }

    public void setParserDone(boolean done){
        System.out.println("Indexer: received - Parsing is DONE");
        this.m_parsingDone = done;
    }

    public boolean getParserDone(){
        return m_parsingDone;
    }

    public void setStemmer(boolean stem){
        this._stemmer = stem;
    }
}