package pt.apdc.individual63431.util;

public class RequestManagementChange {

    private String targetUsername;
    private String userChange;
    private AuthToken token;

    public RequestManagementChange() {}

    public RequestManagementChange(String targetUsername, String userChange, AuthToken token) {
        this.targetUsername = targetUsername;
        this.userChange = userChange;
        this.token = token;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public String getUserChange() {
        return userChange;
    }
    
    public AuthToken getToken() {
    	return token;
    }
}
