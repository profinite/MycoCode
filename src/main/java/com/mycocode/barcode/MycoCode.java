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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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
        //File file = ResourceUtils.getFile("classpath:blankTags.pdf");
        PDDocument doc = new PDDocument();

        /* Layer a ruler template over it. */
        // Process of imposing a layer begins here
        //PDPageTree destinationPages = doc.getDocumentCatalog().getPages();

        LayerUtility layerUtility = new LayerUtility(doc);

        File file = ResourceUtils.getFile("classpath:blankTags.pdf");
        PDDocument templatePDF = PDDocument.load(file);
        PDFormXObject firstForm = layerUtility.importPageAsForm(templatePDF, 0);

        for(int i = 0; i <= count / SLIPS_PER_PAGE; i++) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.beginText();
            contentStream.showText("Maravilla");
            // Some text
            // Table 1 (Depending on table 1 size, pdf pages will increase)
            contentStream.endText();
            contentStream.close();

            AffineTransform affineTransform = new AffineTransform();
            PDPageTree destinationPages = doc.getDocumentCatalog().getPages();
            PDPage destPage = destinationPages.get(i);
            layerUtility.wrapInSaveRestore(destPage);
            layerUtility.appendFormAsLayer(destPage, firstForm, affineTransform, "external page" + i);
        }
        for(int i = 0; i < count / SLIPS_PER_PAGE; i++) {
            int begin = start + i * SLIPS_PER_PAGE;
            int end = begin + SLIPS_PER_PAGE;
            drawPersonalSlips(doc, IntStream.rangeClosed(begin, end).boxed().toList(), initials);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        doc.save(outputStream);
        return outputStream.toByteArray();
    }
    private static void drawPersonalSlips(PDDocument doc, List<Integer> range, String initials) throws Exception {
        PDPage page = new PDPage();
        doc.addPage(page);
        Function<Integer, String> pad = x -> String.format("%04d", x);
        List<BufferedImage> QRs = range.stream()
                .map(x -> initials + pad.apply(x))
                .map(MycoCode::toINaturalistURLTag)
                .map(Barcoder::generateQrcode)
                .toList();
        PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        int index = 0;
        Point p = new Point(142, 10);
        for (BufferedImage qr : QRs) {
            float lineOffset = index * 100 + p.y;
            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, qr);
            contentStream.drawImage(pdImage, p.x,  lineOffset);
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(p.x + 100, lineOffset); // Adjust coordinates as needed
            contentStream.showText(initials + " " + pad.apply(range.get(index)));
            contentStream.newLine();
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





