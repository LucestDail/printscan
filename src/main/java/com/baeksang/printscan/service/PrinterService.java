package com.baeksang.printscan.service;

import org.springframework.stereotype.Service;
import javax.print.*;
import java.util.Arrays;
import java.util.Optional;

@Service
public class PrinterService {
    
    /**
     * 프린터 이름으로 프린터 서비스를 찾습니다.
     */
    public Optional<javax.print.PrintService> findPrinter(String... printerNames) {
        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(services)
                .filter(printer -> Arrays.stream(printerNames)
                        .anyMatch(name -> printer.getName().toLowerCase().contains(name.toLowerCase())))
                .findFirst();
    }

    /**
     * ZPL 명령어를 프린터로 전송합니다.
     */
    public void printZpl(javax.print.PrintService printer, String zpl) throws PrintException {
        DocPrintJob job = printer.createPrintJob();
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(zpl.getBytes(), flavor, null);
        job.print(doc, null);
    }

    /**
     * QR 코드를 포함한 ZPL 명령어를 생성합니다.
     */
    // public String generateQrZpl(String data, int x, int y, int scale) {
    //     return String.format("^XA\n^FO%d,%d^BQN,%d,10^FDLA,%s^FS\n^XZ",
    //             x, y, scale, data);
    // }
    public String generateQrZpl(String data, int x, int y, int rotation, int cellSize) {
        return String.format("^XA\n^FO%d,%d^BQN,%d,%d^FDLA,%s^FS\n^XZ",
                x, y, rotation, cellSize, data);
    }

    /**
     * 텍스트를 포함한 ZPL 명령어를 생성합니다.
     */
    public String generateTextZpl(String text, int x, int y, int fontSize) {
        return String.format("^XA\n^FO%d,%d^A0N,%d,%d^FD%s^FS\n^XZ",
                x, y, fontSize, fontSize, text);
    }

    /**
     * QR 코드와 텍스트를 모두 포함한 ZPL 명령어를 생성합니다.
     */
    public String generateQrWithTextZpl(String data, String text) {
        return "^XA\n" +
               "^FO100,50^BQN,2,10^FDLA," + data + "^FS\n" +
               "^FO100,300^A0N,30,30^FD" + text + "^FS\n" +
               "^XZ";
    }
} 