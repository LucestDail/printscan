package com.printscan.edge.label.raster;

import java.awt.image.BufferedImage;

/**
 * BufferedImage(흑백 렌더 결과) → ZPL ^GFA 그래픽 필드.
 * 화면에 그린 그대로 프린터로 나가므로 한글/QR/임의 디자인이 그대로 인쇄된다(WYSIWYG).
 *
 * ^GFA,{총바이트},{총바이트},{행당바이트},{HEX}
 *   - 픽셀 luminance < 128 → 검은 점(비트 1), MSB first, 행은 바이트 경계로 패딩.
 */
public final class ZplGraphicEncoder {

    private ZplGraphicEncoder() {}

    /** 이미지 → ^GFA 필드 문자열(^FO/^FS 미포함). */
    public static String toGraphicField(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int bytesPerRow = (w + 7) / 8;
        int total = bytesPerRow * h;
        StringBuilder hex = new StringBuilder(total * 2);
        for (int y = 0; y < h; y++) {
            for (int bx = 0; bx < bytesPerRow; bx++) {
                int b = 0;
                for (int k = 0; k < 8; k++) {
                    int x = bx * 8 + k;
                    int black = 0;
                    if (x < w) {
                        int rgb = img.getRGB(x, y);
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int bl = rgb & 0xff;
                        int lum = (r * 299 + g * 587 + bl * 114) / 1000;
                        black = lum < 128 ? 1 : 0;
                    }
                    b = (b << 1) | black;
                }
                hex.append(HEX[(b >> 4) & 0xf]).append(HEX[b & 0xf]);
            }
        }
        return "^GFA," + total + "," + total + "," + bytesPerRow + "," + hex;
    }

    /**
     * 압축 ^GFA. ZPL ACS: ':' = 직전 행과 동일, 런렝스(대문자 G–Y=1–19, 소문자 g–z=20–400)로 반복 hex 압축.
     * 라벨은 여백(0x00)이 많아 5~10x 축소 → 배치/네트워크 처리량 개선. 디코드 왕복으로 원본과 동일 보장(테스트).
     */
    public static String toGraphicFieldCompressed(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int bytesPerRow = (w + 7) / 8;
        int total = bytesPerRow * h;
        StringBuilder data = new StringBuilder();
        String prev = null;
        for (int y = 0; y < h; y++) {
            String row = rowHex(img, y, bytesPerRow);
            if (row.equals(prev)) {
                data.append(':');            // 직전 행 반복
            } else {
                compressRow(row, data);
                prev = row;
            }
        }
        return "^GFA," + total + "," + total + "," + bytesPerRow + "," + data;
    }

    private static String rowHex(BufferedImage img, int y, int bytesPerRow) {
        StringBuilder sb = new StringBuilder(bytesPerRow * 2);
        int w = img.getWidth();
        for (int bx = 0; bx < bytesPerRow; bx++) {
            int b = 0;
            for (int k = 0; k < 8; k++) {
                int x = bx * 8 + k;
                int black = 0;
                if (x < w) {
                    int rgb = img.getRGB(x, y);
                    int lum = (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
                    black = lum < 128 ? 1 : 0;
                }
                b = (b << 1) | black;
            }
            sb.append(HEX[(b >> 4) & 0xf]).append(HEX[b & 0xf]);
        }
        return sb.toString();
    }

    /** 한 행 hex 를 런렝스 압축해 data 에 append. */
    private static void compressRow(String row, StringBuilder data) {
        int i = 0, n = row.length();
        while (i < n) {
            char c = row.charAt(i);
            int run = 1;
            while (i + run < n && row.charAt(i + run) == c) run++;
            if (run == 1) {
                data.append(c);
            } else {
                appendCount(run, data);
                data.append(c);
            }
            i += run;
        }
    }

    /** 반복 횟수 → ACS 카운트 문자열(소문자 20단위 + 대문자 1–19). */
    private static void appendCount(int count, StringBuilder sb) {
        while (count >= 20) {
            int m = Math.min(400, (count / 20) * 20);
            sb.append((char) ('g' + m / 20 - 1));   // g=20 … z=400
            count -= m;
        }
        if (count > 0) sb.append((char) ('G' + count - 1)); // G=1 … Y=19
    }

    /** 전체 라벨 ZPL 로 감싼다: ^XA ^PW ^LL ^FO0,0 ^GFA ^FS [^PQ] ^XZ. */
    public static String wrapLabel(BufferedImage img, int copies) {
        return wrapLabel(img, copies, -1, -1);
    }

    /** darkness(~SD 0~30)·speed(^PR) 를 함께 지정. 음수면 미설정. */
    public static String wrapLabel(BufferedImage img, int copies, int darkness, int speed) {
        StringBuilder z = new StringBuilder();
        z.append("^XA\n");
        if (darkness >= 0) z.append("~SD").append(Math.min(30, darkness)).append('\n');
        if (speed > 0) z.append("^PR").append(speed).append('\n');
        z.append("^PW").append(img.getWidth()).append('\n');
        z.append("^LL").append(img.getHeight()).append('\n');
        z.append("^FO0,0").append(toGraphicFieldCompressed(img)).append("^FS\n");
        if (copies > 1) {
            z.append("^PQ").append(copies).append('\n');
        }
        z.append("^XZ");
        return z.toString();
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
}
