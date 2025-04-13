	package pt.apdc.individual63431.resources;
	
	import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
	import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
	import jakarta.ws.rs.Path;
	import jakarta.ws.rs.PathParam;
	import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
	import jakarta.ws.rs.core.Response;
	import jakarta.ws.rs.core.Response.Status;
	
	import pt.apdc.individual63431.util.AuthToken;
import pt.apdc.individual63431.util.ChangePasswordRequest;
import pt.apdc.individual63431.util.RemoveAccountRequest;
	import pt.apdc.individual63431.util.RequestManagementChange;
	import pt.apdc.individual63431.util.UserData;
	import pt.apdc.individual63431.util.UserEntity;
	
	import com.google.cloud.datastore.*;
	import com.google.gson.Gson;

	import java.util.logging.Logger;
	import java.util.ArrayList;
	import java.util.List;
	import java.util.logging.Level;
	import org.apache.commons.codec.digest.DigestUtils;
	
	@Path("/user-management")
	public class ManagementResource {
	
	    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	    private final Gson g = new Gson();
	
	    public ManagementResource() {
	
	    }
	    
	    @POST
	    @Path("/register")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response registerUser(UserData data) {
	    	try {
	        	
	        	LOG.fine("Attempt to register User: " + data.username);
	            if(!data.isDataValid()) {
	                return Response.status(Status.FORBIDDEN).entity("Missing required fields.").build();
	            }
	            if (!data.isEmailValid()) {
	            	LOG.warning("Invalid email: " + data.getEmail());
	            	return Response.status(Status.BAD_REQUEST).entity("Your email is considered invalid").build();
	            }
	            if (!data.isPasswordValid()) {
	            	LOG.warning("Invalid password: " + data.getPassword());
	            	return Response.status(Status.BAD_REQUEST).entity("Your password is considered invalid").build();
	            }
	            
	            Key userKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(data.username);
	            Entity user = datastore.get(userKey);

	            if (user != null) {
	                return Response.status(Status.FORBIDDEN).entity("User arleady exists.").build();
	            }
	            
	            data.role="enduser";
	            data.state="DESATIVADA";
	            Entity newUser = Entity.newBuilder(userKey)
	            		.set("password", DigestUtils.sha256Hex(data.password))
	            		.set("username", data.username)
	            		.set("email", data.email)
	            		.set("fullName", data.fullName)
	            		.set("phoneNumber", data.phoneNumber)
	            		.set("privacy", data.privacy)
	            		.set("role", data.role)
	            		.set("state", data.state).build();
	            datastore.put(newUser);
	            
	            LOG.info("User has been registred successfully");
	            return Response.ok().entity("User registred").build();
	    	} catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastore\"}").build();
	    	} catch (Exception e) {
	    		LOG.severe("unknown err");
	    		return Response.serverError().entity("{\"error\": \"unknown error\"}").build();
	    	}
	    	
	    }
	    
		@POST
	    @Path("/manage-roles")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response changeUserRole(@HeaderParam("Authorization") String tokenID,
	    		@QueryParam("target") String targetUsername, @QueryParam("newRole") String newRole) {
	        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	        
	    	try {
	    		Entity tokenEntity = datastore.get(tokenKey);
	    		if(!isTokenValid(tokenEntity)) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn´t logged").build();
		    	}
		        
		        Key requesterKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(tokenEntity.getString("username"));
		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(targetUsername);
		        Transaction txn = datastore.newTransaction();
		        
		        try {
		        	Entity requesterUsr = txn.get(requesterKey);
		        	Entity targetUsr = txn.get(targetKey);
		        	if(targetUsr==null) {
			        	LOG.warning("targeted user not found");
			        	return Response.status(Status.BAD_REQUEST).entity("Target user doesn't exist").build();
			        }
			        
			        if(!hasPermissionForRequestRole(requesterUsr.getString("role"), targetUsr.getString("role"), newRole)) {
			            return Response.status(Status.FORBIDDEN).entity("User isn't authorized to change this user role").build();
			        }
		        	
		        	Entity updatedUser = Entity.newBuilder(targetUsr)
		            		.set("role", newRole).build();
		        	txn.update(updatedUser);
		        	txn.commit();
		        	
		        	LOG.info("Role changed with success");
	        		return Response.ok().entity("User role updated successfully").build();
		        }finally {
		        	if (txn.isActive()) txn.rollback();
		        }
		    
	    	} catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastore\"}").build();
	    	} catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing user role", e);
	            return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    @POST
	    @Path("/manage-states")
	    @Consumes(MediaType.APPLICATION_JSON)	    
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response changeUserState(@HeaderParam("Authorization") String tokenID,
	    		@QueryParam("target") String targetUsername, @QueryParam("newState") String newState) {
	        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	    	
	    	try {
	    		Entity tokenEntity = datastore.get(tokenKey);
	    		if(!isTokenValid(tokenEntity)) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
		    	}
		        
		        Key requesterKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(tokenEntity.getString("username"));
		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(targetUsername);
		        Transaction txn = datastore.newTransaction();
		        
		        try {
		        	Entity requesterUsr = txn.get(requesterKey);
		        	Entity targetUsr = txn.get(targetKey);
			        if(targetUsr==null) {
			        	return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
			        }
			        if(!hasPermissionForRequestState(requesterUsr.getString("role"), targetUsr.getString("role")
			        		, targetUsr.getString("state"), newState)) {
			            return Response.status(Status.FORBIDDEN).entity("User isn't authorized to change this user state").build();
			        }
		        	
		        	Entity updatedUser = Entity.newBuilder(targetUsr)
		            		.set("state", newState).build();
		        	txn.update(updatedUser);
		        	txn.commit();
		        	
		        	LOG.info("State changed with success");
	        		return Response.ok().entity("User state updated successfully").build();
		        } finally {
		        	if (txn.isActive())
		        		txn.rollback();
		        }
	    	} catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastore\"}").build();
	    	} catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing user state", e);
	            return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }

	    @POST
	    @Path("/remove-account")
	    @Consumes(MediaType.APPLICATION_JSON)	    
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response removeUserAccount(@HeaderParam("Authorization") String tokenID, @QueryParam("target") String targetUsername
	    		,@QueryParam("newPsw1") String newPsw1, @QueryParam("newPsw2") String newPsw2) {
	    	Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	    	
	    	try {
	    		Entity tokenEntity = datastore.get(tokenKey);
	    		if(!isTokenValid(tokenEntity)) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
		    	}
		        
		        Key requesterKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(tokenEntity.getString("username"));
		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(targetUsername);
		        Transaction txn = datastore.newTransaction();
		        
		        try {
		        	Entity requesterUser = txn.get(requesterKey);
		        	Entity targetUser = txn.get(targetKey);
		        	if(targetUser == null) {
		        		return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
		        	}
		        	if(!hasPermissionToRemove(requesterUser.getString("role"), targetUser.getString("role"))) {
		        		return Response.status(Status.FORBIDDEN).entity("User isn't authorized to remove this user's account").build();
		        	}
		        	
		        	txn.delete(targetKey);
		        	txn.commit();
		        	
		        	LOG.info("Removed with success");
		        	return Response.ok().entity("Account removed successfully").build();
		        } finally {
		        	if (txn.isActive())
		        		txn.rollback();
		        }
	    	} catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastore\"}").build();
	    	} catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error removing user account", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    @POST
	    @Path("/list-users")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response listUsers(@HeaderParam("Authorization") String tokenID) {
	    	Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	    	
	        try {
	        	Entity tokenEntity = datastore.get(tokenKey);
	        	if(!isTokenValid(tokenEntity)) {
	        		return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	        	}

	            Query<Entity> query = Query.newEntityQueryBuilder()
	                .setKind("User")
	                .build();

	            QueryResults<Entity> results = datastore.run(query);
	            List<UserData> userList = new ArrayList<>();
	            
	            while (results.hasNext()) {
	            	Entity user = results.next();
	            	String role = user.getString("role");
	            	String state = user.getString("state");
	            	String privacy = user.getString("privacy");
	            	
	            	/*if(isAdmin || isBackoffice && role.equalsIgnoreCase("enduser")) {
	            		
	            	}*/
	            	
	            }
	            
	            return Response.ok().entity(userList).build();
	        } catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastor\"}").build();
	        } catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error listing user accounts", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    
	    @POST
	    @Path("/update-account")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response updateUserData(UserData newData, @HeaderParam("Authorization") String tokenID, @QueryParam("target") String targetUsername) {
	    	Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	    	
	        try {
	        	Entity tokenEntity = datastore.get(tokenKey);
	        	if(!isTokenValid(tokenEntity)) {
	        		return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	        	}
	        	
	        	Key requesterKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(tokenEntity.getString("username"));
	        	Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(targetUsername);
	        	Transaction txn = datastore.newTransaction();
	        	
	        	try {
	        		Entity requesterUser = txn.get(requesterKey);
	        		Entity targetUser = txn.get(targetKey);
	        		if(targetUser == null) {
		        		return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
		        	}
	        		
	        		if(!hasAttributesUpdatePermission(requesterUser, targetUser, newData)) {
	        			return Response.status(Status.FORBIDDEN)
	                            .entity("User isn´t allowed to change this attributes.").build();
	        		}
	        		
	        		Entity.Builder udpated = Entity.newBuilder(targetUser);
	        		
	        		if (newData.getPhoneNumber() != null) {
	        			udpated.set("phoneNumber", newData.getPhoneNumber());
	        		}

	        		if (newData.getPrivacy() != null) {
	        			udpated.set("privacy", newData.getPrivacy());
	        		}

	        		if (newData.getCcNumber() != null) {
	        			udpated.set("ccNumber", newData.getCcNumber());
	        		}

	        		if (newData.getNIF() != null) {
	        			udpated.set("NIF", newData.getNIF());
	        		}

	        		if (newData.getCompany() != null) {
	        			udpated.set("company", newData.getCompany());
	        		}

	        		if (newData.getJobTitle() != null) {
	        			udpated.set("jobTitle", newData.getJobTitle());
	        		}

	        		if (newData.getAddress() != null) {
	        			udpated.set("address", newData.getAddress());
	        		}

	        		if (newData.getCompanyNIF() != null) {
	        			udpated.set("companyNIF", newData.getCompanyNIF());
	        		}

	        		if ("admin".equalsIgnoreCase(requesterUser.getString("role"))) {
	        		    if (newData.getRole() != null) {
	        		    	udpated.set("role", newData.getRole());
	        		    }
	        		    if (newData.getState() != null) {
	        		    	udpated.set("state", newData.getState());
	        		    }
	        		}
	        		Entity updatedUser = udpated.build();
	        		txn.update(updatedUser);
	        		txn.commit();
	        		
	        		LOG.info("Attributes changed with succes");
	        		return Response.ok().entity("User attributes updated successfully").build();
	        	} finally {
	        		if (txn.isActive())
		        		txn.rollback();
	        	}
	        } catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastor\"}").build();
	        } catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error updating user accounts", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    @POST
	    @Path("/change-password")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response changePassword(@HeaderParam("Authorization") String tokenID, @QueryParam("target") String targetUsername
	    		, @QueryParam("currentPsw") String currentPsw, @QueryParam("newPsw1") String newPsw1, @QueryParam("newPsw2") String newPsw2) {
	    	Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	    	
	    	try {
	        	Entity tokenEntity = datastore.get(tokenKey);
	        	if(!isTokenValid(tokenEntity)) {
	        		return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	        	}
	        	if((newPsw1!=null) && newPsw1.equals(newPsw2)) {
	        		return Response.status(Status.BAD_REQUEST).entity("Passwords dont match").build();
	        	}
	        	if(!isNewPasswordValid(newPsw1)) {
	        		return Response.status(Status.BAD_REQUEST).entity("Password isn't secure enough").build();
	        	}
	        	
	        	Key userKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(tokenEntity.getString("username"));
	        	Transaction txn = datastore.newTransaction();
	        	
	        	try {
	        		Entity user = txn.get(userKey);
	        		
	        		String currentPass = user.getString("password");
	        		String passwordRequest = DigestUtils.sha1Hex(currentPsw);
	        		
	        		if(!currentPass.equals(passwordRequest)) {
	        			return Response.status(Status.BAD_REQUEST).entity("Current password incorrect").build();
	        		}
	        		
	        		Entity updatedUser = Entity.newBuilder(user).set("password", DigestUtils.sha1Hex(newPsw1)).build();
	        		
	        		txn.update(updatedUser);
	        		txn.commit();
	        		
	        		LOG.info("password changed with success");
	        		return Response.ok().entity("Your password has been changed").build();
	        	} finally {
	        		if(txn.isActive())
	        			txn.rollback();
	        	}
	    	 } catch (DatastoreException e) {
		    		LOG.severe("Não conseguiu aceder a datastore");
		    		return Response.serverError().entity("{\"error\": \"error accessing datastor\"}").build();
	    	} catch(Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing password", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    private boolean isTokenValid(Entity token) {
	    	long curretTime = System.currentTimeMillis();
	    	return token == null || curretTime < token.getLong("validTo");
	    }
	    
	    @PostConstruct
	    private void initRootUser() {
	    	Transaction txn = datastore.newTransaction();
	    	
	    	try {
	    		
	    		Key rootKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey("root");

	            if (datastore.get(rootKey) != null) {
	                LOG.info("Root user already exists. Skipping initialization.");
	                return;
	            }

	            UserData rootUser = new UserData();
	            rootUser.username = "root";
	            rootUser.email = "t.mata@campus.fct.unl.pt";
	            rootUser.fullName = "System Administrator";
	            rootUser.phoneNumber = "000000000";
	            rootUser.password = "admin123";
	            rootUser.privacy = "private";
	            rootUser.role = "admin";
	            rootUser.state = "ATIVADA";
	            
	            Entity root = Entity.newBuilder(rootKey)
	            		.set("password", DigestUtils.sha1Hex(rootUser.password))
	            		.set("username", rootUser.username)
	            		.set("email", rootUser.email)
	            		.set("fullName", rootUser.fullName)
	            		.set("phoneNumber", rootUser.phoneNumber)
	            		.set("privacy", rootUser.privacy)
	            		.set("role", rootUser.role)
	            		.set("state", rootUser.state).build();
	            txn.put(root);
	            txn.commit();
	            
	            LOG.info("Root user created.");
	            
	    	} catch(DatastoreException e) {
	    		LOG.severe("error creating root admin");
	    		return;
	    	} finally {
	    		if(txn.isActive()) txn.rollback();
	    	}
	        
	    }
	    
	    
	    private boolean hasAttributesUpdatePermission(Entity requesterUser, Entity targetUser, UserData newData) {
	    	
	    	String requesterRole = requesterUser.getString("role");
	    	String targetRole = targetUser.getString("role");
	    	boolean updateMySelf = requesterUser.getString("username").equals(targetUser.getString("username"));
	    	
	    	if(requesterRole.equals("admin")) {
	    		return true;
	    	}
	    	
	    	if(requesterRole.equals("enduser") && updateMySelf) {
	    		
	    		if(newData.getUsername()!=null || newData.getEmail()!=null || newData.getFullName()!=null || 
	    				newData.getRole()!=null || newData.getPassword()!=null)
	    			return false;
	    		if(targetUser.getString("state").equals("DESATIVADA")) {
	    			return newData.getState().equals("ATIVADA");
	    		} return true;
	    	
	    	}
	    	
	    	if(requesterRole.equals("backoffice")) {
	    		
	    		if(!(targetRole.equals("enduser")) || !(targetRole.equals("partner"))) {
	    			return false;
	    		}
	    		if(newData.getUsername()!=null || newData.getEmail()!=null) {
	    			return false;
	    		}
	    		return true;
	    	}
	    	
	    	return false;
	    }
	    
	    private boolean hasPermissionForRequestRole (String requesterRole, String targetRole, String newRole) {
	        if (requesterRole.equals("admin")) return true;
	        else if (requesterRole.equals("backoffice"))
	            return (targetRole.equals("enduser") && newRole.equals("partner"))
	            		||(targetRole.equals("partner") && newRole.equals("enduser"));
	        else
	            return false;
	    }
	    
	    private boolean hasPermissionForRequestState(String requesterRole, String targetRole, String currentState, String newState) {
	    	if (requesterRole.equals("admin")) return true;
	        else if (requesterRole.equals("backoffice")) {
	            return (currentState.equals("ATIVADA")&&newState.equals("DESATIVADA"))
	            		||(currentState.equals("DESATIVADA")&&newState.equals("ATIVADA"));
	        }else
	        return false;
	    }
	    
	    private boolean hasPermissionToRemove(String requesterRole, String targetRole) {
			if("admin".equals(requesterRole)) return true;
			else if ("backoffice".equals(requesterRole)) { return "enduser".equals(targetRole) || "partner".equals(targetRole); }
			else return false;
		}
	    
	    private boolean isNewPasswordValid(String newpassword) {
			 return newpassword.length() > 6 && newpassword.matches(".*[a-zA-Z].*") && newpassword.matches(".*[0-9].*");
		 }
	    
	}
