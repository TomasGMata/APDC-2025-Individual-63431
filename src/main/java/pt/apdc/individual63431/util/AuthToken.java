package pt.apdc.individual63431.util;

import java.util.UUID;

public class AuthToken {

    private static final long EXPIRATION_TIME = 1000*60*60*2;

    private String username;
    private String role;
    private long validFrom;
    private long validTo;
    private String tokenID;

    public AuthToken() {

    }

    public AuthToken(String username, String role) {
        this.username = username;
        this.tokenID = UUID.randomUUID().toString();
        this.validFrom = System.currentTimeMillis();
        this.validTo = this.validFrom + EXPIRATION_TIME;

    }
    
    public String getUsername() {
    	return username;
    }
    
    public String getRole() {
    	return role;
    }

    public String getTokenID() {
        return tokenID;
    }

    public long getValidFrom() {
        return validFrom;
    }

    public long getValidTo() {
        return validTo;
    }

}
