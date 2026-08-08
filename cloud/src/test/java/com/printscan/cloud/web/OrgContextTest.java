package com.printscan.cloud.web;

import com.printscan.cloud.domain.Organization;
import com.printscan.cloud.domain.OrganizationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 테넌트 해석: X-Org-Key 우선, 없으면 단일org 폴백, 멀티org면 거부. */
class OrgContextTest {

    private Organization org(long id, String key) {
        Organization o = new Organization(); o.setId(id); o.setName("o"); o.setApiKey(key); return o;
    }

    @Test
    void 키있으면_해당org() {
        OrganizationRepository repo = mock(OrganizationRepository.class);
        when(repo.findByApiKey("K")).thenReturn(Optional.of(org(1, "K")));
        assertEquals(1L, new OrgContext(repo).resolve("K").getId());
    }

    @Test
    void 키없고_단일org면_폴백() {
        OrganizationRepository repo = mock(OrganizationRepository.class);
        when(repo.findAll()).thenReturn(List.of(org(7, "K")));
        assertEquals(7L, new OrgContext(repo).resolve(null).getId());
    }

    @Test
    void 키없고_멀티org면_거부() {
        OrganizationRepository repo = mock(OrganizationRepository.class);
        when(repo.findAll()).thenReturn(List.of(org(1, "A"), org(2, "B")));
        assertThrows(IllegalArgumentException.class, () -> new OrgContext(repo).resolve(null));
    }

    @Test
    void 잘못된키_거부() {
        OrganizationRepository repo = mock(OrganizationRepository.class);
        when(repo.findByApiKey("BAD")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new OrgContext(repo).resolve("BAD"));
    }
}
