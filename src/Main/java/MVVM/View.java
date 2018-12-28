package MVVM;

import FileReading.ReadFile2;
import Indexing.TermData;
import Searching.Query;
import Searching.RetrievedDocument;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.util.*;

public class View implements Observer {

    private ViewModel vm;

    @FXML
    //controllers for part A (indexing)
    public TextField corpus;
    public TextField dictpost;
    public CheckBox stemming;

    public javafx.scene.control.ChoiceBox _languageChoice;
    public ObservableList<String> _languagesList= FXCollections.observableArrayList();
    public ListView listView;

    //Controllers and data structures needed for city selection
    ArrayList<String> citiesSelected = new ArrayList<>();
    ListView<CityFilter2> cityListView = new ListView<>();
    public Button selectAll;
    public Button deselectAll;
    public Button confirm;

    //Controllers for part B (searching + results)
    public TextField tf_enterQuery;
    public TextField tf_loadQueryFile;
    public ListView resultsListView = new ListView();
    public BorderPane bp_results;
    public TextArea textResults;
    public ComboBox cb_queries;
    public Button btn_showText;
    private String lastSelected; //to get last hyperlink selected


    public void setVm(ViewModel vm) {
        this.vm = vm;
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent -mouse click event
     */
    public void corpusChoose(ActionEvent actionEvent) {

        actionEvent.consume();
        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                corpus.setText("");
            }else{
                corpus.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error opening directory chooser. Info: " + e.getMessage());
        }
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent - mouse click event
     */
    public void outputChoose(ActionEvent actionEvent) {
        actionEvent.consume();
        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                dictpost.setText("");
            }else{
                dictpost.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error opening directory chooser. Info: " + e.getMessage());
        }
    }

