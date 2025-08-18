package com.example.grafanaautobuilder.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. Check for Authorization header
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Extract JWT from header
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            // 3. Validate and set authentication
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception e) {
                    // Log the exception but don't block the request
                    System.err.println("Error processing JWT token: " + e.getMessage());
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Log the exception but don't block the request
            System.err.println("Error in JWT filter: " + e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
}

// // The JwtAuthFilter is a Spring Security filter that handles JWT (JSON Web Token) authentication for incoming HTTP requests. Its main purposes are:
// Extract & Validate JWT – Checks the Authorization header for a valid Bearer token.
// Authenticate Requests – If the JWT is valid, it loads user details and sets up Spring Security's authentication context.
// Secure API Access – Ensures only authenticated users with valid tokens can access protected endpoints.
// Stateless Security – Works without server-side sessions, relying only on the JWT for authentication.