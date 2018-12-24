package FileReading;

import Indexing.IndexMerger;
import Indexing.Indexer;
import Indexing.TermData;
import Parse.Parser;
import Searching.Query;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReadFile2 implements Runnable {

    public int numOfFilesRead;
    public int numOfDocRead;
    private String _corpusPath;
    private String _originalPath;
    private BlockingQueue<Document> _documentsQueue;
    private int _docNum;


    public ReadFile2(String path) {
        this._originalPath = path;
        this._corpusPath = path;
        numOfDocRead = 0;
        numOfFilesRead = 0;
        _docNum = 0;
    }


    public void setQueue(BlockingQueue<Document> queue){
        this._documentsQueue = queue;
    }

    /**
     * method to find and retrieve stop words
     * @return - hashset of stop words
     */
    public HashSet<String> getStopWords(){
        HashSet<String> toReturn = new HashSet<>();
        File f = new File(_originalPath + "stop_words.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                toReturn.add(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return toReturn;
    }

    /**
     * a method to extract all documents from the given _corpusPath
     */
    public HashMap<Path, Exception> getDocuments(){
        HashMap<Path, Exception> problemPaths = new HashMap<>();

        try (Stream<Path> paths = Files.walk(Paths.get(_corpusPath), 2)){
            System.out.println("Reading files");
            paths.filter(Files::isRegularFile).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    try{
                        extractDocs(path);
                    }
                    catch (IOException | InterruptedException e){
                        problemPaths.put(path, e);
                    }

                }
            });
            System.out.println("ReadFile2: Finished reading, send to parser     num of doc: "+_docNum);
            Document last = new Document("done");
            last.setFinal(true);
            _documentsQueue.put(last);

        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }

        return problemPaths;
    }


    private void extractDocs(Path path) throws IOException, InterruptedException {

        try (BufferedReader br = new BufferedReader(new FileReader(path.toString()))) {
            String line;
            StringBuilder s = new StringBuilder();
            boolean sameDoc = false;
            short startLine = 0;
            short endLine = 0;
            short lineCounter = -1;

            while ((line = br.readLine()) != null) {
                lineCounter++;
                if (sameDoc && !line.contains("</DOC>")) {
                    s.append(line).append("\n");
                }

                else if (line.contains("<DOC>")) {
                    sameDoc = true;
                    startLine = lineCounter;
                }

                else if (line.contains("</DOC>")){
                    sameDoc = false;
                    endLine = lineCounter;

                    Document d = new Document(s.toString(), startLine, endLine, _docNum, path.getFileName().toString());
                    _docNum++;

                    _documentsQueue.put(d);
                    numOfDocRead++;
                    s.setLength(0);
                }


            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
    }


 /*
    public static void main(String[] args){
       long start = System.nanoTime();
        System.out.println("Started");

        // initing classes
        ReadFile2 read = new ReadFile2("C:\\Users\\חגי קלינהוף\\Desktop\\שנה ג'\\סמסטר ה'\\אחזור מידע\\פרויקט מנוע\\Part 1 tests");
        //ReadFile2 read = new ReadFile2("C:\\Users\\חגי קלינהוף\\Desktop\\שנה ג'\\סמסטר ה'\\אחזור מידע\\פרויקט מנוע\\Part 1 tests");
        Parser p = new Parser();
        read.setParser(p);
        p.setStopWords(read.getStopWords());
        Indexer i = new Indexer(1000, "C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output");
        p.setIndexer(i);

        // initing queues
        BlockingQueue<Document> before = new ArrayBlockingQueue<>(1000);
        BlockingQueue<Document> after = new ArrayBlockingQueue<>(1000);

        read.setQueue(before);
        p.setBeforeParse(before);
        p.setAfterParse(after);
        i.setDocsQueue(after);

        // starting threads
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<HashMap<Path, Exception>> f = service.submit(read);
        //Thread reader = new Thread(read);
        Thread parser = new Thread(p);
        Thread indexer = new Thread(i);
        //reader.start();

        parser.start();
        indexer.start();
        HashMap<Path, Exception> m = null;
        try {
            m = f.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        try {
            indexer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        finally {
            long end = System.nanoTime();
            long total = end-start;
            long milis = total/1000000;

            System.out.println(" Done! \n Total Time : " + milis/60000.00);
            System.out.println(java.time.LocalTime.now());
            System.out.println(m);
            service.shutdownNow();
            System.out.println("Parser Alive: " + parser.isAlive());
            System.out.println("Indexer Alive: " + indexer.isAlive());
        }

        TermData t = i._corpusDictionary.get("settled");
        long pointer = t.getM_pointer();

        try {
            RandomAccessFile file = new RandomAccessFile("C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output" + "\\FinalPosting.txt", "r" );
            file.seek(pointer);
            String termLine = file.readLine();
            System.out.println(termLine);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("d:\\documents\\users\\tamiry\\Downloads\\TermsDictionary"));
            HashMap<String, TermData> dict = (HashMap<String, TermData>) inputStream.readObject();
            inputStream.close();
            RandomAccessFile file = new RandomAccessFile("C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output\\FinalPosting.txt", "r" );
            BufferedWriter check = new BufferedWriter(new FileWriter("C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output" + "\\posting_check.txt"));
            TreeMap<String, TermData> sorted = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            sorted.putAll(dict);
            for (String s :
                    sorted.keySet()){
                long pointer = dict.get(s).getM_pointer();

                file.seek(pointer);
                String termLine = file.readLine() + "\n";
                check.write(termLine);
            }

            check.close();
        }
        catch (IOException e){
            System.out.println("cant open file \n" + e.getMessage());
        }catch (ClassNotFoundException f){
            System.out.println("object is not good " + f.getMessage());

        }

        Indexer.dataForReport();
    }
*/
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public void run () {
        getDocuments();
    }

    /**
     * method to get stop words
     * @param corpusPath - the path of the corup
     * @return - hash set of stop words
     */
    public static HashSet<String> getStopWords(String corpusPath){
        HashSet<String> toReturn = new HashSet<>();
        File f = new File(corpusPath + "stop_words.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                toReturn.add(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return toReturn;
    }


    /**
     * method to extract queries from a query file
     * @param queryFile - the path to the file
     * @return - list of containing queries
     * @throws IOException - if exception has occured
     */
    public static List<Query> getQueries(String queryFile) throws IOException{

        List<Query> toReturn = new ArrayList<>();
        List<String> file = Files.readAllLines(Paths.get(queryFile));
        StringBuilder queryData = new StringBuilder();

        for (String line :
                file){
            if (line.contains("</top>")){
                toReturn.add(buildQuery(queryData.toString()));
                queryData = new StringBuilder();
            }
            else{
                queryData.append(line);
            }
        }
        return toReturn;
    }

    /**
     * method to build a query
     * @param queryData - the entire query
     * @return - query object parsed by the entire string
     */
    private static Query buildQuery(String queryData) {
        Query toReturn = new Query();
        String phrase = "";

        int start = queryData.indexOf("Number:");
        int end = queryData.indexOf("<title>");
        phrase = queryData.substring(start+7, end).trim();
        toReturn.set_number(phrase);

        start = end;
        end = queryData.indexOf("<desc>");
        phrase = queryData.substring(start+7, end).trim();
        toReturn.set_title(phrase);

        start = queryData.indexOf("Description:");
        end = queryData.indexOf("<narr>");
        phrase = queryData.substring(start+12, end).trim();
        toReturn.set_description(phrase);

        start = queryData.indexOf("Narrative:");
        phrase = queryData.substring(start+10).trim();
        toReturn.set_narrative(phrase);

        return toReturn;
    }

    /**
     * method to extract text from file, used to show query result text.
     * @param path - corpus + directory path
     * @param startLine - start of the document
     * @param endLine - end of the document
     * @return
     */
    public static String getTextFromDoc (String path, int startLine, int endLine){

        String valueToReturn = "";

        try {
            int idx = path.lastIndexOf('\\');
            String fileName = path.substring(idx);
            String currentLine = "";
            BufferedReader br = new BufferedReader(new FileReader(path + fileName));
            int counter = 0;
            while (counter < startLine) {
                br.readLine();
                counter++;
            }
            while (!currentLine.contains("<TEXT>")) {
                currentLine = br.readLine();
                counter++;
            }
            while (counter < endLine && !currentLine.contains("</TEXT>")){
                currentLine = br.readLine();
                if (currentLine.contains("</TEXT>"))
                    valueToReturn += currentLine.substring(0, currentLine.length() - 7);
                else
                    valueToReturn += (currentLine + "\n");
                counter++;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return valueToReturn;
    }
}