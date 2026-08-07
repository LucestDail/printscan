package com.printscan.edge.label;

/**
 * 일련번호 배치 사양. seqVar 변수에 prefix + (start+i) 를 pad 자리로 채워 count 장 연속 출력.
 * 예: seqVar="seq", prefix="NET-", start=1, count=100, pad=4 → NET-0001 … NET-0100.
 */
public record SerialSpec(String seqVar, String prefix, int start, int count, int pad) {

    public String format(int i) {
        String num = pad > 0 ? String.format("%0" + pad + "d", start + i) : String.valueOf(start + i);
        return (prefix == null ? "" : prefix) + num;
    }

    public String var() { return (seqVar == null || seqVar.isBlank()) ? "seq" : seqVar; }
}
