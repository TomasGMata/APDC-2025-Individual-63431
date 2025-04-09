package pt.apdc.individual63431.util;

public class UserData {

    public String username;
    public String email;
    public String fullName;
    public String phoneNumber;
    public String privacy;

    public String ccNumber;
    public String role;
    public String NIF;
    public String company;
    public String jobTitle;
    public String address;
    public String companyNIF;
    public String state;

    public UserData() {
    }

    public UserData(String username) {
    	this.username = username;
    }

    public boolean isDataValid() {
        return username != null && email != null && fullName != null && phoneNumber != null && privacy != null;
    }
}
