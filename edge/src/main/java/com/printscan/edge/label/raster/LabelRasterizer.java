package com.printscan.edge.label.raster;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.printscan.edge.config.LabelProperties;
import com.printscan.edge.label.LabelElement;
import com.printscan.edge.label.LabelTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * 라벨 템플릿(요소 + 변수) → 흑백 BufferedImage. Java2D 로 한글 텍스트/QR/바코드/박스를 배치.
 * 좌표·크기는 mm → dpi 로 px 변환. 이 이미지를 그대로 미리보기 PNG 로도, ^GFA 인쇄로도 쓴다(WYSIWYG).
 */
@Slf4j
@Component
public class LabelRasterizer {

    private final LabelProperties props;
    private final Font baseFont;

    public LabelRasterizer(LabelProperties props) {
        this.props = props;
        this.baseFont = resolveFont(props);
        log.info("[raster] base font = '{}'", baseFont.getFontName());
    }

    private static Font resolveFont(LabelProperties props) {
        // 1) font-path 지정 시 파일 직접 로드
        if (props.getFontPath() != null && !props.getFontPath().isBlank()) {
            try {
                Font f = Font.createFont(Font.TRUETYPE_FONT, new File(props.getFontPath()));
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f.deriveFont(Font.PLAIN, 10f);
            } catch (Exception e) {
                log.warn("[raster] font-path 로드 실패({}) → 패밀리명 폴백", props.getFontPath());
            }
        }
        // 2) 패밀리명(fontconfig). 리눅스에서 'Noto Sans CJK KR' 해석 → 한글 렌더. 미해석 시 논리폰트.
        return new Font(props.getFontFamily(), Font.PLAIN, 10);
    }

    private int dotsPerMm(int dpi) { return dpi >= 300 ? 12 : 8; }
    private int mm2dot(double mm, int dpi) { return (int) Math.round(mm * dotsPerMm(dpi)); }

    /** 템플릿 + 변수 → 흑백 이미지. */
    public BufferedImage render(LabelTemplate t, Map<String, String> vars) {
        int dpi = t.getDpi() != null ? t.getDpi() : props.getDefaultDpi();
        int w = Math.max(1, mm2dot(t.getWidthMm(), dpi));
        int h = Math.max(1, mm2dot(t.getHeightMm(), dpi));

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.BLACK);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            for (LabelElement e : t.elements()) {
                int x = mm2dot(e.getXMm(), dpi);
                int y = mm2dot(e.getYMm(), dpi);
                String val = bind(e.getValue(), vars);
                switch (e.getType()) {
                    case TEXT -> drawText(g, val, x, y, mm2dot(e.getSizeMm(), dpi), e.isBold());
                    case QR -> drawQr(g, val, x, y, mm2dot(e.getSizeMm(), dpi));
                    case DATAMATRIX -> drawDatamatrix(g, val, x, y, mm2dot(e.getSizeMm(), dpi));
                    case BARCODE -> drawBarcode(g, val, x, y,
                            mm2dot(e.getWidthMm() > 0 ? e.getWidthMm() : 30, dpi),
                            mm2dot(e.getSizeMm(), dpi));
                    case BOX -> g.fillRect(x, y, mm2dot(e.getWidthMm(), dpi), mm2dot(e.getHeightMm(), dpi));
                }
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private void drawText(Graphics2D g, String text, int x, int y, int heightPx, boolean bold) {
        if (text == null || text.isEmpty()) return;
        Font f = baseFont.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) Math.max(6, heightPx));
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x, y + fm.getAscent()); // (x,y)=좌상단 → baseline 보정
    }

    private void drawQr(Graphics2D g, String data, int x, int y, int sizePx) {
        if (data == null || data.isEmpty() || sizePx <= 0) return;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2); // 콰이엇존(모듈) — 스캐너 신뢰성
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            BitMatrix m = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            g.drawImage(MatrixToImageWriter.toBufferedImage(m), x, y, null);
        } catch (Exception e) {
            log.warn("[raster] QR 실패: {}", e.getMessage());
        }
    }

    private void drawDatamatrix(Graphics2D g, String data, int x, int y, int sizePx) {
        if (data == null || data.isEmpty() || sizePx <= 0) return;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix m = new DataMatrixWriter().encode(data, BarcodeFormat.DATA_MATRIX, sizePx, sizePx, hints);
            g.drawImage(MatrixToImageWriter.toBufferedImage(m), x, y, null);
        } catch (Exception e) {
            log.warn("[raster] DataMatrix 실패: {}", e.getMessage());
        }
    }

    private void drawBarcode(Graphics2D g, String data, int x, int y, int widthPx, int heightPx) {
        if (data == null || data.isEmpty() || widthPx <= 0 || heightPx <= 0) return;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 10); // 좌우 콰이엇존(px)
            BitMatrix m = new Code128Writer().encode(data, BarcodeFormat.CODE_128, widthPx, heightPx, hints);
            g.drawImage(MatrixToImageWriter.toBufferedImage(m), x, y, null);
        } catch (Exception e) {
            log.warn("[raster] 바코드 실패: {}", e.getMessage());
        }
    }

    /** {{key}} → value 치환. null 값은 빈 문자열. */
    public static String bind(String s, Map<String, String> vars) {
        if (s == null) return "";
        if (vars == null || vars.isEmpty()) return s;
        String out = s;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }
}
