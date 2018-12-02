package Indexing;

import java.util.ArrayList;
import java.util.HashMap;

public class CityDetails {
    private String m_country;
    private String m_currency;
    private String m_population;
    private HashMap<String, ArrayList<Integer>> m_docs;

    public CityDetails(String m_country, String m_currency, String m_population, HashMap<String, ArrayList<Integer>> m_docs) {
        this.m_country = m_country;
        this.m_currency = m_currency;
        this.m_population = m_population;
        this.m_docs = m_docs;
    }

    public HashMap<String, ArrayList<Integer>> getM_docs() {
        return m_docs;
    }
}
