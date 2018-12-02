package MVVM;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class View {

    @FXML
    public TextField corpus;
    public TextField dictpost;
    public CheckBox stemming;
    private ViewModel vm = new ViewModel();



    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent
     */
    public void directoryBrowseEvent(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                corpus.setText("No Directory selected");
            }else{
                corpus.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {

        }
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent
     */
    public void dictionaryBrowseEvent(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                dictpost.setText("No Directory selected");
            }else{
                dictpost.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {

        }
    }

    /**
     * this method will execute the user input
     * @param actionEvent
     */
    public void executeEvent(ActionEvent actionEvent) {

        try {
            String [] details = new String[3];
            details[0] = String.valueOf(stemming.isSelected());
            details[1] = corpus.getText();
            details[2] = dictpost.getText();
            vm.execute(details);
        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request for resetting from the user
     * @param actionEvent
     */
    public void resetEvent(ActionEvent actionEvent) {

        try {
            String [] details = new String[3];
            details[0] = String.valueOf(stemming.isSelected());
            details[1] = corpus.getText();
            details[2] = dictpost.getText();
            vm.reset(details);
            stemming.setSelected(false);
            corpus.setText("");
            dictpost.setText("");
        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request to load the dictionary
     * @param actionEvent
     */
    public void loadDictEvent(ActionEvent actionEvent) {

        try {
            vm.loadDict(String.valueOf(stemming.isSelected()));
        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request to view the dictionary
     * @param actionEvent
     */
    public void showDictEvent(ActionEvent actionEvent) {

        try {
            Stage stage = new Stage();
            stage.setTitle("Dictionary");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("../showDict.fxml").openStream());
            Scene scene = new Scene(root, 800, 550);
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.setResizable(false);
            vm.showDict(String.valueOf(stemming.isSelected()));
            stage.show();
        } catch (Exception e) {

        }
    }

}

