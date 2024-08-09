package com.mycocode.barcode;

import io.nayuki.qrcodegen.DataTooLongException;
import io.nayuki.qrcodegen.QrCode;

import java.awt.image.BufferedImage;
import java.util.Objects;

public class Barcoder {
    public static BufferedImage generateQrcode(String barcodeText) {
        return generateQrcode(barcodeText, 1);
    }
    public static BufferedImage generateQrcode(String barcodeText, int scale) {
        QrCode qrCode;
        try {
            qrCode = QrCode.encodeText(barcodeText, QrCode.Ecc.LOW);
            return toImage(qrCode, scale, 0);
        } catch(DataTooLongException e) {
            System.err.println("Internal error, url is too long." + e.toString());
        }
        return null;
    }

    private static BufferedImage toImage(QrCode qr, int scale, int border) {
        return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
    }

    private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
        Objects.requireNonNull(qr);
        if (scale <= 0 || border < 0)
            throw new IllegalArgumentException("Value out of range");
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");
        BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColor : lightColor);
            }
        }
        return result;
    }
}