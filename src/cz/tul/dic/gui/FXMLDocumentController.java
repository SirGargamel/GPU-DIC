/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

/**
 *
 * @author Petr Jecmen
 */
public class FXMLDocumentController implements Initializable {
    
    private static final File DEFAULT_DIR = new File("D:\\temp");

    @FXML
    private Button buttonDyn;

    @FXML
    private void handleButtonActionDyn(ActionEvent event) throws IOException {
        Computation.commenceComputationDynamic(selectInput());        
    }
    
    @FXML
    private void handleButtonActionStatic(ActionEvent event) throws IOException {
        Computation.commenceComputationStatic(selectInput());
    }
    
    private Object selectInput() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(DEFAULT_DIR);        
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.bmp, *.jpg)", "*.bmp", "*.jpg"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        final Object result;
        if (fileList != null && fileList.size() == 1) {
            result = fileList.get(0);
        } else {
            result = fileList;
        }
        return result;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {        
    }

}
