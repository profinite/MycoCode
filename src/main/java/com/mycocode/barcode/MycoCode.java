package com.mycocode.barcode;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MycoCode {
    final static boolean TEST_MODE = true;
    public static void main(String[] args) throws Exception, IOException, InterruptedException {
        System.out.println("Welcome to MycoCode!🍄");
        long start = System.currentTimeMillis();
//        System.out.println(IntStream.range(0, 6));
        IntStream.range(0, 6).forEach(System.out::println);
//        IntStream.range(0, 6).forEach(System.out::println);
        MycoCode mc = new MycoCode();
        mc.generateSlips(2);
        System.out.println("End: " + (System.currentTimeMillis() - start) / 1000.0 + " seconds");
    }
    public void generateSlips(int count) throws Exception {
        /*
        4) Put on Spring Boot
        5) Deploy to Firebase etc.
        6) Add Exception handling
        */
        PDDocument doc;
        if (TEST_MODE) {
           // File file = getPDF().toFile();
            File file = new File("src/test/exemplar.pdf");
            doc = PDDocument.load(file);
        } else {
            byte[] pdfBytes = SlipRequest.requestSlip();
            FileOutputStream fos = new FileOutputStream("/tmp/fresh_slip.pdf");
            fos.write(pdfBytes);
            fos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes);
            doc = PDDocument.load(bais);
        }
        List<String> info = getVoucherInfo(doc).toList();
        String base = info.get(0);
        int num = Integer.parseInt(info.get(1));
        insertImage(doc, num, base);
        doc.save("/tmp/qr_slips.pdf");
        doc.close();
    }

    void insertImage(PDDocument doc, int baseNum, String base) throws IOException {
        for (int i = 0; i < doc.getNumberOfPages(); i++, baseNum++) {
            PDPage page = doc.getPage(i);
            insertCode(doc, page, baseNum, base);
        }
    }
    private void insertCode(PDDocument doc, PDPage page, int num, String base) throws IOException {
        //PDImageXObject pdImage = PDImageXObject.createFromFile("/tmp/qr.png", doc);
        List<BufferedImage> QRs = Stream.of(num, ++num)
                .map(i -> base + i)
                .map(MycoCode::toINaturalistURL)
                .map(Barcoder::generateQrcode)
                .toList();
        int index = 0;
        Point p = new Point(142.7f, 82.7f);
        p = new Point(p.x - 2.0f, p.y - 2.0f + 1.0f);
        for (BufferedImage qr : QRs) {
            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, qr);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
            contentStream.drawImage(pdImage, p.x, p.y + index * (361.4f - 1.0f));
            contentStream.close();
            index++;
        }
    }
    private record Point (float x, float y) {};
    static String toINaturalistURL(String voucherNum) {
        final String iNaturalistBase = "https://www.inaturalist.org/observations?verifiable=any&place_id=any&field:Voucher%20Number(s)=";
        return iNaturalistBase + voucherNum;
    }
    static Stream<String> getVoucherInfo(PDDocument doc) throws IOException {
            // eg, "CM23-07508";
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(doc);
            return text.lines().limit(2);
    }
    public static Path getPDF() throws IOException {
        String pdfUrl = "https://mycomap.com/index.php?app=mycocal&module=calendar&controller=slips&do=download&id=4658";
        String downloadPath = "/tmp/slips.pdf";

        // Using NIO for efficiency
        Path targetPath = Path.of(downloadPath);
        try (InputStream in = new URL(pdfUrl).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }
}





