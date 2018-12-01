package MVVM;

public class ViewModel {

    private Model m = new Model();

    public void execute(String [] details) {
        m.execute(details);
    }

    public void reset(String[] details) {
        m.reset(details);
    }

    public void loadDict(String stemming) {
        m.loadDict(stemming);
    }

    public void showDict(String stemming) {
        m.showDict(stemming);
    }
}
