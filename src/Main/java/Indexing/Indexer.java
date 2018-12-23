package Indexing;

import FileReading.Document;
import Parse.Parser;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

public class Indexer implements Runnable {

    private int m_docsPerPartialPosting;    // amount of docs per partial posting
    private int _partialPostingCount;       // counter for doc in a partial posting
    private int m_docsIndexed;              // needed for reports
    private boolean _stemmer;               // if stemming is used

    private final String PARTIAL_POSTING = "PartialPosting";
    private final String TXT = ".txt";
    private final String DOC_POSTING = "DocumentPosting";
    private final String WORKING_DIRECTORY;
    private final String STEMMER = "STEM";

    private BlockingQueue<Document> m_docsQueue;            // queue of document given from the parser
    public HashMap<String, TermData> _corpusDictionary;         // the total corpus dictionary
    private HashMap<String, TreeSet<PostingTermData>> _partialPosting;  // the partial posting data for the current documents

    private CityIndex _cityIndex;                            // instance of the city API, contains all API relevant details
    private HashMap <String, CityDetails> _cityDictionary;   // structure for city details

    private HashMap<Integer, PostingDocData> _docData;       // structure for docs posting data
    private DocumentDictionary _docDictionary;
    private HashSet<String> _docLanguages;                  // structure for docs languages
    private double _avgDocLength;                           // avd doc length for later ranking

    public Indexer(int docsPerPartialPosting, String writingLocation) {
        this.WORKING_DIRECTORY = writingLocation;
        this.m_docsPerPartialPosting = docsPerPartialPosting;
        this.m_docsIndexed = 0;
        this._partialPostingCount = 0;

        this._corpusDictionary = new HashMap<>();
        this._partialPosting = new HashMap<>();
        this._cityIndex = new CityIndex();
        this._cityDictionary = new HashMap<>();

        _docData = new HashMap<>();
        _docLanguages = new HashSet<>();
        _docDictionary = new DocumentDictionary();
    }


