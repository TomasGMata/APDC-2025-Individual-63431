package pt.apdc.individual63431.util;

import com.google.cloud.datastore.Entity;

public class RemoveAccountRequest {
	
	private AuthToken token;
	private String targetUsername;
	
	public RemoveAccountRequest () {
	}

	public AuthToken getToken() { return token; }
	
	public String getUsername() { return targetUsername; }
	
	public AuthToken setToken(AuthToken token) { return this.token = token; }
	
	public String setUsername(String targetUsername) { return this.targetUsername = targetUsername; }
	
	public boolean hasPermissionToRemove(Entity targetUser) {
		String requesterRole = token.getRole();
		String targetRole = targetUser.getString("role");
		if("admin".equals(requesterRole)) return true;
		else if ("backoffice".equals(requesterRole)) { return "enduser".equals(targetRole) || "partner".equals(targetRole); }
		else return false;
	}	
}
