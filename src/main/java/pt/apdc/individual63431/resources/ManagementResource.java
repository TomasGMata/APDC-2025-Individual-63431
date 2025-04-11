	package pt.apdc.individual63431.resources;
	
	import jakarta.ws.rs.Consumes;
	import jakarta.ws.rs.GET;
	import jakarta.ws.rs.POST;
	import jakarta.ws.rs.Path;
	import jakarta.ws.rs.PathParam;
	import jakarta.ws.rs.Produces;
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
	
	@Path("/{username}")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public class ManagementResource {
	
	    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	    private final Gson g = new Gson();
	
	    public ManagementResource() {
	
	    }
	
	    @POST
	    @Path("/manageRoles")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response changeUserRole(RequestManagementChange rq) {
	    	try {
	    		AuthToken token = isTokenValid(rq.getToken());
		        if(token==null) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn´t logged").build();
		    	}
		        if(!rq.hasPermissionForRequestRole()) {
		            return Response.status(Status.FORBIDDEN).entity("User isn't authorized to change this user role").build();
		        }

		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(rq.getTargetUsername());
		        Transaction txn = datastore.newTransaction();
		        
		        try {
		        	Entity targetUsr = txn.get(targetKey);
			        
			        if(targetUsr==null) {
			        	return Response.status(Status.BAD_REQUEST).entity("Target user doesn't exist").build();
			        }
		        	
		        	boolean isEnduserTargetValid = (targetUsr.getString("role").equals("partner") && rq.getUserChange().equals("enduser"));
		        	boolean isPartnerTargetValid = (targetUsr.getString("role").equals("enduser") && rq.getUserChange().equals("partner"));
		        	if(token.getRole().equals("backoffice") && (!isEnduserTargetValid && !isPartnerTargetValid)) {
		        		return Response.status(Status.FORBIDDEN).entity("User isn't authorized to change this user role").build();
		        	}
		        	
		        	Entity updatedUser = Entity.newBuilder(targetUsr)
		            		.set("role", rq.getUserChange()).build();
		        	txn.update(updatedUser);
		        	txn.commit();
		        	
		        	LOG.info("Role changed with success");
	        		return Response.ok().entity("User role updated successfully").build();
		        }finally {
		        	if (txn.isActive()) txn.rollback();
		        }
	    	} catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing user role", e);
	            return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    @POST
	    @Path("/manageStates")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response changeUserState(RequestManagementChange rq) {
	    	try {
	    		AuthToken token = isTokenValid(rq.getToken());
		        if(token==null) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
		    	}
		        if(!rq.hasPermissionForRequestRole()) {
		            return Response.status(Status.FORBIDDEN).entity("User isn't authorized to change this user state").build();
		        }

		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(rq.getTargetUsername());
		        Transaction txn = datastore.newTransaction();
		        
		        try {
		        	Entity targetUsr = txn.get(targetKey);
			        
			        if(targetUsr==null) {
			        	return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
			        }
			        
		        	boolean isActivateValid = targetUsr.getString("state").equals("DESATIVADA") && rq.getUserChange().equals("ATIVADA");
		        	boolean isDesactivateValid = targetUsr.getString("state").equals("ATIVADA") && rq.getUserChange().equals("DESATIVADA");
		        	if(targetUsr.getString("role").equals("backoffice") && !isActivateValid && !isDesactivateValid) {
		        		return Response.status(Status.FORBIDDEN).entity("User isn't authorized to change this user role").build();
		        	}
		        	
		        	Entity updatedUser = Entity.newBuilder(targetUsr)
		            		.set("state", rq.getUserChange()).build();
		        	txn.update(updatedUser);
		        	txn.commit();
		        	
		        	LOG.info("State changed with success");
	        		return Response.ok().entity("User state updated successfully").build();
		        } finally {
		        	if (txn.isActive())
		        		txn.rollback();
		        }
	    	} catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing user state", e);
	            return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }

	    @POST
	    @Path("/removeAccount")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response removeUserAccount(RemoveAccountRequest request) {
	    	try {
	    		AuthToken token = isTokenValid(request.getToken());
		        if(token==null) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
		    	}
		        
		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(request.getUsername());
		        Transaction txn = datastore.newTransaction();
		        
		        try {
		        	Entity targetUser = txn.get(targetKey);
		        	
		        	if(targetUser == null) {
		        		return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
		        	}
		        	
		        	if(!request.hasPermissionToRemove(targetUser)) {
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
	    	} catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error removing user account", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    @POST
	    @Path("/listUsers")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response listUsers(AuthToken token) {
	        try {
	        	AuthToken at = isTokenValid(token);
	        	if(at == null) {
	        		return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	        	}
	        	
	        	boolean isBackoffice = "backoffice".equalsIgnoreCase(at.getRole());
	        	boolean isAdmin = "admin".equalsIgnoreCase(at.getRole());
	        	boolean isEnduser = "enduser".equalsIgnoreCase(at.getRole());

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
	            	
	            	if(isAdmin || isBackoffice && role.equalsIgnoreCase("enduser")) {
	            		
	            	}
	            	
	            }
	            
	            return Response.ok().entity(userList).build();
	        	
	        } catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error listing user accounts", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    
	    @POST
	    @Path("/updateAccount")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response updateUserData(UserData newData, AuthToken t, @PathParam("username") String targetUsername) {
	        try {
	        	AuthToken at = isTokenValid(t);
	        	if(at == null) {
	        		return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	        	}
	        	
	        	Key targetKey = datastore.newKeyFactory()
                        .setKind(UserEntity.Kind)
                        .newKey(targetUsername);
	        	Transaction txn = datastore.newTransaction();
	        	
	        	try {
	        		Entity targetUser = txn.get(targetKey);
	        		if(targetUser == null) {
		        		return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
		        	}
	        		
	        		if(!hasUpdatePermission(at, targetUser, newData)) {
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

	        		if ("admin".equalsIgnoreCase(at.getRole())) {
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
	        } catch (Exception e) {
	    		LOG.log(Level.SEVERE, "Error updating user accounts", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    @POST
	    @Path("/changePassword")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response changePassword(AuthToken token, ChangePasswordRequest rq) {
	    	try {
	        	AuthToken at = isTokenValid(token);
	        	if(at == null) {
	        		return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	        	}
	        	
	        	if(!rq.isChangeValid()) {
	        		return Response.status(Status.BAD_REQUEST).entity("Password change is invalid").build();
	        	}
	        	if(!rq.isNewPasswordValid()) {
	        		return Response.status(Status.BAD_REQUEST).entity("Password isn't secure enough").build();
	        	}
	        	
	        	Key userKey = datastore.newKeyFactory()
                        .setKind(UserEntity.Kind)
                        .newKey(at.getUsername());
	        	Transaction txn = datastore.newTransaction();
	        	
	        	try {
	        		Entity user = txn.get(userKey);
	        		
	        		String currentPass = user.getString("password");
	        		String passwordRequest = DigestUtils.sha1Hex(rq.getCurrentPassword());
	        		
	        		if(currentPass.equals(passwordRequest)) {
	        			return Response.status(Status.BAD_REQUEST).entity("Current password incorrect").build();
	        		}
	        		
	        		String newPassword = DigestUtils.sha1Hex(rq.getNewPassword());
	        		Entity updatedUser = Entity.newBuilder(user).set("password", newPassword).build();
	        		
	        		txn.update(updatedUser);
	        		txn.commit();
	        		
	        		LOG.info("password changed with success");
	        		return Response.ok().entity("Your password has been changed").build();
	        	} finally {
	        		if(txn.isActive())
	        			txn.rollback();
	        	}
	    	} catch(Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing password", e);
	    		return Response.status(Status.BAD_REQUEST).build();
	    	}
	    }
	    
	    private AuthToken isTokenValid(AuthToken token) {
	    	return null;
	    }
	    
	    
	    private boolean hasUpdatePermission(AuthToken token, Entity targetUser, UserData newData) {
	    	String requesterRole = token.getRole();
	    	String targetRole = targetUser.getString("role");
	    	boolean updateMySelf = token.getUsername().equals(targetUser.getString("username"));
	    	
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
	    
	}
