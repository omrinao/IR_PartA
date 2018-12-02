package MVVM;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static Stage pStage;

    @Override
    public void start(Stage primaryStage) throws Exception{

        pStage = primaryStage;

        System.out.println(getClass().getResource(""));
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("../MainView.fxml"));
        Parent root = fxml.load();
        primaryStage.setTitle("IR 2019");
        Scene scene = new Scene(root, 750, 500);
        scene.getStylesheets().add(getClass().getResource("../ViewStyle.css").toExternalForm());
        primaryStage.setScene(scene);

        // --------- setting up the MVVM --------
        Model model = new Model();
        ViewModel viewModel = new ViewModel();
        View view = fxml.getController();

        model.addObserver(viewModel);
        viewModel.addObserver(view);


        // ------------ ready to show
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
