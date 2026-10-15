package com.example.ShopFlow.config;

import com.example.ShopFlow.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    private static final long MAX_REQUEST = 5;
    private static final long WINDOW_SECONDS = 60;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        String clientIp = extractClientIp(request);
        String route = request.getRequestURI();
        String redisKey = "rate:" + route + ":" + clientIp;

        boolean allowed = rateLimiterService.isAllowed(redisKey, MAX_REQUEST, WINDOW_SECONDS);

        if (!allowed){
            long waitTimeSeconds = rateLimiterService.getTtlInSeconds(redisKey);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(waitTimeSeconds));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = """
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Limite de requisições excedido. Tente novamente em %d segundos."
                }
                """.formatted(waitTimeSeconds);

            response.getWriter().write(jsonResponse);
            return false;

        }

        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }



}
