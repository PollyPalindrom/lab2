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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Controller {
    @FXML
    private TextArea inputArea;
    @FXML
    private TextArea outputArea;
    @FXML
    private TextArea debugArea;
    @FXML
    private TextArea bufferArea;
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
        checkInput();
    }

    public void checkInput() {
        inputArea.textProperty().addListener((observable) -> {
            String dataStr = inputArea.getText();
            for (int i = 0; i < dataStr.length(); i++) {
                if (dataStr.charAt(i) != '0' && dataStr.charAt(i) != '1') {
                    debugArea.appendText("Can't send text. Message contains characters that are not 0 and 1\n");
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
        debugArea.appendText("Input: " + inputArea.getText() + "\n");
        String dataStr = inputArea.getText();
        try {
            if (dataStr.length() % 11 == 0) {
                for (int i = 0; i < dataStr.length() / 11; i++) {
                    String data = dataStr.substring(i * 11, (i + 1) * 11);
//                    data = createHammingCode(data);
//                    data = generateError(data);
                    if (i != dataStr.length() / 11 - 1) collision(data, dataStr, 0, 0);
                    else collision(data, dataStr, 0, 1);
                }
            } else {
                for (int i = 0; i < dataStr.length() / 11; i++) {
                    String data = dataStr.substring(i * 11, (i + 1) * 11);
//                    data = createHammingCode(data);
//                    data = generateError(data);
                    collision(data, dataStr, 0, 0);
                }
                String newStr = "";
                for (int i = 0; i < 11 - (dataStr.length() - (dataStr.length() / 11) * 11); i++) {
                    newStr += "0";
                }
                newStr += dataStr.substring(dataStr.length() - (dataStr.length() - (dataStr.length() / 11) * 11));
                String data = newStr;
//                data = createHammingCode(data);
//                data = generateError(data);
                collision(data, dataStr, 1, 1);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
        inputArea.clear();
    }

    private void collision(String data, String dataStr, int isShort, int isLast) throws SerialPortException {
        int flg = 0;
        for (int i = 0; i < 10; ) {
            while (generateChannelOccupation() == 1) {
                debugArea.appendText("Channel is occupied\n");
            }
            debugArea.appendText("Channel is free\n");
            bufferArea.appendText("Info to send: " + data + ":");
            byte[] dataByte = data.getBytes();
            for (int j = 0; j < data.length(); j++) {
                serialPort.writeByte(dataByte[j]);
            }
            if (generateCollision() == 1) {
                debugArea.appendText("There is collision\n");
                bufferArea.appendText("*\n");
                debugArea.appendText("Jam\n");
                serialPort.writeByte((byte) '*');
                if (isShort == 1) serialPort.writeByte(String.valueOf(dataStr.length() % 11).getBytes()[0]);
                serialPort.writeByte((byte) '\n');
                i++;
                if (i > 9) {
                    flg = 1;
                } else {
                    flg = 2;
                    waitRandomTimeWindow(i + 1);
                }
            } else {
                bufferArea.appendText("\n");
                debugArea.appendText("There isn't collision\n");
                if (isShort == 1) serialPort.writeByte(String.valueOf(dataStr.length() % 11).getBytes()[0]);
                serialPort.writeByte((byte) '\n');
                if (isLast == 1) serialPort.writeByte((byte) '!');
                break;
            }
            if (flg == 1) {
                debugArea.appendText("Error\n");
                break;
            }
        }
        bufferArea.appendText("\n");
        debugArea.appendText("End of transmission\n");
    }

    public static void pause(int s) {
        try {
            TimeUnit.MILLISECONDS.sleep(s * 10L);
        } catch (InterruptedException e) {
            System.err.format("IOException: %s%n", e);
        }
    }

    private int generateNumberOfTimeSlots(int n) {
        Integer k = Integer.min(n, 10);
        Integer m = (int) Math.pow(2, k);
        Random rand = new Random();
        return rand.nextInt(m + 1);
    }

    private void waitRandomTimeWindow(int n) {
        debugArea.appendText("Waiting\n");
        pause(generateNumberOfTimeSlots(n));
    }

    private Integer generateChannelOccupation() {
        int occupation = 0;
        Random rand = new Random();
        int x = 1 + rand.nextInt(20 - 1 + 1);
        if (x < 8) {
            occupation = 1;
        }
        return occupation;
    }

    private Integer generateCollision() {
        int collision = 0;
        Random rand = new Random();
        int x = 1 + rand.nextInt(20 - 1 + 1);
        if (x % 2 == 0) {
            collision = 1;
        }
        return collision;
    }

    private String generateError(String data) {
        Random rand = new Random();
        int x = 1 + rand.nextInt(20 - 1 + 1);
        if (x % 2 == 0) {
            debugArea.appendText("single error was generated\n");
            int y = rand.nextInt(data.length() - 1 + 1);
            if (data.toCharArray()[y] == '0') {
                data = data.substring(0, y) + "1" + data.substring(y + 1);
            } else {
                data = data.substring(0, y) + "0" + data.substring(y + 1);
            }
        } else {
            if (x < 8) {
                debugArea.appendText("double error was generated\n");
                int y = rand.nextInt(data.length() - 1 + 1);
                if (data.toCharArray()[y] == '0') {
                    data = data.substring(0, y) + "1" + data.substring(y + 1);
                } else {
                    data = data.substring(0, y) + "0" + data.substring(y + 1);
                }
                int z = rand.nextInt(data.length() - 1 + 1);
                if (z != y) {
                    if (data.toCharArray()[z] == '0') {
                        data = data.substring(0, z) + "1" + data.substring(z + 1);
                    } else {
                        data = data.substring(0, z) + "0" + data.substring(z + 1);
                    }
                }
            } else {
                debugArea.appendText("without errors\n");
            }
        }
        return data;
    }

    public String createHammingCode(String data) {
        String hamming = data.substring(0, 7);//0,7
        hamming += "?";
        hamming += data.substring(7, 10);//7,10
        hamming += "?";
        hamming += data.substring(10);//10
        hamming += "??";
        ArrayList<Integer> r1 = new ArrayList(
                Arrays.asList(2, 4, 6, 8, 10, 12));//2,4,6,8,10,12
        ArrayList<Integer> r2 = new ArrayList(
                Arrays.asList(1, 4, 5, 8, 9, 10, 12));//1,4,5,8,9,10,12
        ArrayList<Integer> r3 = new ArrayList(
                Arrays.asList(1, 2, 3, 8, 9, 10));//1,2,3,8,9,10
        ArrayList<Integer> r4 = new ArrayList(
                Arrays.asList(1, 2, 3, 4, 5, 6));//1,2,3,4,5,6
        ArrayList<Integer> p = new ArrayList(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14));//1,2,3,4,5,6,7,8,9,10,11,12,13,14
        hamming = generateControlNumber(hamming, 14, r1, 0);
        hamming = generateControlNumber(hamming, 13, r2, 0);
        hamming = generateControlNumber(hamming, 11, r3, 0);
        hamming = generateControlNumber(hamming, 7, r4, 0);
        Boolean temp = summ(hamming, p, 0);
        if (temp) hamming = "1" + hamming;
        else hamming = "0" + hamming;
        debugArea.appendText("Without errors: " + hamming + "\n");
        return hamming;
    }

    public String generateControlNumber(String hamming, Integer index, ArrayList<Integer> list, Integer start) {
        Boolean temp = summ(hamming, list, start);
        if (temp) hamming = hamming.substring(0, index) + "1" + hamming.substring(index + 1);
        else hamming = hamming.substring(0, index) + "0" + hamming.substring(index + 1);
        return hamming;
    }

    public boolean summ(String hamming, ArrayList<Integer> list, Integer start) {
        Boolean temp = false;
        if (Integer.parseInt(String.valueOf(hamming.toCharArray()[start])) == 0) temp = false;
        if (Integer.parseInt(String.valueOf(hamming.toCharArray()[start])) == 1) temp = true;
        for (int i : list) {
            if (Integer.parseInt(String.valueOf(hamming.toCharArray()[i])) == 0) temp = temp ^ false;
            if (Integer.parseInt(String.valueOf(hamming.toCharArray()[i])) == 1) temp = temp ^ true;
        }
        return temp;
    }

    public String processString() {
        String dataStr = inputArea.getText();
        bufferArea.appendText("Message: ");
        buf = "";
        if (buffer.length() > 7) {
            buf = buffer.substring(buffer.length() - 7);
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
            dataStr = buf.substring(buf.length() - dataStr.length() - count * 3, buf.length());
            bufferArea.appendText(dataStr + "\n");
            StringBuilder newStrr = new StringBuilder();
            for (int i = 0; i < dataStr.length(); i++) {
                if (dataStr.charAt(i) != '{' && dataStr.charAt(i) != '}') newStrr.append(dataStr.charAt(i));
            }
            dataStr = newStrr.toString();
        } else {
            bufferArea.appendText(dataStr + "\n");
        }
        buffer += buf;
        return dataStr;
    }

    public void bitstaffing(String flag) {
        StringBuilder newStr = new StringBuilder();
        String text = buf;
        int count = (text + "\0").split(flag).length - 1;
        for (int j = 0; j < count; j++) {
            for (int i = 0; i < buf.length(); i++) {
                newStr.append(buf.toCharArray()[i]);
                if (i == buf.indexOf(flag) + 6) {
                    newStr.append("{0}");
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
                    if ((new String(data)).equals("!")){
                        outputArea.appendText("\n");
                    }
                    if (!(new String(data)).equals("\n") && !(new String(data)).equals("!")) {
                        bufferForOut += new String(data);
                    } else if ((new String(data)).equals("\n")) {
                        int temp = 0;
                        if (bufferForOut.toCharArray()[bufferForOut.length() - 1] != '*') {
                            if (Integer.parseInt(String.valueOf(bufferForOut.toCharArray()[bufferForOut.length() - 1])) > 1) {
                                temp = Integer.parseInt(String.valueOf(bufferForOut.toCharArray()[bufferForOut.length() - 1]));
                                bufferForOut = bufferForOut.substring(0, bufferForOut.length() - 1);
                            }
                        }
                        if (bufferForOut.length() > 11) {
                            String newStr = bufferForOut.substring(0, bufferForOut.length());
                            if (bufferForOut.toCharArray()[11] == '*') {
                                debugArea.appendText("Frame with collision error\n");
                                bufferForOut = (new StringBuilder(bufferForOut)).deleteCharAt(11).toString();
                            } else {
                                debugArea.appendText("Frame without collision error\n");
                                if (temp == 0) {
                                    outputArea.appendText(bufferForOut);
                                } else {
                                    outputArea.appendText(newStr.substring(newStr.length() - temp));
                                }
                            }
                        } else {
                            debugArea.appendText("Frame without collision error\n");
                            if (temp == 0) {
                                outputArea.appendText(bufferForOut);
                            } else {
                                outputArea.appendText(bufferForOut.substring(bufferForOut.length() - temp));
                            }
                        }

                        bufferForOut = "";
                    }
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }

        public void processString() {
            Integer temp = 0;
            if (bufferForOut.length() % 16 != 0) {
                temp = Integer.parseInt(String.valueOf(bufferForOut.toCharArray()[bufferForOut.length() - 1]));
                bufferForOut = bufferForOut.substring(0, bufferForOut.length() - 1);
            }
            for (int i = 0; i < bufferForOut.length() / 16; i++) {
                String newStr = decode(bufferForOut.substring(i * 16, (i + 1) * 16));
                StringBuilder str = new StringBuilder(newStr);
                bufferArea.appendText(newStr + "\n");
                if (newStr.contains("{")) {
                    str.deleteCharAt(str.indexOf("{"));
                }
                if (newStr.contains("}")) {
                    str.deleteCharAt(str.indexOf("}"));
                }
                if (newStr.contains("[")) {
                    str.deleteCharAt(str.indexOf("["));
                }
                if (newStr.contains("]")) {
                    str.deleteCharAt(str.indexOf("]"));
                }
                if (temp == 0 && i == bufferForOut.length() / 16 - 1)
                    outputArea.appendText(str.substring(1, 8) + str.substring(9, 12) + str.substring(13, 14) + "\n");
                else if (temp != 0 && i != bufferForOut.length() / 16 - 1) {
                    outputArea.appendText(str.substring(1, 8) + str.substring(9, 12) + str.substring(13, 14));
                } else if (temp != 0 && i == bufferForOut.length() / 16 - 1) {
                    String str2 = str.substring(1, 8) + str.substring(9, 12) + str.substring(13, 14);
                    outputArea.appendText(str2.substring(str2.length() - temp) + "\n");
                } else if (temp == 0) {
                    outputArea.appendText(str.substring(1, 8) + str.substring(9, 12) + str.substring(13, 14));
                }
            }
            bufferForOut = "";
        }

        public String decode(String data) {
            Boolean temp = false;
            String newData = data.substring(1);
            String info = "";
            String p = "";
            String pf = data.substring(0, 1);
            String decodeData = data.substring(1, 8) + data.substring(9, 12) + data.substring(13, 14);

            ArrayList<Integer> c1 = new ArrayList(
                    Arrays.asList(2, 4, 6, 8, 10, 12, 14));
            ArrayList<Integer> c2 = new ArrayList(
                    Arrays.asList(1, 4, 5, 8, 9, 10, 12, 13));
            ArrayList<Integer> c3 = new ArrayList(
                    Arrays.asList(1, 2, 3, 8, 9, 10, 11));
            ArrayList<Integer> c4 = new ArrayList(
                    Arrays.asList(1, 2, 3, 4, 5, 6, 7));
            ArrayList<Integer> pInd = new ArrayList(
                    Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));

            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    temp = summ(newData, c4, 0);
                }
                if (i == 1) {
                    temp = summ(newData, c3, 0);
                }
                if (i == 2) {
                    temp = summ(newData, c2, 0);
                }
                if (i == 3) {
                    temp = summ(newData, c1, 0);
                }
                if (temp) info += "1";
                else info += "0";
            }

            temp = summ(data, pInd, 0);
            if (temp) p = "1";
            else p = "0";

            if (info.equals("0000")) {
                if (p.equals("0")) {
                    StringBuilder str = new StringBuilder(data);
                    int j = 0;
                    for (int i = 0; i < str.length(); i++) {
                        if (str.charAt(i) == '1' || str.charAt(i) == '0') {
                            if (j == 8 || j == 12 || j == 14 || j == 15) {
                                str.insert(i, "[");
                                str.insert(i + 2, "]");
                                i++;
                            }
                            j++;
                        }
                    }
                    return str.toString() + ":" + info;
                } else {
                    debugArea.appendText("Error occurred in p bit\n");
                    StringBuilder str = new StringBuilder(data);
                    str.insert(0, "{");
                    str.insert(2, "}");
                    int j = 0;
                    for (int i = 0; i < str.length(); i++) {
                        if (str.charAt(i) == '1' || str.charAt(i) == '0') {
                            if (j == 8 || j == 12 || j == 14 || j == 15) {
                                str.insert(i, "[");
                                str.insert(i + 2, "]");
                                i++;
                            }
                            j++;
                        }
                    }
                    return str.toString() + ":" + info;
                }
            } else {
                if (p.equals("0")) {
                    debugArea.appendText("Double error detected\n");
                    StringBuilder str = new StringBuilder(data);
                    int j = 0;
                    for (int i = 0; i < str.length(); i++) {
                        if (str.charAt(i) == '1' || str.charAt(i) == '0') {
                            if (j == 8 || j == 12 || j == 14 || j == 15) {
                                str.insert(i, "[");
                                str.insert(i + 2, "]");
                                i++;
                            }
                            j++;
                        }
                    }
                    return str.toString() + ":" + info;
                } else {
                    int index = Integer.parseInt(info, 2);
                    if (newData.toCharArray()[newData.length() - index] == '0')
                        newData = newData.substring(0, newData.length() - index) + "1" + newData.substring(newData.length() - index + 1);
                    else
                        newData = newData.substring(0, newData.length() - index) + "0" + newData.substring(newData.length() - index + 1);
                    StringBuilder newStr = new StringBuilder(newData);
                    newStr.insert(newData.length() - index, "{").insert(newData.length() - index + 2, "}");
                    int j = 0;
                    for (int i = 0; i < newStr.length(); i++) {
                        if (newStr.charAt(i) == '1' || newStr.charAt(i) == '0') {
                            if (j == 7 || j == 11 || j == 13 || j == 14) {
                                newStr.insert(i, "[");
                                newStr.insert(i + 2, "]");
                                i++;
                            }
                            j++;
                        }
                    }
                    return pf + newStr.toString() + ":" + info;
                }
            }
        }

        public void debitstaffing(String data) {
            if (!data.equals("\n")) bufferForOut += data;
            if (!(bufferForOut.contains("00001010") && (bufferForOut.lastIndexOf("00001010") + 7) == bufferForOut.length() - 1))
                outputArea.appendText(data);
        }

    }
}