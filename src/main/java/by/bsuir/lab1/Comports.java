package by.bsuir.lab1;

import javafx.scene.control.TextArea;
import jssc.SerialPort;
import jssc.SerialPortException;

public class Comports {

    public void init(SerialPort serialPort, TextArea debugArea) throws SerialPortException {
        try {
            debugArea.appendText("Try to open " + serialPort.getPortName() + "\n");
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_1200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
        } catch (SerialPortException e) {
            debugArea.appendText("Can't open " + serialPort.getPortName() + "\n");
            throw e;
        }
        debugArea.appendText(serialPort.getPortName() + " is open\n");
    }
}
