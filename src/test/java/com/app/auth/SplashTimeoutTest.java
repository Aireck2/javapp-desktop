package com.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.config.AppConfig;
import org.junit.jupiter.api.Test;

class SplashTimeoutTest {

    @Test
    void minimoVisible_porDefecto5000ms() {
        assertThat(AppConfig.splashMinMillis()).isEqualTo(5000L);
    }

    @Test
    void settle_respetaMinimo() {
        assertThat(SplashView.remainingMillis(1000L, 1200L, 5000L)).isEqualTo(4800L);
        assertThat(SplashView.remainingMillis(1000L, 6000L, 5000L)).isEqualTo(0L);
    }
}
