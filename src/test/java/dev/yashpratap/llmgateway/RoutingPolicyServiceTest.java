package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.domain.RoutingPolicy;
import dev.yashpratap.llmgateway.domain.Tenant;
import dev.yashpratap.llmgateway.routing.RoutingPolicyRepository;
import dev.yashpratap.llmgateway.routing.RoutingPolicyService;
import dev.yashpratap.llmgateway.routing.dto.RoutingPolicyResponse;
import dev.yashpratap.llmgateway.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoutingPolicyService}.
 */
@ExtendWith(MockitoExtension.class)
class RoutingPolicyServiceTest {

    @Mock
    RoutingPolicyRepository routingPolicyRepository;

    @Mock
    TenantRepository tenantRepository;

    RoutingPolicyService routingPolicyService;

    @BeforeEach
    void setUp() {
        routingPolicyService = new RoutingPolicyService(routingPolicyRepository, tenantRepository);
    }

    @Test
    void testGetPolicyForTenant_noPolicyConfigured_returnsPriorityDefault() {
        when(routingPolicyRepository.findByTenantId(any())).thenReturn(Optional.empty());

        RoutingPolicyResponse response = routingPolicyService.getPolicyForTenant(UUID.randomUUID());

        assertThat(response.strategy()).isEqualTo("PRIORITY");
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void testGetPolicyForTenant_policyExists_returnsConfiguredStrategy() {
        UUID tenantId = UUID.randomUUID();
        RoutingPolicy policy = new RoutingPolicy();
        policy.setStrategy("COST");
        policy.setUpdatedAt(LocalDateTime.now());

        when(routingPolicyRepository.findByTenantId(tenantId)).thenReturn(Optional.of(policy));

        RoutingPolicyResponse response = routingPolicyService.getPolicyForTenant(tenantId);

        assertThat(response.strategy()).isEqualTo("COST");
    }

    @Test
    void testUpdatePolicy_invalidStrategy_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                routingPolicyService.updatePolicy(UUID.randomUUID(), "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid routing strategy");
    }

    @Test
    void testUpdatePolicy_tenantNotFound_throwsEntityNotFoundException() {
        when(tenantRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() ->
                routingPolicyService.updatePolicy(UUID.randomUUID(), "COST"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void testUpdatePolicy_validStrategy_savesAndReturnsPolicy() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        when(routingPolicyRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        RoutingPolicy saved = new RoutingPolicy();
        saved.setStrategy("LATENCY");
        saved.setUpdatedAt(LocalDateTime.now());
        when(routingPolicyRepository.save(any())).thenReturn(saved);
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(new Tenant());

        RoutingPolicyResponse response = routingPolicyService.updatePolicy(tenantId, "LATENCY");

        assertThat(response.strategy()).isEqualTo("LATENCY");
    }
}
