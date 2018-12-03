package Indexing;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class IndexMerger {

    private final String WORKING_DIR;                   // where all the files to be read and write
    private final String FINAL_Posting = "FinalPosting";
    private final String TXT = ".txt";

    PriorityQueue<String> _termsQueue;                  // queue of terms from all buffers
    ArrayList<BufferedReader> _postingReaders;          // list of all partial posting readers
    ArrayList<Integer> _usedBufferesIdx;
    BufferedWriter _postingWriter;                      // final posting writer
    HashMap<String, TermData> _corpusDictionary;        // final dictionary to enter pointers
    long _postingPointer;


    public IndexMerger(String working_dir, HashMap<String, TermData> _corpusDictionary) {
        WORKING_DIR = working_dir;
        this._corpusDictionary = _corpusDictionary;
        _postingPointer = 0;

        _postingReaders = new ArrayList<>();
        fillReaders();
        _usedBufferesIdx = new ArrayList<>();

        try {
            _postingWriter = new BufferedWriter(new PrintWriter(WORKING_DIR + FINAL_Posting + TXT));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        _termsQueue = new PriorityQueue<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                int o1Idx = o1.indexOf("#~");
                String o1Comparable = o1.substring(0, o1Idx);

                int o2Idx = o2.indexOf("#~");
                String o2Comparable = o2.substring(0, o2Idx);

                return o1Comparable.compareToIgnoreCase(o2Comparable);
            }
        });
    }


    /**
     * a method to open buffered readers to all partial posting
     */
    private void fillReaders() {
        try (Stream<Path> paths = Files.walk(Paths.get(WORKING_DIR), 1)){
            paths.filter(Files::isRegularFile).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    if (path.getName(path.getNameCount()-1).toString().startsWith("PartialPosting")) {
                        try {
                            _postingReaders.add(new BufferedReader(new FileReader( path.toFile())));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        //System.out.println(path);
                    }
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * a method used to merge all posting to a single final one
     */
    public void mergePostings() throws IOException {

        try{
            fillQueue();

            Comparator<? super String> comparator = _termsQueue.comparator();

            String curTerm = null;
            while (!_termsQueue.isEmpty()){
                curTerm = _termsQueue.poll();
                //System.out.println("Polled term: " + curTerm);
                rememberBuffer(curTerm);

                int termEnd = curTerm.indexOf("#~");
                String origTerm = curTerm.substring(0, termEnd);
                String toAddTerm = arrangeFinalTerm(curTerm, comparator);

                if (_corpusDictionary.containsKey(origTerm.toLowerCase())){ // lower case exist!
                    _corpusDictionary.get(origTerm.toLowerCase()).m_pointer = _postingPointer;
                  //  _postingPointer += toAddTerm.length();
                }
                else if (_corpusDictionary.containsKey(origTerm.toUpperCase())) { // lower case does not exist!
                    _corpusDictionary.get(origTerm.toUpperCase()).m_pointer = _postingPointer;
                  //  _postingPointer += toAddTerm.length();
                }
                else if (_corpusDictionary.containsKey(origTerm)) {
                    _corpusDictionary.get(origTerm).m_pointer = _postingPointer;
                   // _postingPointer += toAddTerm.length();
                }

                properFillQueue();

                String onlyATest = origTerm + "#~" + toAddTerm;
                _postingPointer += onlyATest.getBytes().length;//(origTerm.length()+1);
                _postingWriter.append(onlyATest);
                //System.out.println("wrote term: " + onlyATest);

            }
            //cutResources();
        }catch (IOException e){

            System.out.println("FAILED MERGING");
        }

        finally {
            cutResources();
            System.out.println("finished merging. program should be over " + java.time.LocalTime.now());
        }

    }

    /**
     * a method to close all resources needed for the merge operation
     */
    private void cutResources() {
        try{
            for (BufferedReader br : _postingReaders)
                br.close();

            _postingWriter.close();
        }
        catch (IOException e){
            System.out.println("Error closing all resources AFTER merge.");
            System.out.println(e.getMessage());
        }
    }

    /**
     * filling the queue with every consumes buffer line
     */
    private void properFillQueue() {
        for (Integer i : _usedBufferesIdx){
            insertLineToQueue(i);
        }
        _usedBufferesIdx.clear();
    }

    /**
     * a method that receives an integer of buffered reader location, get it's line and puts it in the queue
     * @param i - the integer representing the buffered reader
     */
    private void insertLineToQueue(Integer i) {
        BufferedReader br = _postingReaders.get(i);
        try{
            String line = br.readLine();
            if (line != null){
                line += i;
                _termsQueue.add(line);
            }
        }catch (IOException e){
            System.out.println("Error at getting line!");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * method that receive a term partial posting representation
     * returns a final posting representation by taking all of the same term lines and concatenate them
     * @param curTerm - the partial posting term
     * @param comparator - the comparing class
     * @return - final posting representing this term
     */
    private String arrangeFinalTerm(String curTerm, Comparator<? super String> comparator) {

        if (_termsQueue.peek() == null){ // no other terms in the queue case
            return cutLineToTerm(curTerm) + "\n";
        }

        StringBuilder builder = new StringBuilder(cutLineToTerm(curTerm));
        while (_termsQueue.peek() != null &&
                comparator.compare(curTerm, _termsQueue.peek()) == 0){ // concatenate all equal term. later will be added to the proper term
            String lineToAdd = _termsQueue.poll();
            rememberBuffer(lineToAdd);
            builder.append(cutLineToTerm(lineToAdd));
        }
        builder.append("\n");
        return builder.toString();
    }

    /**
     * a method to fill the queue with a line from each partial posting
     */
    private void fillQueue() {
        for (int i=0; i<_postingReaders.size(); i++){
            try{
                BufferedReader br = _postingReaders.get(i);
                String line = br.readLine();
                if (line != null){
                    line += i;

                    _termsQueue.add(line);
                }
            } catch (IOException e) {
                System.out.println("Error occurred accessing file\n" + e.getMessage() );
            }
        }
    }

    /**
     * a method to add a buffer number into the list of used buffers
     * @param line - the line of which to add
     */
    private void rememberBuffer(String line){
        String bufferNum = line.substring(line.lastIndexOf(',')+1);
        _usedBufferesIdx.add(Integer.valueOf(bufferNum));
    }

    /**
     * a method to cut a line representing a term in partial posting
     * into a line representing a term in the final posting
     * @param line - the partial posting representation
     * @return - final posting representation
     */
    private String cutLineToTerm(String line){
        int lastChar = line.lastIndexOf(',');
        int firstChar = line.indexOf("#~");
        return line.substring(firstChar+2, lastChar+1);
    }
}