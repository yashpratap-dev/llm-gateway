package dev.yashpratap.llmgateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.common.ApiResponse;
import dev.yashpratap.llmgateway.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts the bearer API key from the {@code Authorization} header
 * and delegates validation to the {@link AuthenticationManager}.
 *
 * <p>On success, the authenticated {@link ApiKeyAuthenticationToken} is stored in the
 * {@link SecurityContextHolder} and the resolved tenant is written into {@link TenantContext}
 * for use by downstream services. On failure, a {@code 401 Unauthorized} JSON response
 * is returned immediately without continuing the filter chain.</p>
 *
 * <p>Paths excluded from filtering (via {@link #shouldNotFilter}): {@code /actuator/**},
 * {@code /swagger-ui/**}, {@code /api-docs/**}, {@code /api/v1/health/**},
 * {@code /swagger-ui.html}, and {@code POST /api/v1/admin/tenants} (bootstrap endpoint).</p>
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final AuthenticationManager authenticationManager;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the filter with all required collaborators.
     *
     * @param authenticationManager the manager that validates the API key token
     * @param tenantContext         the request-scoped context holder to populate on success
     * @param objectMapper          Jackson mapper for serialising the error response body
     */
    public ApiKeyFilter(AuthenticationManager authenticationManager,
                        TenantContext tenantContext,
                        ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    /**
     * Skips this filter for public paths that do not require an API key.
     *
     * @param request the incoming HTTP request
     * @return {@code true} when the request path is publicly accessible
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean isPublicBootstrapPost = PATH_MATCHER.match("/api/v1/admin/tenants", path)
                && "POST".equalsIgnoreCase(request.getMethod());

        return PATH_MATCHER.match("/actuator/**", path)
                || PATH_MATCHER.match("/swagger-ui/**", path)
                || PATH_MATCHER.match("/api-docs/**", path)
                || PATH_MATCHER.match("/api/v1/health/**", path)
                || PATH_MATCHER.match("/swagger-ui.html", path)
                || isPublicBootstrapPost;
    }

    /**
     * Extracts the bearer token, authenticates it, and populates the security context.
     *
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet-level error occurs
     * @throws IOException      if an I/O error occurs writing the error response
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String rawKey = header.substring(BEARER_PREFIX.length());

        try {
            Authentication authenticated = authenticationManager.authenticate(
                    new ApiKeyAuthenticationToken(rawKey));

            SecurityContextHolder.getContext().setAuthentication(authenticated);

            ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authenticated;
            tenantContext.setTenant(authToken.getTenant());
            tenantContext.setApiKeyId(authToken.getApiKeyId());

            filterChain.doFilter(request, response);
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "Invalid API key");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(message, "UNAUTHORIZED");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
