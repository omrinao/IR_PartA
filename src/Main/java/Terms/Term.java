package Terms;

public class Term {

    protected String m_value;
    protected String m_docNo;
    protected String m_location;

    public Term(String m_value, String m_docNo, String m_location) {
        this.m_value = m_value;
        this.m_docNo = m_docNo;
        this.m_location = m_location;
    }

    public String getValue() {
        return m_value;
    }

    public String getDocNo() {
        return m_docNo;
    }

    public String getLocation() {
        return m_location;
    }


    public void setValue(String s) {
        this.m_value = s;
    }
}
