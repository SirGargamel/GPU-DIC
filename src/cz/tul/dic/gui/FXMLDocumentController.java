/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

/**
 *
 * @author Petr Jecmen
 */
public class FXMLDocumentController implements Initializable {

//    @FXML
//    private Label label;

    @FXML
    private void handleButtonAction(ActionEvent event) throws IOException {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // TODO
//            Computation.commenceComputationStatic();
            Computation.commenceComputationDynamic();
        } catch (IOException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
