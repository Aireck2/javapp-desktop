package com.app.auth;

import java.util.Optional;
import java.util.prefs.Preferences;

/** Refresh Token en almacén del SO vía Preferences (Registry/Keychain/SecretService). */
public class TokenStore {

    static final String NODE = "com/javapp/desktop";
    static final String KEY_REFRESH = "refresh_token";

    private final Preferences prefs;

    public TokenStore() {
        this(Preferences.userRoot().node(NODE));
    }

    TokenStore(Preferences prefs) {
        this.prefs = prefs;
    }

    public void saveRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            prefs.remove(KEY_REFRESH);
        } else {
            prefs.put(KEY_REFRESH, refreshToken);
        }
    }

    public Optional<String> loadRefreshToken() {
        return Optional.ofNullable(prefs.get(KEY_REFRESH, null));
    }

    public void clear() {
        prefs.remove(KEY_REFRESH);
    }
}
