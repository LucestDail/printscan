package com.printscan.edge.label.raster;

import com.printscan.edge.config.LabelProperties;
import com.printscan.edge.label.LabelTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 템플릿(한글 텍스트+QR) → 이미지/ZPL 생성 스모크. 폰트 유무와 무관하게 콘텐츠가 그려지는지 확인. */
class LabelRasterizerTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    private LabelTemplate template() {
        LabelTemplate t = new LabelTemplate();
        t.setName("test");
        t.setWidthMm(40);
        t.setHeightMm(30);
        t.setDpi(203);
        t.setElementsJson("""
            [
              {"type":"TEXT","xMm":2,"yMm":2,"value":"한글 {{name}}","sizeMm":4,"bold":true},
              {"type":"QR","xMm":2,"yMm":10,"value":"{{code}}","sizeMm":14},
              {"type":"BARCODE","xMm":2,"yMm":24,"value":"{{code}}","sizeMm":4,"widthMm":30}
            ]
            """);
        return t;
    }

    @Test
    void 렌더_치수_및_콘텐츠존재() {
        LabelRasterizer r = new LabelRasterizer(new LabelProperties());
        BufferedImage img = r.render(template(), Map.of("name", "제품A", "code", "PROD-0001"));

        // 40x30mm @203dpi(8dot/mm) = 320x240
        assertEquals(320, img.getWidth());
        assertEquals(240, img.getHeight());

        // QR/바코드/텍스트가 그려졌으면 검은 픽셀이 존재
        int black = 0;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if ((img.getRGB(x, y) & 0xffffff) == 0) black++;
        assertTrue(black > 100, "검은 픽셀이 충분히 그려져야 함(실제=" + black + ")");
    }

    @Test
    void ZPL_GFA_생성() {
        LabelRasterizer r = new LabelRasterizer(new LabelProperties());
        BufferedImage img = r.render(template(), Map.of("name", "제품A", "code", "PROD-0001"));
        String zpl = ZplGraphicEncoder.wrapLabel(img, 1);
        assertTrue(zpl.contains("^GFA,"));
        assertTrue(zpl.contains("^PW320"));
        assertTrue(zpl.startsWith("^XA") && zpl.trim().endsWith("^XZ"));
    }

    @Test
    void DataMatrix_렌더() {
        LabelTemplate t = new LabelTemplate();
        t.setWidthMm(30); t.setHeightMm(30); t.setDpi(203);
        t.setElementsJson("[{\"type\":\"DATAMATRIX\",\"xMm\":2,\"yMm\":2,\"value\":\"{{code}}\",\"sizeMm\":15}]");
        BufferedImage img = new LabelRasterizer(new LabelProperties()).render(t, Map.of("code", "DM-0001"));
        int black = 0;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if ((img.getRGB(x, y) & 0xffffff) == 0) black++;
        assertTrue(black > 50, "DataMatrix 모듈이 그려져야 함(실제=" + black + ")");
    }

    @Test
    void 변수치환() {
        assertEquals("제품 PROD-1", LabelRasterizer.bind("제품 {{code}}", Map.of("code", "PROD-1")));
        assertEquals("빈값 ", LabelRasterizer.bind("빈값 {{x}}", Map.of("x", "")));
    }
}
