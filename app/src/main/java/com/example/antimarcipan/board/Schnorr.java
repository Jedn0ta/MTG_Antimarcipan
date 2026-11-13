package com.example.antimarcipan.board;

import java.math.BigInteger;
import java.security.MessageDigest;

/** BIP-340 Schnorr podpisy na secp256k1 (pro Nostr). Čistá Java, bez nativních knihoven. */
public final class Schnorr {
    private Schnorr() {}

    static final BigInteger P = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    static final BigInteger N = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    static final BigInteger GX = new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    static final BigInteger GY = new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    private static final BigInteger TWO = BigInteger.valueOf(2), THREE = BigInteger.valueOf(3), SEVEN = BigInteger.valueOf(7);

    // Jakobiánské souřadnice: {X, Y, Z}; null = bod v nekonečnu
    private static BigInteger[] dbl(BigInteger[] p) {
        if (p == null || p[1].signum() == 0) return null;
        BigInteger y2 = p[1].multiply(p[1]).mod(P);
        BigInteger s = p[0].multiply(y2).shiftLeft(2).mod(P);
        BigInteger m = p[0].multiply(p[0]).multiply(THREE).mod(P);
        BigInteger x = m.multiply(m).subtract(s.shiftLeft(1)).mod(P);
        BigInteger y = m.multiply(s.subtract(x)).subtract(y2.multiply(y2).shiftLeft(3)).mod(P);
        BigInteger z = p[1].multiply(p[2]).shiftLeft(1).mod(P);
        return new BigInteger[]{x, y, z};
    }

    private static BigInteger[] add(BigInteger[] p, BigInteger[] q) {
        if (p == null) return q;
        if (q == null) return p;
        BigInteger z1z1 = p[2].multiply(p[2]).mod(P), z2z2 = q[2].multiply(q[2]).mod(P);
        BigInteger u1 = p[0].multiply(z2z2).mod(P), u2 = q[0].multiply(z1z1).mod(P);
        BigInteger s1 = p[1].multiply(q[2]).multiply(z2z2).mod(P), s2 = q[1].multiply(p[2]).multiply(z1z1).mod(P);
        if (u1.equals(u2)) return s1.equals(s2) ? dbl(p) : null;
        BigInteger h = u2.subtract(u1).mod(P), r = s2.subtract(s1).mod(P);
        BigInteger h2 = h.multiply(h).mod(P), h3 = h2.multiply(h).mod(P);
        BigInteger x = r.multiply(r).subtract(h3).subtract(u1.multiply(h2).shiftLeft(1)).mod(P);
        BigInteger y = r.multiply(u1.multiply(h2).subtract(x)).subtract(s1.multiply(h3)).mod(P);
        BigInteger z = h.multiply(p[2]).multiply(q[2]).mod(P);
        return new BigInteger[]{x, y, z};
    }

    private static BigInteger[] mul(BigInteger[] p, BigInteger k) {
        BigInteger[] r = null;
        for (int i = k.bitLength() - 1; i >= 0; i--) {
            r = dbl(r);
            if (k.testBit(i)) r = add(r, p);
        }
        return r;
    }

    /** Afinní {x, y}. */
    private static BigInteger[] affine(BigInteger[] p) {
        BigInteger zi = p[2].modInverse(P), zi2 = zi.multiply(zi).mod(P);
        return new BigInteger[]{p[0].multiply(zi2).mod(P), p[1].multiply(zi2).multiply(zi).mod(P)};
    }

    private static final BigInteger[] G = {GX, GY, BigInteger.ONE};

    static byte[] bytes32(BigInteger v) {
        byte[] b = v.toByteArray(), out = new byte[32];
        int len = Math.min(b.length, 32);
        System.arraycopy(b, b.length - len, out, 32 - len, len);
        return out;
    }

    static BigInteger num(byte[] b) { return new BigInteger(1, b); }

    public static byte[] sha256(byte[]... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (byte[] p : parts) md.update(p);
            return md.digest();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static byte[] taggedHash(String tag, byte[]... parts) {
        byte[] t = sha256(tag.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[][] all = new byte[parts.length + 2][];
        all[0] = t; all[1] = t;
        System.arraycopy(parts, 0, all, 2, parts.length);
        return sha256(all);
    }

    /** X-only veřejný klíč (32 B). */
    public static byte[] publicKey(byte[] secret) {
        BigInteger d = num(secret);
        if (d.signum() == 0 || d.compareTo(N) >= 0) throw new IllegalArgumentException("bad key");
        return bytes32(affine(mul(G, d))[0]);
    }

    /** BIP-340 podpis 32B zprávy. */
    public static byte[] sign(byte[] msg, byte[] secret, byte[] aux) {
        BigInteger d0 = num(secret);
        if (d0.signum() == 0 || d0.compareTo(N) >= 0) throw new IllegalArgumentException("bad key");
        BigInteger[] Pp = affine(mul(G, d0));
        BigInteger d = Pp[1].testBit(0) ? N.subtract(d0) : d0;
        byte[] px = bytes32(Pp[0]);
        byte[] t = bytes32(d.xor(num(taggedHash("BIP0340/aux", aux))));
        BigInteger k0 = num(taggedHash("BIP0340/nonce", t, px, msg)).mod(N);
        if (k0.signum() == 0) throw new IllegalStateException("k=0");
        BigInteger[] R = affine(mul(G, k0));
        BigInteger k = R[1].testBit(0) ? N.subtract(k0) : k0;
        byte[] rx = bytes32(R[0]);
        BigInteger e = num(taggedHash("BIP0340/challenge", rx, px, msg)).mod(N);
        byte[] sig = new byte[64];
        System.arraycopy(rx, 0, sig, 0, 32);
        System.arraycopy(bytes32(k.add(e.multiply(d)).mod(N)), 0, sig, 32, 32);
        return sig;
    }

    /** BIP-340 ověření podpisu. */
    public static boolean verify(byte[] msg, byte[] pub, byte[] sig) {
        try {
            BigInteger x = num(pub);
            if (x.compareTo(P) >= 0) return false;
            BigInteger c = x.pow(3).add(SEVEN).mod(P);
            BigInteger y = c.modPow(P.add(BigInteger.ONE).shiftRight(2), P);
            if (!y.multiply(y).mod(P).equals(c)) return false;
            if (y.testBit(0)) y = P.subtract(y);
            byte[] rb = new byte[32], sb = new byte[32];
            System.arraycopy(sig, 0, rb, 0, 32); System.arraycopy(sig, 32, sb, 0, 32);
            BigInteger r = num(rb), s = num(sb);
            if (r.compareTo(P) >= 0 || s.compareTo(N) >= 0) return false;
            BigInteger e = num(taggedHash("BIP0340/challenge", rb, pub, msg)).mod(N);
            BigInteger[] R = add(mul(G, s), mul(new BigInteger[]{x, y, BigInteger.ONE}, N.subtract(e)));
            if (R == null) return false;
            BigInteger[] Ra = affine(R);
            return !Ra[1].testBit(0) && Ra[0].equals(r);
        } catch (Exception ex) { return false; }
    }

    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }
}
