package pt.apdc.individual63431.util;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;

public class UserEntity {

    public static final String Kind = "User";
    
    private String username;
    private String email;
    private String fullName;
    private String password;
    private String phoneNumber;
    private String privacy;

    private String ccNumber;
    private String role;
    private String NIF;
    private String company;
    private String jobTitle;
    private String address;
    private String companyNIF;
    private String state;
    
    public UserEntity() {
    }

    public UserEntity(Entity entity) {
        this.email = entity.getString("email");
        this.username = entity.getString("username");
        this.fullName = entity.getString("fullName");
        this.phoneNumber = entity.getString("phoneNumber");
        this.privacy = entity.getString("privacy");
        this.state = entity.getString("state");
        this.role = entity.getString("role");
        this.ccNumber = entity.getString("ccNumber");
        this.NIF = entity.getString("NIF");
        this.company = entity.getString("company");
        this.jobTitle = entity.getString("jobTitle");
        this.address = entity.getString("address");
        this.companyNIF = entity.getString("companyNIF");
    }
    
    public boolean isDataValid() {
        return username != null && email != null && fullName != null
                && phoneNumber != null && privacy != null;
    }
    
    public boolean isEmailValid() {
    	return email.contains("@");
    }
    
    public boolean isPasswordValid() {
    	return password.length() > 6;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getCcNumber() {
        return ccNumber;
    }

    public void setCcNumber(String ccNumber) {
        this.ccNumber = ccNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompanyNIF() {
        return companyNIF;
    }

    public void setCompanyNIF(String companyNIF) {
        this.companyNIF = companyNIF;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
