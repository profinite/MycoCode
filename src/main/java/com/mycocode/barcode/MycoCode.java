package com.mycocode.barcode;

import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
public class MycoCode {
    final static boolean TEST_MODE = true;
    public static void main(String[] args) throws Exception {
        System.out.println("Welcome to MycoCode!🍄");
        long start = System.currentTimeMillis();
//        System.out.println(IntStream.range(0, 6));
        IntStream.range(0, 6).forEach(System.out::println);
//        IntStream.range(0, 6).forEach(System.out::println);
        MycoCode.generateSlips(2);
        System.out.println("End: " + (System.currentTimeMillis() - start) / 1000.0 + " seconds");
    }
    public static byte[] generatePersonalSlips(int count, int start, String initials) throws Exception {
        final int SLIPS_PER_PAGE = 24;
        File file = ResourceUtils.getFile("classpath:blankTags.pdf");
        PDDocument doc = PDDocument.load(file);

        for(int i = 0; i < doc.getNumberOfPages(); i++) {
            if(i >= count / SLIPS_PER_PAGE) {
                doc.removePage(i);
                i--;
                continue;
            }
            int begin = start + i * SLIPS_PER_PAGE;
            int end = begin + SLIPS_PER_PAGE;
            var range = IntStream.rangeClosed(++begin, end).boxed().collect(Collectors.toList());
            Collections.reverse(range);
            drawPersonalSlips(doc, doc.getPage(i), range, initials);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        doc.save(outputStream);
        return outputStream.toByteArray();
    }
    private static void drawPersonalSlips(PDDocument doc, PDPage page, List<Integer> range, String initials) throws Exception {
        Function<Integer, String> pad = x -> String.format("%04d", x);
        List<BufferedImage> QRs = range.stream()
                .map(x -> initials + pad.apply(x))
                .map(MycoCode::toINaturalistURLTag)
                .map(Barcoder::generateQrcode)
                .toList();
        PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        int index = 0;
        Point p = new Point(85, 105);
        final int ROW_WIDTH = 26;
        final int COLUMN_WIDTH = 227;
        for (BufferedImage qr : QRs) {
            float row = index % 2 == 0 ? index * ROW_WIDTH + p.y : p.y + (index - 1) * ROW_WIDTH;
            float column = index % 2 == 0 ? p.x : p.x + COLUMN_WIDTH;
            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, qr);
            contentStream.drawImage(pdImage, column,  row);
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 26);
            contentStream.beginText();
            contentStream.newLineAtOffset(column + 65, row + 5); // Adjust coordinates as needed
            contentStream.showText(initials + " " + pad.apply(range.get(index)));
            contentStream.endText();
            index++;
        }
        contentStream.close();
    }
    public static byte[] generateSlips(int count) throws Exception {
        /*
        4) Put on Spring Boot
        5) Deploy to Firebase etc.
        6) Add Exception handling
        */
        PDDocument doc;
        if (TEST_MODE) {
            File file = ResourceUtils.getFile("classpath:exemplar.pdf");
            doc = PDDocument.load(file);
            System.err.println("Would normally generate: " + count + "slips.");
        } else {
            byte[] pdfBytes = SlipRequest.requestSlip(count);
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
        //doc.save("qr_slips.pdf");
        //doc.close();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        doc.save(outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Overlay each page of this document a QR code.
     * @param doc - The original PDF image
     * @param baseNum - Voucher number
     * @param base - Voucher label
     */
    static void insertImage(PDDocument doc, int baseNum, String base) throws IOException {
        boolean duplex = doc.getNumberOfPages() != 1;
        for (int i = 0; i < doc.getNumberOfPages(); i++, baseNum++) {
            PDPage page = doc.getPage(i);
            insertCode(doc, page, baseNum + i * 2, base, duplex);
        }
    }
    private static void insertCode(PDDocument doc, PDPage page, int num, String base, boolean duplex) throws IOException {
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
            if(!duplex) break;
        }
    }
    private record Point (float x, float y) {}
    static String toINaturalistURL(String voucherNum) {
        final String iNaturalistBase = "https://www.inaturalist.org/observations?verifiable=any&place_id=any&field:Voucher%20Number(s)=";
        return iNaturalistBase + voucherNum;
    }
    static String toINaturalistURLTag(String tag) {
        final String iNaturalistBase = "https://www.inaturalist.org/observations?q=";
        return iNaturalistBase + tag;
    }
    static Stream<String> getVoucherInfo(PDDocument doc) throws IOException {
            // eg, "CM23-07508";
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(doc);
            return text.lines().limit(2);
    }
}





