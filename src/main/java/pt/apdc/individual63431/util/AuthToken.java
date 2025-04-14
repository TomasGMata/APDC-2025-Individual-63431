package pt.apdc.individual63431.util;

import java.util.UUID;

public class AuthToken {

    private static final long EXPIRATION_TIME = 1000*60*60*2;

    private String username;
    private String role;
    private ValidTime VALID;
    private  String verifier;
    private String tokenID;
    
    public static class ValidTime {
    	public final long VALID_FROM;
    	public final long VALID_TO;
    	
    	public ValidTime(long valFrom, long valTo) {
    		this.VALID_FROM = valFrom;
    		this.VALID_TO = valTo;
    	}
    }
    
    public AuthToken() {
    }

    public AuthToken(String username, String role) {
        this.username = username;
        this.tokenID = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        this.VALID = new ValidTime(currentTime, currentTime + EXPIRATION_TIME);
        this.verifier = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

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

    public ValidTime getValid() {
        return VALID;
    }
    
    public String getVerifier() {
        return verifier;
    }

}
