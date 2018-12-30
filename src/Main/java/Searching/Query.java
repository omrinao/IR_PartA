package Searching;

public class Query {

    private String _title;       // query title
    private String _number;      // query number
    private String _description; // query description
    private String _narrative;   // query narrative

    public Query() {
    }

    public Query(String _title, String _number, String _description, String _narrative) {

        this._title = _title;
        this._number = _number;
        this._description = _description;
        this._narrative = _narrative;
    }

    public String get_title() {
        return _title;
    }

    public void set_title(String _title) {
        this._title = _title;
    }

    public String get_number() {
        return _number;
    }

    public void set_number(String _number) {
        this._number = _number;
    }

    public String get_description() {
        return _description;
    }

    public void set_description(String _description) {
        this._description = _description;
    }

    public String get_narrative() {
        return _narrative;
    }

    public void set_narrative(String _narrative) {
        this._narrative = _narrative;
    }

    public Integer get_donNumber(){
        return Integer.valueOf(_number);
    }

    @Override
    public String toString() {
        return "Query{" +
                "_title='" + _title + '\'' +
                ", _number='" + _number + '\'' +
                ", _description='" + _description + '\'' +
                ", _narrative='" + _narrative + '\'' +
                '}';
    }
}
