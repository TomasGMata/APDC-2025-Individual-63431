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

    public boolean hasPermissionForRequestRole () {
        if (token.getRole().equals("admin")) return true;
        else if (token.getRole().equals("backoffice") && (userChange.equals("enduser"))||(userChange.equals("partner")))
            return true;
        else
            return false;
    }
    
    public boolean hasPermissionForAccountStateChange() {
        String requesterRole = token.getRole().toUpperCase();
        
        if ("admin".equals(requesterRole)) {
            return true;
        }else if ("backoffice".equals(requesterRole) && (userChange.equals("ativada"))||(userChange.equals("desativada"))) {
            return true;
        }else
        return false;
    }
}
