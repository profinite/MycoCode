package com.mycocode.barcode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// test with, eg http://localhost:8080/generate-pdf?count=2
@RestController
public class MycoCodeController {

    @Autowired
    private MycoCode mycoCode;

    @GetMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam(value = "count", defaultValue = "1") int count,
            @RequestParam(value = "start", defaultValue = "0") int start,
            @RequestParam(value = "initials", defaultValue = "ZZZ") String initials) {
        System.err.println("GeneratePDF");
        try {
            byte[] pdfData = null;
            if(initials.equals("ZZZ") || initials.isEmpty())
                pdfData = mycoCode.generateSlips(count);
            else if(initials.equals("FDS"))
                ;
//                pdfData = mycoCode.generateFundisSlips(count, start);
            else
                pdfData = mycoCode.generatePersonalSlips(count, start, initials);
            System.err.println(initials);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr_slips.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN) // Set content type to plain text
                    .body(("Error generating PDF: " + e.getMessage()).getBytes());
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating PDF: " + e.getMessage());
        }
    }
}