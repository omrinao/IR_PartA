package FileReading;

import Indexing.Indexer;
import Indexing.TermData;
import Parse.Parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReadFile2 implements Callable<HashMap<Path, Exception>> {

    public int numOfFilesRead;
    public int numOfDocRead;
    private String _corpusPath;
    private String _originalPath;
    private BlockingQueue<Document> _documentsQueue;
    private Parser _parser;


    public ReadFile2(String path) {
        this._originalPath = path;
        this._corpusPath = path + "\\corpus";
        numOfDocRead = 0;
        numOfFilesRead = 0;
    }

    public void setQueue(BlockingQueue<Document> queue){
        this._documentsQueue = queue;
    }

    public void setParser (Parser p){this._parser = p;}

    /**
     * method to find and retrieve stop words
     * @return - hashset of stop words
     */
    public HashSet<String> getStopWords(){
        HashSet<String> toReturn = new HashSet<>();
        File f = new File(_originalPath + "\\stop_words.txt");
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
            System.out.println("ReadFile2: Finished reading, send to parser");
            Document last = new Document("done");
            last.setFinal(true);
            _documentsQueue.put(last);

        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }

        return problemPaths;
    }

    private void extractDocs(Path path) throws IOException, InterruptedException {
        String entireFile = new String(Files.readAllBytes(path));

        String[] splitByDoc = entireFile.split("<DOC>");
        for (String doc :
                splitByDoc) {
            Document d = new Document(doc);
            //System.out.println("Read doc: " + d.getDocNum());
            _documentsQueue.put(d);
        }

    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <_parser>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     @Override
     public void run() {
     getDocuments();
     }
     */

    public static void main(String[] args){
        long start = System.nanoTime();
        System.out.println("Started");

        // initing classes
        ReadFile2 read = new ReadFile2("C:\\Users\\חגי קלינהוף\\Desktop\\שנה ג'\\סמסטר ה'\\אחזור מידע\\פרויקט מנוע\\Part 1 tests");
        Parser p = new Parser();
        read.setParser(p);
        p.setStopWords(read.getStopWords());
        Indexer i = new Indexer(200);
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

        TermData t = i._corpusDictionary.get("ARTHUR");
        long pointer = t.getM_pointer();

        try {
            RandomAccessFile file = new RandomAccessFile(System.getProperty("user.dir") + "\\FinalPosting.txt", "r" );
            file.seek(pointer);
            String termLine = file.readLine();
            System.out.println(termLine);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public HashMap<Path, Exception> call() throws Exception {
        return getDocuments();
    }
}