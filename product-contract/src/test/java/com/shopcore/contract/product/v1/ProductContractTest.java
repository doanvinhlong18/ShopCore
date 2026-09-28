package com.shopcore.contract.product.v1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductContractTest {

    @Test
    void shouldGenerateMessagesAndBlockingStub() {
        var request = GetProductRequest.newBuilder()
                .setProductId("00000000-0000-0000-0000-000000000001")
                .build();

        assertThat(request.getProductId()).isNotBlank();
        assertThat(ProductServiceGrpc.ProductServiceBlockingStub.class.getSimpleName())
                .isEqualTo("ProductServiceBlockingStub");
        assertThat(ProductServiceGrpc.getServiceDescriptor().getMethods()).hasSize(3);
    }
}
