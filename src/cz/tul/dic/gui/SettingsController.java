/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.data.AppSettings;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.output.color.ColorMap;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * FXML Controller class
 *
 * @author Petr Ječmen
 */
public class SettingsController implements Initializable {

    @FXML
    private ComboBox<AbstractOpenCLMemoryManager.Type> comboMemory;
    @FXML
    private ComboBox<ColorMap.Type> comboColor;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        final ObservableList<AbstractOpenCLMemoryManager.Type> comboMemoryData = FXCollections.observableArrayList();
        comboMemoryData.addAll(AbstractOpenCLMemoryManager.Type.values());
        comboMemory.setItems(comboMemoryData);
        comboMemory.valueProperty().addListener((ObservableValue<? extends AbstractOpenCLMemoryManager.Type> observable, AbstractOpenCLMemoryManager.Type oldValue, AbstractOpenCLMemoryManager.Type newValue) -> {            
            AppSettings.getInstance().setMemManagerType(newValue);
        });
        comboMemory.setConverter(new StringConverter<AbstractOpenCLMemoryManager.Type>() {

            private final Map<String, AbstractOpenCLMemoryManager.Type> data = new HashMap<>(AbstractOpenCLMemoryManager.Type.values().length);

            @Override
            public String toString(AbstractOpenCLMemoryManager.Type object) {
                final String result = Lang.getString(object.toString());
                data.put(result, object);
                return result;
            }

            @Override
            public AbstractOpenCLMemoryManager.Type fromString(String string) {
                return data.get(string);
            }
        });
        comboMemory.getSelectionModel().select(AppSettings.getInstance().getMemManagerType());
        
        final ObservableList<ColorMap.Type> comboColorData = FXCollections.observableArrayList();
        comboColorData.addAll(ColorMap.Type.values());
        comboColor.setItems(comboColorData);
        comboColor.valueProperty().addListener((ObservableValue<? extends ColorMap.Type> observable, ColorMap.Type oldValue, ColorMap.Type newValue) -> {            
            AppSettings.getInstance().setColorMapType(newValue);
        });        
        comboColor.getSelectionModel().select(AppSettings.getInstance().getColorMapType());
    }    
    
}
