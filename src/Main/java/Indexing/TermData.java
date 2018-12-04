package Indexing;

import java.io.Serializable;

public class TermData implements Serializable {

    int m_df;
    float m_idf;
    int m_totalTF;

    long m_pointer;

    public int getM_df() {
        return m_df;
    }

    public float getM_idf() {
        return m_idf;
    }

    public int getM_totalTF() {
        return m_totalTF;
    }

    public long getM_pointer() {
        return m_pointer;
    }

    public TermData(int m_df, int m_totalTF) {
        this.m_df = m_df;
        this.m_totalTF = m_totalTF;
    }
}
