package com.mycocode.barcode;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * FIXME: Refactor this
 * 1) Refactor
 * 2) Add Exception handling for disconnected mycoblitz etc
 */
@Component
public class MycoCode {
    final static boolean TEST_MODE = false;
    /* purely for prototyping, not related to Spring */
    public static void main(String[] args) throws Exception {
        System.out.println("Welcome to MycoCode!🍄");
        long start = System.currentTimeMillis();
        IntStream.range(0, 6).forEach(System.out::println);
        MycoCode.generateSlips(2);
        System.out.println("End: " + (System.currentTimeMillis() - start) / 1000.0 + " seconds");
    }

    /**
     * @param count - How many slips to generate
     * @param start - beginning number for this series
     * @param initials - Collector's three-letter initials
     * @return PDF byte stream
     */
    public static byte[] generatePersonalSlips(int count, int start, String initials) throws Exception {
        final int SLIPS_PER_PAGE = 24;
        PDDocument doc;
        File file;
        try {
            file = ResourceUtils.getFile("classpath:blankTags.pdf");
            doc = PDDocument.load(file);
        }
        catch(Exception e) {
            file = ResourceUtils.getFile("blankTags.pdf");
            doc = PDDocument.load(file);
        }

        for(int i = 0; i < doc.getNumberOfPages(); i++) {
            if(i >= count / SLIPS_PER_PAGE) {
                doc.removePage(i--);
                continue;
            }
            int begin = start + i * SLIPS_PER_PAGE + 1; // begin count at 001
            int end = begin + SLIPS_PER_PAGE;
            var range = IntStream.range(begin, end).boxed().collect(Collectors.toList()).reversed();
            drawPersonalSlips(doc, doc.getPage(i), range, initials);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        doc.save(outputStream);
        doc.close();
        return outputStream.toByteArray();
    }

    /**
     * Create a personal voucher label
     * @param range - numbers to print
     * @param initials - collector's initials
     * @throws Exception for usual IO reasons
     */
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
            float column = (index + 1) % 2 == 0 ? p.x : p.x + COLUMN_WIDTH;

            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, qr);
            contentStream.drawImage(pdImage, column,  row);
            drawText(contentStream, column, row, initials + " " + pad.apply(range.get(index)));
            index++;
        }
        contentStream.close();
    }
    // Helper function to write the label text
    private static void drawText(PDPageContentStream content, float x, float y, String label) throws Exception {
        content.beginText();
//        content.newLineAtOffset(x + 65, y + 5); // Adjust coordinates as needed
        content.newLineAtOffset(x + 65, y + 9); // Adjust coordinates as needed
        content.setFont(PDType1Font.HELVETICA_BOLD, 22);
        content.showText(" " + label);
        content.setLeading(17.5f);
        //content.setLeading(14.5f);
        content.newLine();
        content.setFont(PDType1Font.HELVETICA_BOLD, 8);
        content.showText("!……….!……….!……….!……….!");
        //content.newLineAtOffset(x + 65, y + 5); // Adjust coordinates as needed
        content.endText();
    }

    /**
     * TODO: Add exception handling
     * @param count - How many voucher slips to print
     * @return ByteStream of Newly generated PDF
     */
    public static byte[] generateSlips(int count) throws Exception {
        PDDocument doc;
        if (TEST_MODE) {
            File file = ResourceUtils.getFile("classpath:blankMycota.pdf");
            doc = PDDocument.load(file);
            System.err.println("Would normally generate: " + count + "slips.");
        } else {
            byte[] pdfBytes = SlipRequest.requestSlip(count);
            FileOutputStream fos = new FileOutputStream("fresh_slip.pdf");
            fos.write(pdfBytes);
            fos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes);
            doc = PDDocument.load(bais);
        }
        List<String> info = getVoucherInfo(doc).toList();
        String base = info.get(0);
        int num = Integer.parseInt(info.get(1));
        insertImage(doc, num, base);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        doc.save(outputStream);
//        doc.close();
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
        final int SLIPS_PER_PAGE = 2;
        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            PDPage page = doc.getPage(i);
            insertCode(doc, page, baseNum + i * SLIPS_PER_PAGE, base, duplex);
        }
    }
    private static void insertCode(PDDocument doc, PDPage page, int num, String base, boolean duplex) throws IOException {
        //PDImageXObject pdImage = PDImageXObject.createFromFile("/tmp/qr.png", doc);
        List<BufferedImage> QRs = Stream.of(num, ++num)
                .map(i -> base + i)
                .map(MycoCode::toINaturalistURL)
                .map(Barcoder::generateQrcode)
                .toList()
                .reversed();
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
    private static String toINaturalistURL(String voucherNum) {
        final String iNaturalistBase = "https://www.inaturalist.org/observations?verifiable=any&place_id=any&field:Voucher%20Number(s)=";
        return iNaturalistBase + voucherNum;
    }
    private static String toINaturalistURLTag(String tag) {
        final String iNaturalistBase = "https://www.inaturalist.org/observations?q=";
        return iNaturalistBase + tag;
    }
    private static String toINaturalistURLField(String num) {
        final String iNaturalistBase = "https://www.inaturalist.org/observations?verifiable=any&place_id=any&field:Field%20number=";
        return iNaturalistBase + num;
    }
    private static Stream<String> getVoucherInfo(PDDocument doc) throws IOException {
            // eg, "CM23-07508";
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(doc);
            return text.lines().limit(2);
    }
    public static byte[] generateFundisSlips(int count, int start) throws Exception {
        final int SLIPS_PER_PAGE = 4;
        PDDocument doc;
        File file;
            file = ResourceUtils.getFile("classpath:blankFundis.pdf");
            doc = PDDocument.load(file);

        for(int i = 0; i < doc.getNumberOfPages(); i++) {
            if(i >= count / SLIPS_PER_PAGE) {
                doc.removePage(i--);
                continue;
            }
            int begin = start + i * SLIPS_PER_PAGE + 1; // begin count at 001
            int end = begin + SLIPS_PER_PAGE;
            var range = IntStream.range(begin, end).boxed().collect(Collectors.toList()).reversed();
            drawFundisSlips(doc, doc.getPage(i), range);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        doc.save(outputStream);
        return outputStream.toByteArray();
    }
    private static void drawFundisSlips(PDDocument doc, PDPage page, List<Integer> range) throws Exception {
        Function<Integer, String> pad = x -> String.format("%04d", x);
        List<BufferedImage> QRs = range.stream()
                .map(pad)
                .map(MycoCode::toINaturalistURLField)
                .map(Barcoder::generateQrcode)
                .toList();
        PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        int index = 0;
        Point p = new Point(340, 20);
        final int ROW_WIDTH = 152;
        final int COLUMN_WIDTH = 390;
        for (BufferedImage qr : QRs) {
            float row = index % 2 == 0 ? index * ROW_WIDTH + p.y : p.y + (index - 1) * ROW_WIDTH;
//            float column = index % 2 == 0 ? p.x : p.x + COLUMN_WIDTH;
            float column = (index + 1) % 2 == 0 ? p.x : p.x + COLUMN_WIDTH;

            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, qr);
            contentStream.beginText();
            contentStream.newLineAtOffset(column - 65, row + 5); // Adjust coordinates as needed
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 22);
            contentStream.showText(pad.apply(range.get(index)));
            contentStream.endText();
            contentStream.drawImage(pdImage, column,  row);
            index++;
        }
        contentStream.close();
    }
}