package FileReading;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Document {

    /*
    TODO: finish extract headers method
    TODO: make getters and setters and make some tests
      */
    private String m_docNum;
    private String m_date;
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
        m_date = "";
        m_isFinal = false;
        _startLine = start;
        _endLine = end;
        m_docNum = "" + id;
        _path = path;

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
        int start = substring.indexOf("<F P=105>");
        int end = substring.indexOf("</F>");
        if (start!=-1 &&
                end!=-1 &&
                start < end){
            m_language = substring.substring(start+8, end);
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
        if (startIdx!=-1 && endIdx!=-1 && startIdx<endIdx)
            m_date = sub.substring(startIdx + 7,endIdx).trim();
        else
            m_date = "";

        startIdx = sub.indexOf("<TI>");
        endIdx = sub.indexOf("</TI>");
        if (startIdx!=-1 && endIdx!=-1 && startIdx < endIdx)
            m_title = sub.substring(startIdx + 4,endIdx).trim();
        else
            m_title = "";

        startIdx = sub.indexOf("<F P=104>");
        endIdx = sub.indexOf("</F P=104>");
        if (startIdx != -1 && endIdx != -1 && startIdx<endIdx){
            String cityVal = sub.substring(startIdx + 9, endIdx).trim();
            String[] parsedCities = cityVal.split(" ");
            if (parsedCities.length >= 2){
                m_city = parsedCities[0] + parsedCities[1];
            }
            else
            {
                m_city = parsedCities[0];
            }
        }

    }

    public Document(String name){

    }

    public void setFinal (boolean isLast){
        this.m_isFinal = isLast;
    }

    public boolean getFinal(){return m_isFinal;}

    public String getDocNum() {
        return m_docNum;
    }

    public String getDate() {
        return m_date;
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

    public LinkedHashMap<String, ArrayList<Integer>> getTermsMap() {
        return m_terms;
    }

    public ArrayList<Integer> getCityTerms() {
        return m_cityTerms;
    }

    public void setLength(int length){
        this.m_length = length;
    }



}