    /**
     * this method will execute the user input
     * @param actionEvent - mouse click
     */
    public void executeEvent(ActionEvent actionEvent) {

        actionEvent.consume();
        try {
            String [] details = new String[3];
            details[0] = String.valueOf(stemming.isSelected());
            details[1] = corpus.getText();
            details[2] = dictpost.getText();
            if ( details[1] == null||details[1].isEmpty()
                     ||
                    details[0]==null || details[0].isEmpty()){
                popProblem("Please specify both:\nCorpus directory\nWriting path");
                return;
            }

            vm.execute(details);
            HashSet<String> languagesFound = vm.getLanguages();
            if (languagesFound!=null){
                TreeSet<String> sorted = new TreeSet<>(languagesFound);
                for (String s:
                        sorted){
                    if (!s.matches(".*\\d+.*")){
                        _languagesList.add(s);
                    }
                }
                _languageChoice.setItems(_languagesList);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this method will send to the view model a request for resetting from the user
     * @param actionEvent - mouse click
     */
    public void resetEvent(ActionEvent actionEvent) {

        actionEvent.consume();
        try {
            String [] details = new String[2];
            details[0] = corpus.getText();
            details[1] = dictpost.getText();

            vm.reset(details);
            stemming.setSelected(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this method will send to the view model a request to load the dictionary
     * @param actionEvent - mouse click
     */
    public void loadDictEvent(ActionEvent actionEvent) {

        actionEvent.consume();
        try {
            String[] args = {String.valueOf(stemming.isSelected()), dictpost.getText()};
            vm.loadDict(args);
        } catch (Exception e) {
            System.out.println("Error while loading dictionary. Info: " + e.getMessage());
        }
    }

    /**
     * this method will send to the view model a request to view the dictionary
     * @param actionEvent - mouse click
     */
    public void showDictEvent(ActionEvent actionEvent) {

        Stage stage=new Stage();
        HashMap<String, TermData> unsortedDict = vm.getTermDict(String.valueOf(stemming.isSelected()), dictpost.getText());
        if (unsortedDict == null){
            popProblem("Dictionary not found\nExecute the program\nor\nLoad existing dictionary");
            actionEvent.consume();
            return;
        }

        listView = new ListView();

        TreeMap<String, TermData> sorted = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        sorted.putAll(unsortedDict);
        for (String s : sorted.keySet()) {
            listView.getItems().add("Term:  " + s + "     Total TF:  "+ unsortedDict.get(s).getM_totalTF());
        }

        Scene scene=new Scene(new Group());
        stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
        final VBox vBox=new VBox();
        vBox.setSpacing(5);
        vBox.setPadding(new Insets(10,0,0,10));
        vBox.setPrefWidth(350);
        vBox.getChildren().addAll(listView);
        vBox.setAlignment(Pos.CENTER);


        Group group=((Group) scene.getRoot());
        group.getChildren().addAll(vBox);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public void update(java.util.Observable o, Object arg) {
        if (o == vm){
            if (arg instanceof String){
                String info = (String) arg;
                if (info.contains("Error")){
                    popProblem(info);
                }
                else{
                    popInfo(info);
                }
            }
        }
    }

    /**
     * generic method to pop problems
     * @param info - info that will be displayed
     */
    private void popInfo(String info) {
        Alert prob = new Alert(Alert.AlertType.INFORMATION);
        DialogPane dialogPane = prob.getDialogPane();
        //dialogPane.getStylesheets().add(getClass().getResource("ViewStyle.css").toExternalForm());
        //dialogPane.getStyleClass().add("myDialog");

        prob.setContentText(info);
        prob.showAndWait();
    }

    /**
     * a method to pop errors with a description
     *
     * @param description - of the error occurred
     */
    private void popProblem(String description) {
        Alert prob = new Alert(Alert.AlertType.ERROR);
        DialogPane dialogPane = prob.getDialogPane();
        //dialogPane.getStylesheets().add(getClass().getResource("ViewStyle.css").toExternalForm());
        //dialogPane.getStyleClass().add("myDialog");

        prob.setContentText(description);
        prob.showAndWait();
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent - mouse click
     */
    public void queryChoose(ActionEvent actionEvent) {

        actionEvent.consume();
        try {
            FileChooser fileChooser = new FileChooser();
            File selectedFile =
                    fileChooser.showOpenDialog(Main.pStage);

            if(selectedFile == null){
                tf_loadQueryFile.setText("");
            }else{
                tf_loadQueryFile.setText(selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error while choosing file. Info: " + e.getMessage());
        }
    }

    /**
     *this method will allow the user to choose city/cities to retrieve documents from
     * @param actionEvent - mouse click
     */
    public void cityChoose(ActionEvent actionEvent){
        actionEvent.consume();
        try {
            TreeSet<String> cities = vm.getCities(dictpost.getText(), stemming.isSelected());
            if (cities != null) {
                Stage cityStage = new Stage();
                cityStage.setTitle("City Chooser");

                for (String city : cities) {
                    CityFilter2 item = new CityFilter2(city, false);

                    item.onProperty().addListener((obs, wasOn, isNowOn) -> {
                        if (isNowOn && !citiesSelected.contains(item.getName()))
                            citiesSelected.add(item.getName());
                        else if (!isNowOn && citiesSelected.contains(item.getName()))
                            citiesSelected.remove(item.getName());
                    });

                    cityListView.getItems().add(item);
                }

                //set checkbox cell
                cityListView.setCellFactory(CheckBoxListCell.forListView(new Callback<CityFilter2, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(CityFilter2 item) {
                        if (citiesSelected.contains(item.getName()))
                            item.setOn(true);
                        else
                            item.setOn(false);
                        return item.onProperty();
                    }
                }));

                //set the controllers
                selectAll = new Button("Select All");
                deselectAll = new Button("Deselect All");
                confirm = new Button("Confirm");
                deselectAll.setMaxWidth(200);
                BorderPane root = new BorderPane();
                root.setLeft(cityListView);
                VBox buttons = new VBox();
                buttons.setPadding(new Insets(20, 20, 20, 20));
                buttons.getChildren().addAll(selectAll, deselectAll, confirm);
                buttons.setSpacing(20);
                root.setCenter(buttons);

                //select all cities
                selectAll.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        citiesSelected.clear();
                        for (String city : cities) {
                            CityFilter2 item = new CityFilter2(city, true);
                            citiesSelected.add(item.getName());
                            cityListView.setCellFactory(CheckBoxListCell.forListView(new Callback<CityFilter2, ObservableValue<Boolean>>() {
                                @Override
                                public ObservableValue<Boolean> call(CityFilter2 item) {
                                    item.onProperty().set(true);
                                    return item.onProperty();
                                }
                            }));
                        }
                    }
                });

                //deselect all cities
                deselectAll.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        citiesSelected.clear();

                        for (String city : cities) {
                            CityFilter2 item = new CityFilter2(city, false);

                            cityListView.setCellFactory(CheckBoxListCell.forListView(new Callback<CityFilter2, ObservableValue<Boolean>>() {
                                @Override
                                public ObservableValue<Boolean> call(CityFilter2 item) {
                                    item.onProperty().set(false);
                                    return item.onProperty();
                                }
                            }));
                        }
                    }
                });

                //getting the selected cities from the user
                confirm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        cityStage.close();
                    }
                });

                Scene scene = new Scene(root, 420, 400);
                cityStage.setScene(scene);
                scene.getStylesheets().add(getClass().getResource("/ViewStyle.css").toExternalForm());
                cityStage.showAndWait();
            }
           /* else{
                popProblem("Please load dictionary before choosing city"); // ?
            }*/
            } catch(Exception e){
                e.printStackTrace();
            }
    }


