package Indexing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class CityDetails implements Serializable {
    private String _country;
    private String _currency;
    private String _population;
    private HashMap<String, ArrayList<Integer>> m_docs;

    public CityDetails(String m_country, String m_currency, String m_population, HashMap<String, ArrayList<Integer>> m_docs) {
        this._country = m_country;
        this._currency = m_currency;
        this._population = m_population;
        this.m_docs = m_docs;
    }

    public HashMap<String, ArrayList<Integer>> getM_docs() {
        return m_docs;
    }

    public String get_country() {
        return _country;
    }

    public String get_currency() {
        return _currency;
    }

    public String get_population() {
        return _population;
    }
}
