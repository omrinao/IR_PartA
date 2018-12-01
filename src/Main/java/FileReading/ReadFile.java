package FileReading;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import Indexing.Indexer;
import Parse.Parser;

/**
 * a class to read all documents from a given _corpusPath
 */
public class ReadFile implements Runnable{
    public int numOfFilesRead;
    public int numOfDocRead;
    private String _corpusPath;
    private String _originalPath;

    static Indexer indexer = new Indexer(2);
    static BlockingQueue<Document> queue = new ArrayBlockingQueue<Document>(100);

    public ReadFile(String path) {
        this._originalPath = path;
        this._corpusPath = path + "\\corpus";
        numOfDocRead = 0;
        numOfFilesRead = 0;
    }

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
    public void getDocuments(){
        File directory = new File(_corpusPath);
        Parser p = new Parser(getStopWords());
        if (directory.isDirectory())
            for (File sub_dir : directory.listFiles(File::isDirectory)){
                getDocs(sub_dir, p);
            }
        System.out.println("number of docs read at READ FILE: " + numOfFilesRead);
        System.out.println("number of documents made at READ FILE: " + numOfDocRead );
        System.out.println("number of documents parsed at PARSER: " + p.parsedDoc);
    }

    /**
     * an aid method to extract all files from sub directory
     * @param sub_dir - the sub directory containing file with documents
     */
    private void getDocs(File sub_dir, Parser p) {
        File[] allFiles = sub_dir.listFiles();

        if (allFiles == null || allFiles.length == 0){
            System.out.println("No files in this directory");
            return;
        }

        for (File f : allFiles){
            splitDoc(f, p);
            numOfFilesRead++;
        }
    }

    /**
     * an aid method to create all documents from a file
     * @param f - the working file
     */
    private void splitDoc(File f, Parser p) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            StringBuilder s = new StringBuilder();
            boolean sameDoc = false;
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (sameDoc && !line.contains("</DOC>"))
                    s.append(line).append("\n");
                if (line.contains("<DOC>"))
                    sameDoc = true;
                if (line.contains("</DOC>")){
                    sameDoc = false;
                    Document d = new Document(s.toString());
                    //Parse p = new Parse(getStopWords());
                    p.parseDocument();
                    queue.add(d);

                    if (queue.size() == 5){
                        indexer.setParserDone(true);
                        indexer.indexDocuments();
                        System.exit(5);
                    }


                    numOfDocRead++;
                    // System.out.println(d.getDocNum() + "\n" + Arrays.toString(d.getTermsMap().entrySet().toArray()));
                    s = new StringBuilder();
                    i++;
                }


            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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
        getDocuments();
    }


    public static void main(String[] args){
        /**
         * TODO: fix problems at parser:
         *      1. bad insertion of single months
         *      2. bad insertion of word divided by '-'
         *
         *//*
        ReadFile r = new ReadFile("C:\\Users\\חגי קלינהוף\\Desktop\\שנה ג'\\סמסטר ה'\\אחזור מידע\\פרויקט מנוע\\חלק 1");
        indexer.setDocsQueue(queue);
        long start = System.nanoTime();
        r.getDocuments();
        long end = System.nanoTime();
        long total = end-start;
        long milis = total/1000000;
        System.out.println(" Done! \n Total Time : " + milis/60000);
        System.out.println();
        try (Stream<Path> paths = Files.walk(Paths.get( System.getProperty("user.dir")), 1)){
            paths.filter(Files::isRegularFile).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    if (path.getName(path.getNameCount()-1).toString().startsWith("PartialPosting"))
                        System.out.println(path);
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }*/

        System.out.println(java.time.LocalTime.now());
    }

}