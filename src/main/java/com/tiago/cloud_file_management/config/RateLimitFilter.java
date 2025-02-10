package com.tiago.cloud_file_management.config;

import io.github.bucket4j.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    @Override
    protected void doFilterInternal(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse  response,
                                    jakarta.servlet.FilterChain filterChain) throws IOException, ServletException {

        String clientIp = request.getRemoteAddr();

        buckets.putIfAbsent(clientIp, createNewBucket());
        Bucket bucket = buckets.get(clientIp);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Muitas requisicoes, tente novamente mais tarde. (Voce pode fazer apenas 8 requisicoes por minuto)");
            response.getWriter().flush();
        }
    }

    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(8, Refill.greedy(8, Duration.ofMinutes(1))))
                .build();
    }
}

