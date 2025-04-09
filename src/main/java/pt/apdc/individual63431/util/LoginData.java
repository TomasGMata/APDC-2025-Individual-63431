package pt.apdc.individual63431.util;

public class LoginData {

    public String username;
    public String password;
    public String email;

    public LoginData() {
    }

    public LoginData(String username, String email, String password) {
    	this.email = email;
    	this.username = username;
    	this.password = password;
    }

}
