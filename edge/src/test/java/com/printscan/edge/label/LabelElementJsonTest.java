package com.printscan.edge.label;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** xMm/yMm 좌표가 JSON 에서 실제로 매핑되는지(JavaBeans 대문자 2연속 함정 회귀). */
class LabelElementJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 좌표_xMm_yMm_매핑() throws Exception {
        String json = "{\"type\":\"QR\",\"xMm\":12.5,\"yMm\":7,\"value\":\"X\",\"sizeMm\":14,\"widthMm\":30,\"heightMm\":2}";
        LabelElement e = mapper.readValue(json, LabelElement.class);
        assertEquals(ElementType.QR, e.getType());
        assertEquals(12.5, e.getXMm(), 0.001, "xMm 매핑");
        assertEquals(7.0, e.getYMm(), 0.001, "yMm 매핑");
        assertEquals(14.0, e.getSizeMm(), 0.001);
        assertEquals(30.0, e.getWidthMm(), 0.001);
        assertEquals(2.0, e.getHeightMm(), 0.001);
    }
}
