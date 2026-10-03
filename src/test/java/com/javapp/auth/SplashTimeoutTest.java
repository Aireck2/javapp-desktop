package com.javapp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.javapp.config.AppConfig;
import org.junit.jupiter.api.Test;

class SplashTimeoutTest {

    @Test
    void minimoVisible_porDefecto1500ms() {
        assertThat(AppConfig.splashMinMillis()).isEqualTo(1500L);
    }

    @Test
    void settle_respetaMinimo() {
        assertThat(SplashView.remainingMillis(1000L, 1200L, 1500L)).isEqualTo(1300L);
        assertThat(SplashView.remainingMillis(1000L, 3000L, 1500L)).isEqualTo(0L);
    }
}
