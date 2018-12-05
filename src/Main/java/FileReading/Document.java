package FileReading;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Document {

    private String m_docNum;
    private String m_title;
    private String m_city;
    private String m_language;
    private String m_text;
    private int m_length;
    private boolean m_isFinal;

    private short _startLine;
    private short _endLine;
    private String _path;

    private LinkedHashMap<String, ArrayList<Integer>> m_terms;
    private ArrayList<Integer> m_cityTerms;

    /**
     * c'tor of full doc
     * @param fullDoc - string that represents a full document
     */
    public Document(String fullDoc, short start, short end, int id, String path) {
        m_terms = new LinkedHashMap<>();
        m_cityTerms = new ArrayList<>();
        m_city = "";
        m_text = "";
        m_title = "";
        m_isFinal = false;
        _startLine = start;
        _endLine = end;
        m_docNum = "" + id;
        m_language = "";
        _path = path;
        m_length = 0;

        int textTag = fullDoc.indexOf("<TEXT>");
        int textEndTag = fullDoc.indexOf("</TEXT>");
        if (textTag != -1 &&
                textEndTag!= -1 &&
                textTag < textEndTag){
            extractHeaders(fullDoc.substring(0, textTag));
            extractText(fullDoc.substring(textTag+6 , textEndTag));
        }
        else if (textTag != -1){ // if no text tag was found, text is an empty string
            extractHeaders(fullDoc.substring(0,textTag));
            m_text = "";
        }

    }

    /**
     * a method to extract the text and language of a document
     * @param substring - substring of all string inside text tag
     */
    private void extractText(String substring) {

        int start = substring.indexOf("<F P=104>");
        int end = substring.indexOf("</F P=104>");
        if (start != -1 && start>end)
            end = start + findFTag(substring.substring(start));

        if (start!=-1 &&
                end!=-1 &&
                start < end){
            String cityVal = substring.substring(start + 9, end).trim();
            String[] parsedCities = cityVal.split("\\s+");
            m_city = parsedCities[0];
        }

        start = substring.indexOf("<F P=105>");
        end = substring.indexOf("</F P=105>");
        if (start != -1 && start>end)
            end = start + findFTag(substring.substring(start));

        if (start!=-1 &&
                end!=-1 &&
                start < end){
            m_language = substring.substring(start+9, end);
            m_language= m_language.replaceAll("\\s+", "");
        }

        if (end != -1){
            m_text = substring.substring(end+4);
        }
        else
            m_text = substring;
    }

    /**
     * a method to extract headers from full text
     * @param sub - substring of the full document containing the headers
     */
    private void extractHeaders(String sub) {
        /* int startIdx = sub.indexOf("<DOCNO>");
        int endIdx = sub.indexOf("</DOCNO>");
        if (startIdx!=-1 &&
                endIdx!=-1 &&
                startIdx < endIdx)
            m_docNum = sub.substring(startIdx + 7,endIdx).trim();
        else
            m_docNum = "";
        */

        int startIdx = sub.indexOf("<DATE1>");
        int endIdx = sub.indexOf("</DATE1>");

        startIdx = sub.indexOf("<TI>");
        endIdx = sub.indexOf("</TI>");
        if (startIdx!=-1 && endIdx!=-1 && startIdx < endIdx)
            m_title = sub.substring(startIdx + 4,endIdx).trim();
        else
            m_title = "";

        startIdx = sub.indexOf("<F P=104>");
        endIdx = sub.indexOf("</F P=104>");
        if (endIdx == -1 && startIdx != -1){
            String temp = sub.substring(startIdx);
            endIdx = startIdx + findFTag(temp);
        }
        if (startIdx != -1 && endIdx != -1 && startIdx<endIdx){
            String cityVal = sub.substring(startIdx + 9, endIdx).trim();
            String[] parsedCities = cityVal.split("\\s+");
            m_city = parsedCities[0];
        }

        startIdx = sub.indexOf("<F P=105>");
        endIdx = sub.indexOf("</F");
        if (startIdx!=-1 && startIdx>endIdx)
            endIdx = startIdx + findFTag(sub.substring(startIdx));

        if (m_language.isEmpty() &&
                startIdx!=-1 &&
                endIdx!=-1 &&
                startIdx < endIdx){
            m_language = sub.substring(startIdx+9, endIdx);
            m_language = m_language.replaceAll("\\s+", "");
        }

    }


    public Document(String name){

    }

    private int findFTag(String sub) {
        return sub.indexOf("</F>");
    }

    public int getMaxTF(){
        int maxTF = 0;
        for (ArrayList<Integer> list :
                m_terms.values()){
            if (list.size() > maxTF){
                maxTF = list.size();
            }
        }

        return maxTF;
    }

    public void setFinal (boolean isLast){
        this.m_isFinal = isLast;
    }

    public boolean getFinal(){return m_isFinal;}

    public String getDocNum() {
        return m_docNum;
    }

    public String getTitle() {
        return m_title;
    }

    public String getText() {
        return m_text;
    }

    public void setText(String text){
        this.m_text = text;
    }


    public String getCity() {
        return m_city;
    }

    public String getLanguage() {
        return m_language;
    }

    public int getLength(){
        return m_length;
    }

    public void calcLength(){
        for (ArrayList<Integer> arr :
                m_terms.values()){
            m_length += arr.size();
        }
    }

    public LinkedHashMap<String, ArrayList<Integer>> getTermsMap() {
        return m_terms;
    }

    public ArrayList<Integer> getCityTerms() {
        return m_cityTerms;
    }

    public short get_startLine(){return _startLine;}

    public short get_endLine(){return _endLine;}

    public String get_path(){return _path;}

    public void setLanguage(String language){
        if (!language.isEmpty() &&
                (language.charAt(0) > 90 || language.charAt(0) < 65)){
            m_language = "";
        }
        else{
            this.m_language = language;
        }
    }


}