    public void runQuery (MouseEvent actionEvent){
        actionEvent.consume();

        PriorityQueue<RetrievedDocument> retrievedDocuments = new PriorityQueue<>();
        ArrayList<Hyperlink> hyperlinks = new ArrayList<>();

        if (corpus.getText().isEmpty() || dictpost.getText().isEmpty()) {
            popProblem("Enter corpus path and output directory!");
            return;
        }

        if (tf_enterQuery.getText().isEmpty() && tf_loadQueryFile.getText().isEmpty()) {
            popProblem("Enter query or load query file to run!");
            return;
        }
        else if(!tf_enterQuery.getText().isEmpty() && !tf_loadQueryFile.getText().isEmpty()){
            popProblem("Enter query OR load query file, you can not enter both!");
            return;
        }

        else if (!tf_enterQuery.getText().isEmpty() && tf_loadQueryFile.getText().isEmpty()){
            retrievedDocuments = vm.processQuery(tf_enterQuery.getText(), citiesSelected, stemming.isSelected(), corpus.getText() + '\\');
        }

        else if (tf_enterQuery.getText().isEmpty() && !tf_loadQueryFile.getText().isEmpty()){
            runQueryFile(tf_loadQueryFile.getText() + '\\');
            return;
        }

        if (retrievedDocuments == null){
            return;
        }

        final PriorityQueue<RetrievedDocument> finRetrievedDocuments = retrievedDocuments;//for handle method

        try{
            Stage resultStage = new Stage();
            resultStage.setTitle("IR 2019");
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/resultsView.fxml"));
            Parent root = fxml.load();
            View newController = fxml.getController();
            bp_results = new BorderPane();
            textResults = new TextArea();
            cb_queries = new ComboBox();
            btn_showText = new Button();
            newController.cb_queries.setDisable(true);
            newController.cb_queries.setVisible(false);
            for (RetrievedDocument retDoc: finRetrievedDocuments) {
                hyperlinks.add(new Hyperlink(retDoc.get_docName()));
            }

            newController.resultsListView.getItems().addAll(hyperlinks);

            for (Hyperlink hl: hyperlinks){
                hl.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent t) {
                        String docName = hl.getText();
                        for (RetrievedDocument rd : finRetrievedDocuments) {
                            if (rd.get_docName().equals(docName)) {
                                newController.textResults.setText(rd.get_strongEntities().toString());
                                newController.lastSelected = ReadFile2.getTextFromDoc
                                        (corpus.getText() + '\\' + rd.get_file(), rd.get_startLine(), rd.get_endLine());
                                break;
                            }
                        }
                    }
                });
            }

            newController.btn_showText.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    newController.textResults.clear();
                    newController.textResults.setText(newController.lastSelected);
                }
            });

            Scene scene = new Scene(root, 800, 450);
            scene.getStylesheets().add(getClass().getResource("/ViewStyle.css").toExternalForm());
            resultStage.setScene(scene);
            resultStage.show();
        }

        catch (Exception e){
            e.printStackTrace();
        }


    }


    private void runQueryFile(String path){

        Map <Query, PriorityQueue<RetrievedDocument>> queries = vm.processQueryByFile
                (path, citiesSelected, stemming.isSelected(), corpus.getText() + '\\');
        if (queries==null){
            return;
        }
        ArrayList<Hyperlink> hyperlinks = new ArrayList<>();
        HashMap<String, PriorityQueue<RetrievedDocument>> stringQueries = new HashMap<>();
        for (Query q: queries.keySet()) {
            stringQueries.put(q.get_title(), queries.get(q));
        }

        try{
            Stage resultStage = new Stage();
            resultStage.setTitle("IR 2019");
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/resultsView.fxml"));
            Parent root = fxml.load();
            View newController = fxml.getController();
            bp_results = new BorderPane();
            textResults = new TextArea();
            cb_queries = new ComboBox();
            newController.cb_queries.setDisable(false);
            newController.cb_queries.setPromptText("Choose query");

            for (Query q: queries.keySet())
                newController.cb_queries.getItems().add(q.get_title());

            newController.cb_queries.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                    newController.resultsListView.getItems().clear();
                    newController.textResults.clear();
                    hyperlinks.clear();
                    PriorityQueue<RetrievedDocument> results = stringQueries.get(newController.cb_queries.getSelectionModel().getSelectedItem());
                  
                    if (results != null) {
                        for (RetrievedDocument rd : results) {
                            hyperlinks.add(new Hyperlink(rd.get_docName()));
                        }
                        newController.resultsListView.getItems().addAll(hyperlinks);

                        for (Hyperlink hl : hyperlinks) {
                            hl.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent t) {
                                    String docName = hl.getText();
                                    for (RetrievedDocument rd : results) {
                                        if (rd.get_docName().equals(docName)) {
                                            newController.textResults.setText(rd.get_strongEntities().toString());
                                            newController.lastSelected = ReadFile2.getTextFromDoc
                                                    (corpus.getText() + '\\' + rd.get_file(), rd.get_startLine(), rd.get_endLine());
                                            break;
                                        }
                                    }
                                }
                            });
                        }

                        newController.btn_showText.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent e) {
                                newController.textResults.clear();
                                newController.textResults.setText(newController.lastSelected);
                            }
                        });
                    }
                    else {
                        newController.resultsListView.getItems().clear();
                        newController.resultsListView.getItems().add("No results were" + '\n' + "returned by the query");
                    }
                }
            });


            Scene scene = new Scene(root, 800, 450);
            scene.getStylesheets().add(getClass().getResource("/ViewStyle.css").toExternalForm());
            resultStage.setScene(scene);
            resultStage.show();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}

