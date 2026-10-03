package com.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TokenStoreTest {

    private final Preferences testNode =
            Preferences.userRoot().node("com/javapp/desktop-test-tokenstore");
    private final TokenStore store = new TokenStore(testNode);

    @AfterEach
    void cleanup() {
        testNode.remove(TokenStore.KEY_REFRESH);
    }

    @Test
    void roundTrip_refreshToken() {
        assertThat(store.loadRefreshToken()).isEmpty();
        store.saveRefreshToken("r-123");
        assertThat(store.loadRefreshToken()).contains("r-123");
        store.clear();
        assertThat(store.loadRefreshToken()).isEmpty();
    }

    @Test
    void saveNull_clears() {
        store.saveRefreshToken("x");
        store.saveRefreshToken(null);
        assertThat(store.loadRefreshToken()).isEmpty();
    }
}
