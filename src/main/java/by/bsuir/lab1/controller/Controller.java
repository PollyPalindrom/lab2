package by.bsuir.lab1.controller;

import by.bsuir.lab1.Comports;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class Controller {
    @FXML
    private TextArea inputArea;
    @FXML
    private TextArea outputArea;
    @FXML
    private TextArea debugArea;
    @FXML
    private ChoiceBox choiceBox;
    @FXML
    private Button send;
    @FXML
    private Button clear;

    private static SerialPort serialPort1 = null;
    private static SerialPort serialPort2 = null;
    private static SerialPort serialPort = null;
    private String buffer = "";
    private String bufferForOut = "";
    private String buf = "";

    @FXML
    void initialize() {
        Comports comport = new Comports();
        try {
            serialPort1 = new SerialPort("COM1");
            comport.init(serialPort1, debugArea);
            serialPort = serialPort1;
            serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
        } catch (SerialPortException e) {
            try {
                serialPort2 = new SerialPort("COM2");
                comport.init(serialPort2, debugArea);
                serialPort = serialPort2;
                serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
            } catch (SerialPortException serialPortException) {
                clear.setDisable(true);
                send.setDisable(true);
                choiceBox.setDisable(true);
                inputArea.setDisable(true);
                debugArea.appendText("There is no ports to open\n ");
            }
        }
        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            try {
                serialPort.setParams(Integer.parseInt(newValue.toString()),
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            } catch (SerialPortException e) {
                debugArea.appendText("Can't set baudrate");
            }
        });
        inputArea.textProperty().addListener((observable) -> {
            String dataStr = inputArea.getText();
            for (int i = 0; i < dataStr.length(); i++) {
                if (dataStr.charAt(i) != '0' && dataStr.charAt(i) != '1') {
                    debugArea.appendText("Can't send text. Message contains not only 0 and 1\n");
                    send.setDisable(true);
                    return;
                }
            }
            send.setDisable(false);
        });
    }

    public void closePort() {
        try {
            if (serialPort != null) {
                if (serialPort.isOpened()) serialPort.closePort();
            }
            if (serialPort1 != null) {
                if (serialPort1.isOpened()) serialPort1.closePort();
            }
            if (serialPort2 != null) {
                if (serialPort2.isOpened()) serialPort2.closePort();
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }

    }


    public void buttonClicked() {
        String dataStr = inputArea.getText();
        StringBuilder newStr = new StringBuilder();
        debugArea.appendText("All symbols: " + buffer);
        buf = "";
        if (buffer.length() > 7) {
            buf = buffer.substring(buffer.length() - 7, buffer.length());
            buffer = buffer.substring(0, buffer.length() - 7);
        } else if (buffer.length() != 0) {
            buf += buffer;
            buffer = buffer.substring(0, buffer.length() - buf.length());
        }
        buf += dataStr;
        if (buf.contains("00001011") || buf.contains("00001010")) {
            String text = buf;
            int count = (text + "\0").split("00001010").length - 2 + (text + "\0").split("00001011").length;
            if ((text + "\0").split("00001010").length - 1 > 0) bitstaffing("00001010");
            if ((text + "\0").split("00001011").length - 1 > 0) bitstaffing("00001011");
            StringBuilder newStrr = new StringBuilder();
            for (int i = buf.length() - dataStr.length() - count; i < buf.length(); i++) {
                newStrr.append(buf.charAt(i));
            }
            dataStr = newStrr.toString();
        }
        buffer += buf;
        debugArea.appendText("{" + dataStr + "}" + "\n");
        dataStr += "\n";
        try {
            byte[] data = dataStr.getBytes();
            for (int i = 0; i < dataStr.length(); i++) {
                serialPort.writeByte(data[i]);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
        inputArea.clear();
    }

    public void bitstaffing(String flag) {
        StringBuilder newStr = new StringBuilder();
        String text = buf;
        int count = (text + "\0").split(flag).length - 1;
        for (int j = 0; j < count; j++) {
            for (int i = 0; i < buf.length(); i++) {
                newStr.append(buf.toCharArray()[i]);
                if (i == buf.indexOf(flag) + 6) {
                    newStr.append(0);
                }
            }
            buf = newStr.toString();
            newStr = new StringBuilder();
        }
    }

    public void clearButtonClicked() {
        outputArea.clear();
    }

    private static class Holder {
        public static final Controller INSTANCE = new Controller();
    }

    public Controller getInstance() {
        return Holder.INSTANCE;
    }

    private class PortReader implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {
            try {
                while (serialPort.getInputBufferBytesCount() != 0) {
                    byte[] data = serialPort.readBytes(1);
                    if (!new String(data).equals("\n")) bufferForOut += new String(data);
                    if (!(bufferForOut.contains("00001010") && (bufferForOut.lastIndexOf("00001010") + 7) == bufferForOut.length() - 1))
                        outputArea.appendText(new String(data));
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
    }
}