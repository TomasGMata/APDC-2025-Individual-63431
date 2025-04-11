package pt.apdc.individual63431.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
 

public class UserData {

    public String username;
    public String email;
    public String fullName;
    public String password;
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

    public UserData(Entity entity) {
        this.email = entity.getString("email");
        this.username = entity.getString("username");
        this.fullName = entity.getString("fullName");
        this.phoneNumber = entity.getString("phoneNumber");
        this.privacy = entity.getString("privacy");
        this.state = entity.getString("state");
        this.role = entity.getString("role");
    }

    public UserData(String username, String email, String fullName, String password,
                    String phoneNumber, String privacy, String ccNumber, String role,
                    String NIF, String company, String jobTitle, String address,
                    String companyNIF, String state) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.privacy = privacy;
        this.ccNumber = ccNumber;
        this.role = role;
        this.NIF = NIF;
        this.company = company;
        this.jobTitle = jobTitle;
        this.address = address;
        this.companyNIF = companyNIF;
        this.state = state;
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
