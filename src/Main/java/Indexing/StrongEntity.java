package Indexing;

public class StrongEntity implements Comparable {
    private float _rank;
    private String _term;

    public StrongEntity(String _term, float _rank) {
        this._rank = _rank;
        this._term = _term;
    }

    public StrongEntity() {
    }

    public float get_rank() {
        return _rank;
    }

    public void set_rank(float _rank) {
        this._rank = _rank;
    }

    public String get_term() {
        return _term;
    }

    public void set_term(String _term) {
        this._term = _term;
    }

    /**
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(Object o) {
        StrongEntity other = (StrongEntity) o;

        return (int) (other._rank-_rank);
    }
}
