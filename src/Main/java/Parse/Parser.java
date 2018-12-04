package Parse;

import FileReading.Document;
import Indexing.Indexer;
import Terms.IntWrapper;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Parser implements Runnable{

    private final List<String> m_badAffix = Arrays.asList("?", ":", "--", ".", ";", "{", "}", "[", "]", "(", ")", "=", "+", "`",
            "'", "\"", "!", "*", "~", "<", ">", "@", "&", "/", "|", "\\");
    private HashSet<String> m_stopWords;
    public int parsedDoc;
    private BlockingQueue<Document> _beforeParse;
    private BlockingQueue<Document> _afterParse;
    private volatile boolean doneReading = false;
    private boolean _stemmer;

    private Indexer _indexer;

    public Parser() {
    }

    public Parser(HashSet<String> stopWords) {
        this.m_stopWords = stopWords;
        parsedDoc = 0;
    }

    public void setStopWords(HashSet<String> stopWords){
        m_stopWords = stopWords;
    }

    public void setIndexer(Indexer idx){this._indexer = idx;}

    public void setBeforeParse (BlockingQueue<Document> queue){this._beforeParse = queue;}

    public void setAfterParse(BlockingQueue<Document> queue) {this._afterParse = queue;}

    public void setDoneReading (boolean done){
        this.doneReading = done;
        System.out.println("Parse: received, Reading is DONE");
    }

    public void setStemmer(boolean stem){this._stemmer = stem;}

    /**
     * main parse method, that parse a single document
     */
    public void parseDocument() {
        Document d = null;
        while (true) {
            try {
                d = _beforeParse.take();
                //System.out.println("took doc: " + d.getDocNum());
                if (d.getFinal()) { // checking if the last one
                    _afterParse.put(d);
                    System.out.println("Parse: Parsing is DONE, send to Indexing");
                    break;
                }
                d.setLanguage(removePeriod(d.getLanguage()));
                String allText = d.getText();
                if (allText == null || allText.isEmpty())
                    continue;
                allText = allText.replace(",", "");
                String[] words = allText.split("\\s+");
                IntWrapper location = new IntWrapper(0);
                parsedDoc++;

                for (int i = 0; i < words.length; i++) {
                    try {
                        String finalTerm = null;
                        String word = words[i];
                        int finalIdx = words.length - 1;
                        LinkedHashMap<String, ArrayList<Integer>> toInsert = d.getTermsMap();
                        boolean wordInsert = false;
                        boolean monthFix = false;

                        word = removePeriod(word);
                        /* this could be an extra rule */
                        if (word.toLowerCase().endsWith("'s")){
                            word = word.substring(0, word.length()-2);
                        }
                        if (word.isEmpty())
                            continue;

                        if (word.contains("$")) {

                            finalTerm = isPrice(i, words);
                            if (i + 1 <= finalIdx && priceOrNum(removePeriod(words[i + 1])))
                                i++;
                        } else if (word.contains("%"))
                            finalTerm = isPercentage(i, words, word);

                        else if (word.toLowerCase().equals("between") && i + 3 <= finalIdx) {
                            if (isNumericValue(words[i + 1])) {
                                String num1ID = words[i + 2];
                                if (priceOrNum(num1ID)) { // number 1 has ID
                                    String[] firstArgPieces = {words[i + 1], num1ID};
                                    String firstArg = isNumber(0, firstArgPieces);

                                    if (words[i + 3].toLowerCase().equals("and")) { // continued by 'and'
                                        if (i + 4 <= finalIdx && isNumericValue(words[i + 4])) { // after and is numeric
                                            if (i + 5 <= finalIdx && priceOrNum(words[i + 5])) { // checking for ID
                                                String[] secondArgPieces = {words[i + 4], words[i + 5]};
                                                String secondArg = "" + isNumber(0, secondArgPieces);

                                                finalTerm = word + " " + firstArg + " and " + secondArg;
                                                i = i + 5;
                                            } else {
                                                finalTerm = word + " " + firstArg + " and " + words[i + 4];
                                                i = i + 4;
                                            }
                                        }
                                    }
                                } else if (words[i + 2].toLowerCase().equals("and")) { // no ID, continued by and
                                    String[] firstArgPieces2 = {words[i + 1]};
                                    String firstArg2 = isNumber(0, firstArgPieces2);
                                    if (isNumericValue(words[i + 3])) {
                                        if (i + 4 <= finalIdx && priceOrNum(words[i + 4])) { // second num has ID
                                            String[] secondArgPieces2 = {words[i + 3], words[i + 4]};
                                            String secondArg2 = isNumber(0, secondArgPieces2);


                                            finalTerm = word + " " + firstArg2 + " and " + secondArg2;
                                            i = i + 4;
                                        } else {
                                            finalTerm = word + " " + firstArg2 + " and " + words[i + 3];
                                            i = i + 3;
                                        }
                                    }
                                }
                            }
                        } else if (!getMonth(word).isEmpty()) {
                            if (i + 1 <= finalIdx && isNumericValue(removePeriod(words[i + 1]))) {
                                finalTerm = isDate(i, words);
                                i++;
                            } else if (i + 1 <= finalIdx && words[i + 1].contains("-")) {
                                finalTerm = isDateRange(i, words);
                                i++;
                            } else {
                                finalTerm = word;
                                monthFix = true;
                            }
                        } else if (isNumericValue(word)) { // the first word is numeric

                            if (i + 1 <= finalIdx) {
                                String word2 = removePeriod(words[i + 1]);

                                if (word2.toLowerCase().equals("percent") || word2.toLowerCase().equals("percentage")) {// it's perecnt type
                                    Float f = Float.valueOf(word);
                                    if (f == f.intValue())
                                        finalTerm = f.intValue() + "%";
                                    else
                                        finalTerm = f +"%";
                                    i++;


                                } else if (!getMonth(word2).isEmpty()) {// it's date type
                                    finalTerm = isDate(i, words);
                                    i++;

                                } else if (priceOrNum(word2)) { // it's price or number. WON'T be range since range contains '-'
                                    if (i + 2 <= finalIdx) { // has 3rd word
                                        String word3 = removePeriod(words[i + 2].toLowerCase());
                                        if (word3.equals("dollars")) {// word 3 is dollars
                                            finalTerm = isPrice(i, words);
                                            i = i + 2;
                                        } else if (word3.equals("u.s")) { // word 3 is u.s
                                            if (i + 3 <= finalIdx && removePeriod(words[i + 3].toLowerCase()).equals("dollars")) {
                                                finalTerm = isPrice(i, words);
                                                i = i + 3;
                                            } else {
                                                finalTerm = isNumber2(i, words, word);
                                                i = i + 1;
                                            }

                                        } else { // word3 is anything else
                                            finalTerm = isNumber2(i, words, word);
                                            i = i + 1;
                                        }
                                    } else { // does not have 3rd word
                                        finalTerm = isNumber2(i, words, word);
                                        i = i + 1;
                                    }
                                } else if (word2.contains("-")) { // it's range
                                    String[] splitRange = word2.split("-");
                                    String firstArg = null;
                                    if (splitRange.length >= 2) {
                                        if (priceOrNum(splitRange[0])) { // first arg of range is number with ID
                                            String[] number = {word, splitRange[0]};
                                            firstArg = isNumber(0, number);

                                            if (isNumericValue(splitRange[1])) { // second arg of range is number
                                                if (i + 2 <= finalIdx && priceOrNum(words[i + 2])) {
                                                    String secondArg = null;
                                                    String[] number2 = {splitRange[1], words[i + 2]};
                                                    secondArg = isNumber(0, number2);

                                                    finalTerm = "" + firstArg + "-" + secondArg;
                                                    i = i + 2;
                                                } else { // second arg is number but not priceOrNum
                                                    finalTerm = firstArg + "-" + splitRange[1];
                                                    i = i + 1;
                                                }
                                            } else {
                                                finalTerm = firstArg + "-" + splitRange[1];
                                                i = i + 1;
                                            }
                                        } else {
                                            finalTerm = isNumber2(i, words, word); // do not advance idx
                                        }
                                    }
                                } else {
                                    finalTerm = isNumber2(i, words, word);
                                }
                            } else
                                finalTerm = isNumber2(i, words, word);
                        } else if (word.contains("-")) {
                            // first arg is word. need to check if second arg is number
                            String[] split3 = word.split("-");
                            if (split3.length >= 2) {
                /*
                number w/o ID. for example: 6-7
                 */
                                String firstArg = null;
                                String secondArg;
                                if (isNumericValue(split3[0]))
                                    firstArg = split3[0];

                                if (isNumericValue(split3[1])) { // second arg is numeric
                                    if (i + 1 <= finalIdx) {
                                        String word2 = removePeriod(words[i + 1]);
                                        if (priceOrNum(word2)) {
                                            String[] number = {split3[1], word2};
                                            secondArg = isNumber(0, number);

                                            if (firstArg == null) { // type: w-m_i
                                                finalTerm = split3[0] + "-" + secondArg;
                                                i = i + 1;
                                            } else { // type: m_i-m_i
                                                finalTerm = firstArg + "-" + secondArg;
                                                i = i + 1;
                                            }
                                        } else if (!getMonth(word2).isEmpty()) {// date range case
                                            finalTerm = isDateRange(i, words);
                                        } else {
                                            finalTerm = word;
                                        }
                                    } else
                                        finalTerm = word;
                                } else
                                    finalTerm = word;
                            }
                        } else if (m_stopWords.contains(word.toLowerCase())) // STOP WORD CASE
                            continue;


                        if (finalTerm == null || monthFix) {
                            finalTerm = removePeriod(word);
                            if (finalTerm.isEmpty() || finalTerm.length()==1){
                                continue;
                            }
                            wordInsert = true;
                            String firstLetter = "" + finalTerm.charAt(0);
                            if (_stemmer) {//stemming case
                                Stemmer stm = new Stemmer();
                                char[] w = new char[50];
                                int ch = finalTerm.charAt(0);
                                if (Character.isLetter((char) ch)) {
                                    int j = 0;
                                    while (j < finalTerm.length()) {
                                        ch = finalTerm.charAt(j);
                                        ch = Character.toLowerCase((char) ch);
                                        w[j] = (char) ch;
                                        if (j < 50) j++;
                                    }
                                    for (int k = 0; k < finalTerm.length(); k++)
                                        stm.add(w[k]);

                                    stm.stem();
                                    finalTerm = stm.toString();
                                }
                            }

                            if (firstLetter.equals(firstLetter.toUpperCase())) { // upper letter case
                                if (toInsert.containsKey(finalTerm.toLowerCase())) {
                                    toInsert.get(finalTerm.toLowerCase()).add(location.get_value());
                                } else if (toInsert.containsKey(finalTerm.toUpperCase()))
                                    toInsert.get(finalTerm.toUpperCase()).add(location.get_value());
                                else {
                                    ArrayList<Integer> termLocations = new ArrayList<>();
                                    termLocations.add(location.get_value());
                                    toInsert.put(finalTerm.toUpperCase(), termLocations);
                                }
                            } else {
                                if (toInsert.containsKey(finalTerm.toLowerCase()))
                                    toInsert.get(finalTerm.toLowerCase()).add(location.get_value());
                                else if (toInsert.containsKey(finalTerm.toUpperCase())) {
                                    ArrayList<Integer> termLoc = toInsert.remove(finalTerm.toUpperCase());
                                    termLoc.add(location.get_value());
                                    toInsert.put(finalTerm.toLowerCase(), termLoc);
                                } else {
                                    ArrayList<Integer> termLocations = new ArrayList<>();
                                    termLocations.add(location.get_value());
                                    toInsert.put(finalTerm.toLowerCase(), termLocations);
                                }
                            }
                            location.increase();
                        }

                        if (!wordInsert && finalTerm != null) {
                            finalTerm = removePeriod(finalTerm);
                            if (finalTerm.isEmpty() || finalTerm.length()==1){
                                continue;
                            }

                            if (toInsert.containsKey(finalTerm)) {
                                toInsert.get(finalTerm).add(location.get_value());
                                location.increase();
                            } else {
                                ArrayList<Integer> termLocations = new ArrayList<>();
                                termLocations.add(location.get_value());
                                toInsert.put(finalTerm, termLocations);
                                location.increase();
                            }
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                        //System.out.println("Problem at word: " + words[i]);
                        //System.out.println("At location: " + termLocation.get_value());
                    }
                }

                _afterParse.put(d);
                d.setText(null);
            } catch (Exception e) {
                System.out.println("bad");
            }

        }
    }

    /**
     * method to identify if a word fits to price/number type
     * @param word2 - the examined word
     * @return - true if it fits, false otherwise
     */
    private boolean priceOrNum(String word2) {
        switch (word2.toLowerCase()){
            case "thousand":
            case "million":
            case "trillion":
            case "billion":
            case "bn":
            case "m":
                return true;
            default:
                return false;
        }
    }

    /**
     * method to retrieve the month base on the string sended
     * @param s - the string to be converted to month
     * @return - converted month date
     */
    private String getMonth(String s) {
        switch (s.toLowerCase()){
            case "january":
            case "jan":
                return "01";
            case "february":
            case "feb":
                return "02";
            case "march":
            case "mar":
                return "03";
            case "april":
            case "apr":
                return "04";
            case "may":
                return "05";
            case "june":
            case "jun":
                return "06";
            case "july":
            case "jul":
                return "07";
            case "august":
            case "aug":
                return "08";
            case "september":
            case "sep":
                return "09";
            case "october":
            case "oct":
                return "10";
            case "november":
            case "nove":
                return "11";
            case "december":
            case "dec":
                return "12";
            default:
                return "";
        }
    }

    /**
     * method to check if a given string is of numeric value
     * @param word - the string to check
     * @return true if its numeric, false otherwise
     */
    public static boolean isNumericValue(String word) {
        try {
            Float.valueOf(word);
        }catch (NumberFormatException e){
            return false;
        }

        return true;
    }

    /**
     * a method that take care of a term identified as date
     * @param idx - the first idx of the date term
     * @param text - the entire text
     * @return - the last idx of the date term
     */
    private String isDate(int idx, String[] text){
        String word = removePeriod(text[idx]);
        String valueToReturn = "";
        if (idx + 1 < text.length) {
            //date range case
            valueToReturn = isDateRange(idx, text);
            if (valueToReturn != "")
                return valueToReturn;

            if (isNumericValue(word)) {
                int val = Float.valueOf(word).intValue();
                if (val < 10)
                    word = "0" + word;
                String month = getMonth(removePeriod(text[idx + 1]));
                if (Integer.valueOf(word) <= 31) {
                    valueToReturn = month + "-" + word;
                } else { // is this else case really necessary??
                    valueToReturn = word + "-" + month;
                }
            } else {
                String month = getMonth(word);
                String num = removePeriod(text[idx + 1]);
                int number = Integer.parseInt(num);
                if (number < 10)
                    num = "0" + num;
                // maybe check that it is a valid month ??
                if (Integer.valueOf(num) <= 31) {
                    valueToReturn = month + "-" + num;
                } else { // is this else case really necessary??
                    valueToReturn = num + "-" + month;
                }
            }
        }
        return valueToReturn;
    }

    /**
     * a method that turns a classified priceRange into a term
     * @param idx - the first idx of the date range term
     * @param text - the entire text
     * @return - the string of the term
     */
    private String isDateRange(int idx, String[] text){
        String valueToReturn = "";
        String [] startToEnd; // array that represent the range of days in the month
        String month = "";
        if (text[idx].contains("-")){// case of 12-13 May
            startToEnd = text[idx].split("-");
            if (startToEnd.length > 1 && isNumericValue(startToEnd[0]) && isNumericValue(startToEnd[1])){
                month = getMonth(text[idx + 1]);
                if (!month.isEmpty())
                    valueToReturn = month + "/" + text[idx];
            }
        }

        else if (!getMonth(text[idx]).isEmpty()){//case of June 6-12
            if (text[idx + 1].contains("-")){
                startToEnd = text[idx + 1].split("-");
                if (startToEnd.length > 1 && isNumericValue(startToEnd[0]) && isNumericValue(startToEnd[1])){
                    month = getMonth(text[idx]);
                    valueToReturn = month + "/" + text[idx + 1];
                }
            }
        }

        return valueToReturn;
    }

    /**
     * a method that turns a classified price into a term
     * @param idx - the idx of the starting price
     * @param text - the entire text
     * @return - the idx of the final word observed
     */
    private String isPrice (int idx, String[] text) throws Exception {
        String valueToReturn = "";
        ArrayList<String> priceTerm = new ArrayList<String>();

        try {
            String temp = "";
            int listIdx = idx;
            int val = 0;
            boolean isInt = false;

            while (listIdx < text.length  && priceTerm.size() < 4){
                priceTerm.add(removePeriod(text[listIdx]));
                listIdx++;
            }
            //case of $ in the beginning of a price, or a price range
            if (priceTerm.get(0).contains("$")) {
                temp = isPriceRange(priceTerm);
                if(temp.isEmpty() && priceTerm.get(0).contains("-")) {
                    ArrayList<String> tempList = new ArrayList<>();
                    temp = priceTerm.get(0);
                    String [] tempArr = temp.split("-");
                    for (String s: tempArr
                         ) {
                        tempList.add(s);
                    }
                    for (int i = 1; i < priceTerm.size(); i++) {
                        tempList.add(priceTerm.get(i));
                    }
                    priceTerm = tempList;
                    temp = "";
                }
                if(priceTerm.get(0).contains("$") && temp.isEmpty()) {
                        temp = priceTerm.get(0);
                        priceTerm.set(0, temp.substring(1));
                    }
                    else
                    return temp;
            }
            float value = Float.valueOf(priceTerm.get(0));
            // checking the next word to calculate the true value
            if (priceTerm.size() >= 2) {
                String secondWord = removePeriod(priceTerm.get(1).toLowerCase());

                if ("thousand".equals(secondWord)) {
                    value = value * 1000;

                } else if ("million".equals(secondWord) || "m".equals(secondWord)) {
                    value = value * 1000000;

                } else if ("billion".equals(secondWord) || "bn".equals(secondWord)) {
                    value *= 1000000000;

                } else if ("trillion".equals(secondWord) || "tn".equals(secondWord)) {
                    value *= 1000000000;
                    value *= 1000;
                }
            }


            if (value < 1000000){
                val = (int)value;
                if (val == value)
                    isInt = true;
                if (priceTerm.size() > 1 && priceTerm.get(1).contains("/")){
                    if (isInt) {
                        valueToReturn = val + " " + priceTerm.get(1) + " Dollars";
                    }
                    else{
                        valueToReturn = value + " " + priceTerm.get(1) + " Dollars";
                    }
                }
                else {
                    if (isInt) {
                        valueToReturn = "" + val + " Dollars";
                    }
                    else {
                        valueToReturn = "" + value + " Dollars";
                    }
                }
            }
            else if (value >= 1000000){
                value = value / 1000000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if (isInt) {
                    valueToReturn = "" + val + "M Dollars";
                }
                else{
                    valueToReturn = "" + value + "M Dollars";
                }
            }


        }catch (NumberFormatException e){
            //e.printStackTrace();
            //System.out.println("Error: given string is not a number (float)! at word: " + priceTerm.toString());
            throw new Exception();
        }catch (Exception e){
            //e.printStackTrace();
            System.out.println("Error: unknown error. at words" + priceTerm.toString());
        }
        return valueToReturn;
    }

    /**
     * a method that turns a classified priceRange into a term
     * @param s - array list of the price range term
     * @return - the string of the term
     */
    private String isPriceRange(ArrayList<String> s) {
        boolean isInt1 = false, isInt2 = false;
        String valueToReturn = "";
        if (s.get(0).contains("-")){ //a range
            String [] sArray = s.get(0).split("-");
            if (sArray.length > 1 && sArray[0].contains("$") && sArray[1].contains("$")){
                sArray[0] = sArray[0].substring(1);
                sArray[1] = sArray[1].substring(1);
                float arg1 = Float.valueOf(sArray[0]);
                float arg2 = Float.valueOf(sArray[1]);
                if (s.size() >= 2) {
                    String secondWord = removePeriod(s.get(1).toLowerCase());
                    // change value according to the next word after the range (if exist)
                    if ("thousand".equals(secondWord)) {
                        arg1 = arg1 * 1000;
                        arg2 = arg2 * 1000;

                    } else if ("million".equals(secondWord) || "m".equals(secondWord)) {
                        arg1 = arg1 * 1000000;
                        arg2 = arg2 * 1000000;

                    } else if ("billion".equals(secondWord) || "bn".equals(secondWord)) {
                        arg1 *= 1000000000;
                        arg2 *= 1000000000;

                    } else if ("trillion".equals(secondWord) || "tn".equals(secondWord)) {
                        arg1 *= 1000000000;
                        arg1 *= 1000;
                        arg2 *= 1000000000;
                        arg2 *= 1000;
                    }
                }
                // creating the term according to the value that calculated
                if (arg2 < 1000000){
                    int val1 = (int)arg1;
                    int val2 = (int)arg2;
                    if (val1 == arg1)
                        isInt1 = true;
                    if (val2 == arg2)
                        isInt2 = true;

                        if (isInt1) {
                            valueToReturn = "" + val1;
                        }
                        else {
                            valueToReturn = "" + arg1;
                        }
                        if (isInt2){
                            valueToReturn = valueToReturn + "-" + val2 + " Dollars";
                        }
                        else{
                            valueToReturn = valueToReturn + "-" + arg2 + " Dollars";
                        }
                }
                else if (arg2 >= 1000000){
                    arg1 = arg1 / 1000000;
                    arg2 = arg2 / 1000000;
                    int val1 = (int)arg1;
                    int val2 = (int)arg2;
                    if (val1 == arg1)
                        isInt1 = true;
                    if (val2 == arg2)
                        isInt2 = true;
                    if (isInt1) {
                        valueToReturn = "" + val1;
                    }
                    else{
                        valueToReturn = "" + arg1;
                    }
                    if (isInt2) {
                        valueToReturn = valueToReturn + "-" + val2 + "M Dollars";
                    }
                    else{
                        valueToReturn = valueToReturn + "-" + arg2 + "M Dollars";
                    }
                }
            }
        }
        return valueToReturn;
    }

    /**
     * a method that turns a classified percentage into a term
     * @param idx - the idx of the starting percentage
     * @param text - the entire text
     * @return - the idx of the final word observed
     */
    private String isPercentage (int idx, String[] text, String word){
        String valueToReturn = "";
        try {
            if (word.contains("%")){
                valueToReturn = word;
            }
            else if (idx + 1 < text.length){// case of percent/percentage after a number
                String nextWord = removePeriod(text[idx + 1].toLowerCase());

                if ("percent".equals(nextWord) || "percentage".equals(nextWord)) {
                    valueToReturn = word + "%";
                }
            }

        }catch (NumberFormatException e){
            e.printStackTrace();
            System.out.println("Error: given string is not a number (float)");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error: unknown error");
        }

        return valueToReturn;
    }

    /**
     * a method that turns a classified number into a term
     * @param idx - the idx of the starting number
     * @param text - the entire text
     * @return - the idx of the final word observed
     */
    private String isNumber (int idx, String[] text){
        String valueToReturn = "";
        try {
            float value = Float.valueOf(removePeriod(text[idx]));
            int val = 0;
            boolean isInt = false;
            String nextWord = "";
            if (idx + 1 < text.length)
                nextWord = text[idx+1].toLowerCase();
            //calculate the true value according to the next word that comes after the number
            if ("thousand".equals(nextWord))
                value = value * 1000;

            else if ("million".equals(nextWord))
                value = value * 1000000;

            else if ("billion".equals(nextWord))
                value *= 1000000000;

            else if ("trillion".equals(nextWord)) {
                value *= 1000000000;
                value *= 1000;
            }

            if (value < 1000 && value > -1000){
                //check if is float or int
                val = (int)value;
                if (val == value)
                    isInt = true;

                if(isInt) {
                    if (nextWord.contains("/")) { //fraction case
                        valueToReturn = val + " " + nextWord;
                        idx++;
                    } else {
                        valueToReturn = "" + val;
                    }
                }
                else{
                    if (nextWord.contains("/")) {
                        valueToReturn = value + " " + nextWord;
                        idx++;
                    } else {
                        valueToReturn = "" + String.format("%.2f", value);
                    }
                }
            }
            else if (value < 1000000 && value > -1000000){
                value = value / 1000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if(isInt) {
                    valueToReturn = "" + val + "K";
                }
                else{
                    valueToReturn = "" + String.format("%.2f", value) + "K";
                }
            }
            else if (value < 1000000000 && value>-1000000000){
                value = value / 1000000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if (isInt) {
                    valueToReturn = "" + val + "M";
                }
                else{
                    valueToReturn = "" + String.format("%.2f", value) + "M";
                }
            }
            else{
                value = value / 1000000000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if (isInt) {
                    valueToReturn = "" + val + "B";
                }
                else {
                    valueToReturn = "" + value + "B";
                }
            }

        }catch (NumberFormatException e){
            e.printStackTrace();
            System.out.println("Error: given string: {" + text[idx] + "} is not a number (float)");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error: unknown error. Word: {" + text[idx] + "}");
        }

        return valueToReturn;
    }


    /**
     * a method that turns a classified number into a term
     * @param idx - the idx of the starting number
     * @param text - the entire text
     * @return - the idx of the final word observed
     */
    private String isNumber2 (int idx, String[] text, String word){
        String valueToReturn = "";
        try {
            float value = Float.valueOf(word);
            int val = 0;
            boolean isInt = false;
            String nextWord = "";
            if (idx + 1 < text.length)
                nextWord = text[idx+1].toLowerCase();
            //calculate the true value according to the next word that comes after the number
            if ("thousand".equals(nextWord))
                value = value * 1000;

            else if ("million".equals(nextWord))
                value = value * 1000000;

            else if ("billion".equals(nextWord))
                value *= 1000000000;

            else if ("trillion".equals(nextWord)) {
                value *= 1000000000;
                value *= 1000;
            }

            if (value < 1000 && value > -1000){
                //check if is float or int
                val = (int)value;
                if (val == value)
                    isInt = true;

                if(isInt) {
                    if (nextWord.contains("/")) { //fraction case
                        valueToReturn = val + " " + nextWord;
                        idx++;
                    } else {
                        valueToReturn = "" + val;
                    }
                }
                else{
                    if (nextWord.contains("/")) {
                        valueToReturn = value + " " + nextWord;
                        idx++;
                    } else {
                        valueToReturn = "" + String.format("%.2f", value);
                    }
                }
            }
            else if (value < 1000000 && value > -1000000){
                value = value / 1000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if(isInt) {
                    valueToReturn = "" + val + "K";
                }
                else{
                    valueToReturn = "" + String.format("%.2f", value) + "K";
                }
            }
            else if (value < 1000000000 && value>-1000000000){
                value = value / 1000000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if (isInt) {
                    valueToReturn = "" + val + "M";
                }
                else{
                    valueToReturn = "" + String.format("%.2f", value) + "M";
                }
            }
            else{
                value = value / 1000000000;
                val = (int)value;
                if (val == value)
                    isInt = true;
                if (isInt) {
                    valueToReturn = "" + val + "B";
                }
                else {
                    valueToReturn = "" + value + "B";
                }
            }

        }catch (NumberFormatException e){
            e.printStackTrace();
            System.out.println("Error: given string: {" + text[idx] + "} is not a number (float)");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error: unknown error. Word: {" + text[idx] + "}");
        }

        return valueToReturn;
    }


    /**
     * a method to cut special characters at the end and start of a given string if exist
     * @param word - the string
     * @return - 'word' free of affix special characters
     */
    private String removePeriod(String word){
        if (!word.isEmpty()){
            word = removePreSuffix(word);
            word = removeDoubleDash(word);
        }

        return word;
    }

    /**
     * this method remove double dash '--' in the middle of a string
     * @param word - the given string
     * @return word with double dash replaced by single dash
     */
    private String removeDoubleDash(String word) {
        boolean hasRemoved;
        do {
            hasRemoved = false;
            if (word.contains("--")){
                word = word.replaceAll("--", "-");
                hasRemoved = true;
            }
        }while (hasRemoved);

        return word;
    }

    /**
     * a method to remove prefix and suffix bad args
     * @param word - the given word
     * @return the same word w\o affix
     */
    private String removePreSuffix(String word){
        boolean hasRemoved = true;
        while (hasRemoved){
            hasRemoved = false;
            for (String bad : m_badAffix){
                if (word.startsWith(bad)){
                    word = word.substring(bad.length());
                    hasRemoved = true;
                }
                if (word.endsWith(bad)){
                    word = word.substring(0, word.length() - bad.length());
                    hasRemoved = true;
                }
            }
            if (word.startsWith("-") && !isNumericValue(word.substring(1))){
                word = word.substring(1);
            }
        }


        return word;
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
        parseDocument();
    }

}