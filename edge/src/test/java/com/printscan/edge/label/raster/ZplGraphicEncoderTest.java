package com.printscan.edge.label.raster;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ^GFA 인코딩의 비트패킹을 폰트 무관하게 결정적으로 검증. */
class ZplGraphicEncoderTest {

    private BufferedImage blank(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, Color.WHITE.getRGB());
        return img;
    }

    @Test
    void 비트패킹_MSB_우선_행바이트경계() {
        BufferedImage img = blank(8, 2);
        img.setRGB(0, 0, Color.BLACK.getRGB()); // row0: x0 → MSB → 0x80
        img.setRGB(7, 1, Color.BLACK.getRGB()); // row1: x7 → LSB → 0x01

        String field = ZplGraphicEncoder.toGraphicField(img);
        // total=2바이트, 행당 1바이트
        assertEquals("^GFA,2,2,1,8001", field);
    }

    @Test
    void 폭이_8의배수아니면_바이트로_반올림패딩() {
        BufferedImage img = blank(9, 1); // 9px → 2 bytes/row
        img.setRGB(8, 0, Color.BLACK.getRGB()); // 두 번째 바이트의 MSB → 0x80
        String field = ZplGraphicEncoder.toGraphicField(img);
        assertEquals("^GFA,2,2,2,0080", field);
    }

    @Test
    void 전체라벨_wrap_구조() {
        BufferedImage img = blank(8, 1);
        String zpl = ZplGraphicEncoder.wrapLabel(img, 3);
        assertTrue(zpl.startsWith("^XA"), "시작 ^XA");
        assertTrue(zpl.contains("^PW8"), "인쇄폭");
        assertTrue(zpl.contains("^LL1"), "라벨길이");
        assertTrue(zpl.contains("^FO0,0^GFA,"), "그래픽 배치");
        assertTrue(zpl.contains("^PQ3"), "매수");
        assertTrue(zpl.trim().endsWith("^XZ"), "종료 ^XZ");
    }
}
