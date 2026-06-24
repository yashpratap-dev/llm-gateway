package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.domain.Tenant;
import dev.yashpratap.llmgateway.security.ApiKeyAuthenticationToken;
import dev.yashpratap.llmgateway.security.ApiKeyFilter;
import dev.yashpratap.llmgateway.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApiKeyFilter}.
 *
 * <p>Calls the public {@code doFilter} entry point (not the protected
 * {@code doFilterInternal}) so the test compiles from a different package.</p>
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyFilterTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TenantContext tenantContext;

    private ApiKeyFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new ApiKeyFilter(authenticationManager, tenantContext, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFilter_missingAuthHeader_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/completions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void testFilter_invalidKey_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/completions");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer lgw_badkey");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid API key"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid API key");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testFilter_validKey_populatesSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/completions");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer lgw_validkey");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        Tenant tenant = new Tenant();
        UUID apiKeyId = UUID.randomUUID();
        ApiKeyAuthenticationToken authenticated = new ApiKeyAuthenticationToken(
                tenant, apiKeyId, List.of());

        when(authenticationManager.authenticate(any())).thenReturn(authenticated);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        verify(tenantContext).setTenant(tenant);
        verify(tenantContext).setApiKeyId(apiKeyId);
    }
}
