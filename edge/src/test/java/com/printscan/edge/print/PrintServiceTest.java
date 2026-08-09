package com.printscan.edge.print;

import com.printscan.edge.config.PrinterProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** transport 디스패치: 활성 mode 로만 전송, 미지원 mode 는 예외. */
class PrintServiceTest {

    /** mode 를 표방하고 마지막 전송 zpl 을 기록하는 가짜 transport. */
    static class FakeTransport implements PrintTransport {
        final String mode;
        final AtomicReference<String> last = new AtomicReference<>();
        FakeTransport(String mode) { this.mode = mode; }
        @Override public String mode() { return mode; }
        @Override public void send(String zpl) { last.set(zpl); }
    }

    private PrinterProperties props(String mode) {
        PrinterProperties p = new PrinterProperties();
        p.setMode(mode);
        return p;
    }

    @Test
    void 활성_mode_transport_로만_전송() throws Exception {
        FakeTransport net = new FakeTransport("network");
        FakeTransport cups = new FakeTransport("cups");
        PrintService svc = new PrintService(props("cups"), List.of(net, cups));

        svc.print("^XA^XZ");

        assertEquals("^XA^XZ", cups.last.get(), "활성 mode(cups) 로 전송되어야 한다");
        assertNull(net.last.get(), "비활성 mode(network) 로는 전송되지 않아야 한다");
    }

    @Test
    void 미지원_mode_는_예외() {
        PrintService svc = new PrintService(props("bogus"), List.of(new FakeTransport("cups")));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> svc.print("^XA^XZ"));
        assertTrue(ex.getMessage().contains("bogus"));
    }
}
