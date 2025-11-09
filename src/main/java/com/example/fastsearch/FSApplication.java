package com.example.fastsearch;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FSApplication extends Application {

    public final ExecutorService executor = Executors.newFixedThreadPool(8);

    private static FSApplication instance;
    public final ConfigState STATE = new ConfigState();

    private Stage Mystage;

    public FSApplication() {
        instance = this;
    }

    public static FSApplication getInstance() {
        return instance;
    }

    private Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        this.Mystage = stage;
        //Mystage.setTitle("Fast Search");
        Mystage.setIconified(false);
        loadScene("hello-view.fxml", 400, 500);
        Mystage.setResizable(false);
        Mystage.show();
    }


    public void loadScene(String sceneName, int width, int height) throws IOException {
        Mystage.setScene(new Scene(new FXMLLoader(FSApplication.class.getResource(sceneName)).load(), width, height));
    }
}
