package com.kritik.POS.restaurant.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageUrlUtilTest {

    @Test
    void toClientUrlReturnsNullForBlankValues() {
        assertThat(ProductImageUrlUtil.toClientUrl(null)).isNull();
        assertThat(ProductImageUrlUtil.toClientUrl("   ")).isNull();
    }

    @Test
    void toClientUrlKeepsHttpUrlsUntouched() {
        assertThat(ProductImageUrlUtil.toClientUrl("https://cdn.example.com/menu/burger.png"))
                .isEqualTo("https://cdn.example.com/menu/burger.png");
    }

    @Test
    void toClientUrlNormalizesLegacyRelativeUploadPaths() {
        assertThat(ProductImageUrlUtil.toClientUrl("uploads\\\\burger.png"))
                .isEqualTo("/uploads/burger.png");
        assertThat(ProductImageUrlUtil.toClientUrl("uploads/pizza.png"))
                .isEqualTo("/uploads/pizza.png");
    }

    @Test
    void toClientUrlExtractsUploadsSegmentFromAbsolutePath() {
        assertThat(ProductImageUrlUtil.toClientUrl("C:/apps/pos/uploads/noodles.png"))
                .isEqualTo("/uploads/noodles.png");
    }

    @Test
    void toClientUrlPrefixesBareFileNames() {
        assertThat(ProductImageUrlUtil.toClientUrl("coffee.png"))
                .isEqualTo("/uploads/coffee.png");
    }
}
