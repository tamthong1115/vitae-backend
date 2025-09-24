package com.chill_guys.vitae_backend.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpResolver {
    private IpResolver() {
    }

    public static InetAddress resolveInet(HttpServletRequest req) {
        String ip = firstNonBlank(
                headerIp(req, "CF-Connecting-IP"),
                firstXff(req.getHeader("X-Forwarded-For")),
                parseForwarded(req.getHeader("Forwarded")),
                req.getRemoteAddr()
        );
        return toInetOrNull(ip);
    }

    private static String firstXff(String xff) {
        if (xff == null || xff.isBlank()) return null;
        return cleanIp(xff.split(",")[0].trim());
    }

    private static String parseForwarded(String fwd) {
        if (fwd == null || fwd.isBlank()) return null;
        int i = fwd.toLowerCase().indexOf("for=");
        if (i < 0) return null;
        String s = fwd.substring(i + 4).trim();
        if (s.startsWith("\"")) s = s.substring(1);
        int end = s.indexOf(';');
        if (end < 0) end = s.indexOf(',');
        if (end > 0) s = s.substring(0, end);
        s = s.replace("\"", "").trim();
        return cleanIp(s);
    }

    private static String cleanIp(String ip) {
        if (ip == null) return null;
        if (ip.startsWith("[") && ip.endsWith("]")) ip = ip.substring(1, ip.length() - 1); // IPv6 [::1]
        int pct = ip.indexOf('%'); // strip IPv6 scope (fe80::1%eth0)
        return pct > 0 ? ip.substring(0, pct) : ip;
    }

    private static InetAddress toInetOrNull(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String headerIp(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return (v == null || v.isBlank()) ? null : cleanIp(v.trim());
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
