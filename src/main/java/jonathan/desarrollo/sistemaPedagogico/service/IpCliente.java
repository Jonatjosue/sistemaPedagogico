package jonathan.desarrollo.sistemaPedagogico.service;

import jakarta.servlet.http.HttpServletRequest;

public final class IpCliente {

    private IpCliente() {}

    public static String de(HttpServletRequest req) {
        String ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        ip = ip.trim();
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}