    /**
     * method to get documents and index them properly in the dictionary
     * and in partial posting structure for later disk write
     */
    public void indexDocuments(){
        int partialIndexed = 0;
        double totalDocsLength = 0;
        while (true){
            Document d = null;
            try {

                d = m_docsQueue.take();

                if (d.getFinal()){
                    break;
                }

                d.calcLength(); // setting length of the document for later use
                totalDocsLength += d.getLength();

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

                            _corpusDictionary.put(term, toLowerTerm);
                        }
                        else {  // adding a new term data to the dictionary
                            TermData newTerm = new TermData(1, docTerms.get(term).size());
                            _corpusDictionary.put(term, newTerm);
                        }


                        if (_partialPosting.containsKey(term)){ // lower case exist this partial posting
                            TreeSet<PostingTermData> postingTermData = _partialPosting.get(term);  // adding to partial posting
                            postingTermData.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                        }
                        else if (_partialPosting.containsKey(term.toUpperCase())){ // upper case exist this partial posting
                            TreeSet<PostingTermData> toLowerPosting = _partialPosting.remove(term.toUpperCase());
                            toLowerPosting.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                            _partialPosting.put(term, toLowerPosting);
                        }
                        else { // upper nor lower exist this partial posting
                            PostingTermData newPosting = new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength());
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
                            postingTreeSet.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                        }
                        else if (_partialPosting.containsKey(term)){ // have seen upper this partial posting
                            TreeSet<PostingTermData> postingTree = _partialPosting.get(term);
                            postingTree.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                        }
                        else { // upper case and does'nt exist
                            PostingTermData termData = new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength());
                            TreeSet<PostingTermData> treeSet = new TreeSet<>();
                            treeSet.add(termData);
                            _partialPosting.put(term, treeSet);
                        }
                    }


                } // for end
                String fixedCity = "";
                // inserting to city dictionary
                if (!d.getCity().isEmpty()) {//checking if <F P=104> label is exist
                    String city = fixCity(d.getCity());
                    fixedCity = city;
                    if (!city.isEmpty()) {//fix city return a value
                        String cityPattern = "";//first uppercase then lowercase
                        HashMap<String, ArrayList<Integer>> docsLocations = new HashMap<>();
                        if (d.getTermsMap().containsKey(city.toUpperCase())) {//check if the document contains the city
                            docsLocations.put(d.getDocNum(), d.getTermsMap().get(city.toUpperCase()));//get the docnum + locations
                        }
                        else if (d.getTermsMap().containsKey(city.toLowerCase())){
                            docsLocations.put(d.getDocNum(), d.getTermsMap().get(city.toLowerCase()));//get the docnum + locations
                        }
                        else {
                            docsLocations.put(d.getDocNum(), null);
                        }
                        cityPattern = Character.toUpperCase(city.charAt(0)) + city.substring(1).toLowerCase();
                        if (_cityDictionary.get(cityPattern) == null) {//city dictionary does not contains the city
                            if (_cityIndex.getDetails(cityPattern)[0] == null) {//its not a capital city
                                _cityDictionary.put(cityPattern, new CityDetails(null, null, null, docsLocations));//inserting just the city name and the document number
                            } else {// its a capital city
                                String[] details = _cityIndex.getDetails(cityPattern);
                                details[1] = fixPopulation(details[1]);
                                _cityDictionary.put(cityPattern, new CityDetails(details[0], details[2], details[1], docsLocations));
                            }

                        } else {//city dictionary contains the city, updating docnum + locations
                            _cityDictionary.get(cityPattern).getM_docs().put(d.getDocNum(), docsLocations.get(d.getDocNum()));//adding another document
                        }
                    }
                }

                Integer docNum = Integer.valueOf(d.getDocNum());
                _docDictionary.insertDoc(docNum, -1, d.get_docName());
                _docData.put(docNum,
                        new PostingDocData(d.getMaxTF(), docTerms.size(), d.get_startLine(), d.get_endLine(), d.get_path(), d.getLength(), fixedCity));

                if (!d.getLanguage().isEmpty()){
                    _docLanguages.add(d.getLanguage());
                }


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
        System.out.println("doc indexed: "+m_docsIndexed);
        IndexMerger merger = new IndexMerger(WORKING_DIRECTORY, _corpusDictionary, _stemmer);
        try {
            merger.mergePostings();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TreeMap<String, TermData> sorted = new TreeMap<>(_corpusDictionary);

        _avgDocLength = totalDocsLength/m_docsIndexed;
        _docDictionary.set_avgDocLength(_avgDocLength);
        writeDictionary();
        writeDocDictionary();
        writeCityDictionary();
        writeLanguagesSet();


        System.out.println("Exiting Indexer");

        //System.out.println("Starting to extract data for report!");
        //dataForReport();
    }

    /**
     * this method receive a string which represents a city and fixes some problem that could be happen
     * @param city - the city
     * @return - fixed city string
     */
    private String fixCity(String city) {
        String valueToReturn = Parser.removePeriod(city);
        String regex = "(.)*(\\d)(.)*";
        Pattern pattern = Pattern.compile(regex);

        if (pattern.matcher(valueToReturn).matches())//city contains number
            return "";

        if (valueToReturn.equalsIgnoreCase("the") || valueToReturn.equalsIgnoreCase("FOR"))
            return "";

        return valueToReturn;
    }

    /**
     * method to write the languages to the disk
     */
    private void writeLanguagesSet() {
        if (_docLanguages.isEmpty()){
            return;
        }
        String path = WORKING_DIRECTORY + "LanguagesSet";
        try(ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(path));) {
            write.writeObject(_corpusDictionary);
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * method that fix the population to x.yy M format
     * @param detail - the population
     * @return - fixed population
     */
    private String fixPopulation(String detail) {
        float population = 0;
        String valueToReturn = "";
        try{
            population = Float.valueOf(detail);
            if (population > 1000000){
                population = population / 1000000;
                valueToReturn = String.format("%.2f", population) + "M";
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return valueToReturn;
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
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * getter for document dictionary
     * @return - document dictionary
     */
    public DocumentDictionary get_docDictionary() {
        return _docDictionary;
    }

    /**
     * method to write document dictionary to disk
     */
    private void writeDocDictionary() {
        String path;
        if (_stemmer){
            path = WORKING_DIRECTORY + STEMMER + "DocsDictionary";
        }
        else{
            path = WORKING_DIRECTORY + "DocsDictionary";
        }

        try(ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(path));) {
            write.writeObject(_docDictionary);
            write.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to write city dictionary to disk
     */
    private void writeCityDictionary(){
        String path = null;
        if (_stemmer){
            path = WORKING_DIRECTORY + STEMMER + "CityDictionary";
        }else{
            path = WORKING_DIRECTORY + "CityDictionary";
        }

        try(ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(path))) {
            write.writeObject(_cityDictionary);
            write.flush();
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
            if (!_docData.isEmpty()){
                writeToDocPosting();
            }
        }
        catch (IOException e){
            /* do something here */
            e.printStackTrace();
        }

        _partialPostingCount++;
        _partialPosting.clear(); // maybe clear
        _docData.clear(); // bug fix, need to test
    }

    /**
     * method to write to doc posting
     * @throws IOException - if error occurred during writing
     */
    private void writeToDocPosting() throws IOException{
        String path = null;
        if (_stemmer)
            path = WORKING_DIRECTORY + DOC_POSTING +STEMMER + TXT;
        else
            path = WORKING_DIRECTORY + DOC_POSTING + TXT;

        try(
                BufferedWriter bw = new BufferedWriter(new PrintWriter(path, "UTF-8"))
                ){
            TreeMap<Integer, PostingDocData> sorted = new TreeMap<>(_docData);
            long pointer = _docDictionary.get_nextPointer();

            for (Integer docNum :
                    sorted.keySet()) {
                PostingDocData cur = sorted.get(docNum);
                String postingToBeWritten = cur.toString() + "\n";
                _docDictionary.getDocData(docNum)._pointer = pointer;
                bw.append(postingToBeWritten);

                pointer += postingToBeWritten.getBytes("UTF-8").length;
            }
            bw.flush();

            _docDictionary.set_nextPointer(pointer);
        }
    }


    /**
     * a method to write the lower case partial posting
     * @param partialName - the name of the partial posting
     * @throws IOException - if an error occurred during file execution
     */
    private void writePartialPosting(String partialName) throws IOException {
        try(
                BufferedWriter lowerWriter = new BufferedWriter(new PrintWriter(partialName, "UTF-8"))
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
        StringBuilder toReturn = new StringBuilder(termEntry + "#~");

        for (PostingTermData termData : treeSet){
            toReturn.append(termData._doc).append(" "); // writing doc num
            toReturn.append(termData._termOccurrences).append(" "); // writing doc TF
            toReturn.append(termData._locations[0]).append(" "); // if occurred in first 20%
            toReturn.append(termData._locations[1]); // if occurred in last 20%
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

    public void setStemmer(boolean stem){
        this._stemmer = stem;
    }

    public int getNumOfIndexed(){return m_docsIndexed;}

    public int getNumOfTerms(){return _corpusDictionary.size();}

    public HashSet<String> get_docLanguages(){return _docLanguages;}

    public double getAvgDocLength() {
        return _avgDocLength;
    }
    /*
        public static void dataForReport(){
            try {
                System.out.println("arting");

                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("d:\\documents\\users\\tamiry\\Downloads\\STEMTermsDictionary"));
                HashMap<String, TermData> loadedDict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();
                System.out.println("Number of unique terms with stemming: " + loadedDict.size());

            ObjectInputStream inputStream2 = new ObjectInputStream(new FileInputStream("d:\\documents\\users\\tamiry\\Downloads\\CityDictionary"));
            HashMap<String, CityDetails> cityDict = (HashMap<String, CityDetails>) inputStream2.readObject();
            inputStream2.close();


            // ---------- amount of states and cities in the corpus
            int totalCities = 0;
            int capitalCities = 0;
            String mostCitiesDocName = null;
            String city = null;
            int mostCityAmount = 0;
            ArrayList<Integer> mostCityLocations = null;
            for (String s :
                    cityDict.keySet()){
                System.out.println(String.format("City name: %s",s));
                totalCities++;
                CityDetails cd = cityDict.get(s);
                if (cd.get_country() != null){
                    System.out.println("  and is capital");
                    capitalCities++;
                }

                HashMap<String, ArrayList<Integer>> details = cd.getM_docs();
                for (String doc :
                        details.keySet()){
                    ArrayList<Integer> arr = details.get(doc);
                    if (arr != null && arr.size() > mostCityAmount){
                        mostCitiesDocName = doc;
                        mostCityAmount = arr.size();
                        city = s;
                        mostCityLocations = arr;
                    }
                }
            }

            System.out.println(String.format("Q4+5: Number of different capital cities/states in the corpus: %s", capitalCities));
            System.out.println(String.format("Q5: Total number of cities in the corpus: %s", totalCities));
            System.out.println(String.format("Q6: Doc number with most cities references: %s", mostCitiesDocName));
            System.out.println(String.format("\t The city name: %s", city));
            System.out.println(String.format("\t The locations are: %s", mostCityLocations.toString()));

        } catch (IOException e) {
            System.out.println("Error at opening file: " + e.getMessage());

        } catch (ClassNotFoundException f) {
            System.out.println("Error at casting object: " + f.getMessage());
        }


    }
    public static void writeCSV(HashMap<String, TermData> termsMap, String path) {

        try (
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path)))
        ) {
            for (String s :
                    termsMap.keySet()) {
                pw.println(String.format("%s,%s", s, termsMap.get(s).m_totalTF));
            }
            pw.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */

}
