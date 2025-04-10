package pt.apdc.individual63431.util;

import java.util.UUID;

public class AuthToken {

    private static final long EXPIRATION_TIME = 1000*60*60*2;

    private String user;
    private String role;
    private String validFrom;
    private String validTo;

    public AuthToken() {

    }

    public AuthToken(String username, String role) {
        this.username = username;
        this.role = role;
        this.tokenID = UUID.randomUUID().toString();
        this.validFrom = System.currentTimeMillis();
        this.validTo = this.validFrom + EXPIRATION_TIME;

    }

    public String getTokenID() {
        return tokenID;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

}
