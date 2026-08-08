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

    // ── 압축 검증 ──────────────────────────────
    private BufferedImage patterned() {
        BufferedImage img = blank(24, 40); // 3 bytes/row, 40 rows (여백 많음)
        // 몇 개 픽셀만 검게 → 대부분 0x00 행(반복) + 런
        img.setRGB(0, 5, Color.BLACK.getRGB());
        img.setRGB(23, 5, Color.BLACK.getRGB());
        for (int x = 0; x < 24; x++) img.setRGB(x, 10, Color.BLACK.getRGB()); // 전부 검은 행(0xFF 런)
        img.setRGB(12, 20, Color.BLACK.getRGB());
        return img;
    }

    /** 압축 field 를 디코드해 원본(비압축) hex 와 동일한지 왕복 검증 → 인쇄 안전 보장. */
    private static String decode(String field) {
        String[] parts = field.split(",", 5);
        int bytesPerRow = Integer.parseInt(parts[3]);
        String data = parts[4];
        int rowLen = bytesPerRow * 2;
        StringBuilder out = new StringBuilder();
        StringBuilder cur = new StringBuilder();
        String prev = "";
        int count = 0;
        for (int i = 0; i < data.length(); i++) {
            char ch = data.charAt(i);
            if (ch == ':') { out.append(prev); continue; }
            if (ch >= 'G' && ch <= 'Y') { count += (ch - 'G' + 1); continue; }
            if (ch >= 'g' && ch <= 'z') { count += (ch - 'g' + 1) * 20; continue; }
            int rep = count > 0 ? count : 1; count = 0;
            for (int r = 0; r < rep; r++) cur.append(ch);
            if (cur.length() == rowLen) { out.append(cur); prev = cur.toString(); cur.setLength(0); }
        }
        return out.toString();
    }

    @Test
    void 압축_왕복이_원본과_동일() {
        BufferedImage img = patterned();
        String uncompressed = ZplGraphicEncoder.toGraphicField(img).split(",", 5)[4];
        String compressed = ZplGraphicEncoder.toGraphicFieldCompressed(img);
        assertEquals(uncompressed, decode(compressed), "압축 디코드가 원본 hex 와 동일해야 인쇄 안전");
    }

    @Test
    void 압축이_실제로_크기를_줄임() {
        BufferedImage img = patterned();
        int raw = ZplGraphicEncoder.toGraphicField(img).length();
        int comp = ZplGraphicEncoder.toGraphicFieldCompressed(img).length();
        assertTrue(comp < raw, "여백 많은 라벨은 압축이 더 작아야 함 (raw=" + raw + ", comp=" + comp + ")");
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
