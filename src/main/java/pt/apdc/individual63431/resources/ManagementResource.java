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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
	import jakarta.ws.rs.core.Response;
	import jakarta.ws.rs.core.Response.Status;
	
	import pt.apdc.individual63431.util.UserData;
	import pt.apdc.individual63431.util.UserEntity;
	
	import com.google.cloud.datastore.Entity;
	import com.google.cloud.datastore.Key;
	import com.google.cloud.datastore.DatastoreOptions;
	import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
	import com.google.cloud.datastore.Datastore;
	import com.google.cloud.datastore.Transaction;	
	import com.google.cloud.datastore.DatastoreException;
	import com.google.cloud.datastore.Query;
	import com.google.cloud.datastore.QueryResults;
	import com.google.gson.Gson;

	import java.util.logging.Logger;
	import java.util.regex.Pattern;
	import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
	import org.apache.commons.codec.digest.DigestUtils;
	
	@Path("/user-management")
	public class ManagementResource {
		
	    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	    private final Gson g = new Gson();
	    
	    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");
	    private static final List<String> ROLES = Arrays.asList("enduser","admin","backoffice","partner");
	    private static final List<String> STATES = Arrays.asList("ATIVADA","DESATIVADA","SUSPENSA");
	    
	    public ManagementResource() {
	
	    }
	    
	    @POST
	    @Path("/register")
	    @Consumes(MediaType.APPLICATION_JSON)
	    public Response registerUser(UserEntity userData, @QueryParam("confirmPassword") String confirmPassword) {
	    	try {
	        	
	        	LOG.fine("Attempt to register User: " + userData.getUsername());
	        	if (userData.getEmail() == null || !EMAIL_PATTERN.matcher(userData.getEmail()).matches()) {
	        		LOG.warning("Invalid email: " + userData.getEmail());
	            	return Response.status(Status.BAD_REQUEST).entity("Your email is considered invalid").build();
	        	}
	            if (userData.getPassword() == null || !PASSWORD_PATTERN.matcher(userData.getPassword()).matches()) {
	            	LOG.warning("Invalid password: " + userData.getPassword());
	            	return Response.status(Status.BAD_REQUEST).entity("Your password is considered invalid").build();
	            }
	            if(!userData.getPassword().equals(confirmPassword)) 
	            	return Response.status(Status.BAD_REQUEST).entity("Your passwords arent the same").build();
	            if(!userData.isDataValid()) {
	                return Response.status(Status.FORBIDDEN).entity("Missing required fields.").build();
	            }
	            
	            Key userKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(userData.getUsername());
	            Entity newUser = datastore.get(userKey);

	            if (newUser != null) {
	                return Response.status(Status.FORBIDDEN).entity("User arleady exists.").build();
	            }
	            
	            Query<Entity> emailQuery = Query.newEntityQueryBuilder()
	                    .setKind("User")
	                    .setFilter(PropertyFilter.eq("email", userData.getEmail()))
	                    .build();
	                
	            if (datastore.run(emailQuery).hasNext()) {
                    return Response.status(Status.BAD_REQUEST)
                        .entity("{\"error\": \"Email já registado\"}").build();
                }
	            
	            userData.setRole("enduser");
	            userData.setState("DESATIVADA");
	            Entity user = Entity.newBuilder(userKey)
	            		.set("password", DigestUtils.sha1Hex(userData.getPassword()))
	            		.set("username", userData.getUsername())
	            		.set("email", userData.getEmail())
	            		.set("fullName", userData.getFullName())
	            		.set("phoneNumber", userData.getPhoneNumber())
	            		.set("privacy", userData.getPrivacy())
	            		.set("role", userData.getRole())
	            		.set("state", userData.getState()).build();
	            datastore.put(user);
	            
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
	    		@QueryParam("target") String target, @QueryParam("newRole") String newRole) {
	        
	    	try {
	    		Entity tokenEntity = validateTokenAndRole(tokenID, "admin", "backoffice");
	    		String requesterRole = tokenEntity.getString("role");
	    		if(!isTokenValid(tokenEntity)) {
		        	return Response.status(Status.FORBIDDEN).entity("User isn´t logged").build();
		    	}
		        
		        Key targetKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(target);
		        Entity targetUsr = datastore.get(targetKey);
		        
		        if(targetUsr==null) {
		        	LOG.warning("targeted user not found");
		        	return Response.status(Status.BAD_REQUEST).entity("Target user doesn't exist").build();
		        }
		        String currentRole = targetUsr.getString("role");
		        if ("backoffice".equals(requesterRole)) {
	                if (!(("enduser".equals(currentRole) && "partner".equals(newRole)) || 
	                     ("partner".equals(currentRole) && "enduser".equals(newRole)))) {
	                    return Response.status(Status.FORBIDDEN)
	                        .entity("{\"error\": \"BACKOFFICE só pode mudar ENDUSER para PARTNER e vice-versa\"}").build();
	                }
	            }


	            Entity updatedUser = Entity.newBuilder(targetUsr)
	                .set("role", newRole)
	                .build();

	            Transaction txn = datastore.newTransaction();
	            try {
	                txn.update(updatedUser);
	                txn.commit();
		        
		        
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
	    public Response removeUserAccount(@HeaderParam("Authorization") String tokenID, @QueryParam("target") String targetUsername) {
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
	    	try {
	            Entity tokenEntity = validateTokenAndRole(tokenID, "enduser", "backoffice", "admin");
	            String requesterRole = tokenEntity.getString("role");

	            QueryResults<Entity> users = datastore.run(Query.newEntityQueryBuilder()
	                .setKind("User")
	                .build());

	            List<Map<String, String>> result = new ArrayList<>();

	            while (users.hasNext()) {
	                Entity user = users.next();
	                String userRole = user.getString("role");
	                String accountState = user.getString("accountState");
	                String profile = user.getString("privacy");

	                if (requesterRole.equals("admin") || 
	                    (requesterRole.equals("") && userRole.equals("enduser")) ||
	                    (requesterRole.equals("enduser") && userRole.equals("enduser") && 
	                     profile.equals("public") && accountState.equals(""))) {

	                    Map<String, String> userData = new HashMap<>();

	                    userData.put("username", user.getString("username"));
	                    userData.put("email", user.getString("email"));

	                    if (requesterRole.equals("admin") || requesterRole.equals("backoffice")) {
	                        userData.put("fullName", getValueOrNotDefined(user, "fullName"));
	                        userData.put("phone", getValueOrNotDefined(user, "phoneNumber"));
	                        userData.put("profile", profile);
	                        userData.put("role", userRole);
	                        userData.put("accountState", accountState);

	                        userData.put("ccNumber", getValueOrNotDefined(user, "ccNumber"));
	                        userData.put("NIF", getValueOrNotDefined(user, "NIF"));
	                        userData.put("employer", getValueOrNotDefined(user, "employer"));
	                        userData.put("jobTitle", getValueOrNotDefined(user, "jobTitle"));
	                        userData.put("address", getValueOrNotDefined(user, "address"));
	                        userData.put("employerNIF", getValueOrNotDefined(user, "employerNIF"));
	                    } else {
	                        userData.put("fullName", getValueOrNotDefined(user, "fullName"));
	                    }

	                    result.add(userData);
	                }
	            }

	            return Response.ok(new Gson().toJson(result)).build();

	        } catch (Exception e) {
	            return Response.serverError()
	                .entity("{\"error\": \"Erro ao listar utilizadores: " + e.getMessage() + "\"}")
	                .build();
	        }
	    }
	    
	    
	    private String getValueOrNotDefined(Entity user, String string) {
			// TODO Auto-generated method stub
			return null;
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
	    @Produces(MediaType.APPLICATION_JSON)
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
	    
	    @POST
	    @Path("/loggout")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response loggoutOfAccount(@HeaderParam("Authorization") String tokenID) {
	    	Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenID);
	    	
	    	try {
	    		Entity tokenEntity = datastore.get(tokenKey);
	    		if(isTokenValid(tokenEntity)) {
	    			return Response.status(Status.FORBIDDEN).entity("User isn't logged").build();
	    		}
	    		
	    		Transaction txn = datastore.newTransaction();
	    		try {
	    			Entity notLoggedToken = Entity.newBuilder(tokenEntity)
	    					.set("validTo", System.currentTimeMillis()-0.01).build();
	    			txn.update(notLoggedToken);
	    			txn.commit();
	    			
	    			LOG.info("User isnt logeed out");
	    			return Response.ok().entity("You have logged out successfully").build();
	    		} finally {
	    			if(txn.isActive()) txn.rollback();
	    		}
	    	} catch (DatastoreException e) {
	    		LOG.severe("Não conseguiu aceder a datastore");
	    		return Response.serverError().entity("{\"error\": \"error accessing datastor\"}").build();
	    	} catch(Exception e) {
	    		LOG.log(Level.SEVERE, "Error changing password", e);
	    		return Response.status(Status.BAD_REQUEST).build(); }
	    }
	    
	    private boolean isTokenValid(Entity token) {
	    	long currentTime = System.currentTimeMillis();
	    	return token != null && currentTime < token.getLong("validTo");
	    }
	    
	    private Entity validateTokenAndRole(String token, String... allowedRoles) {
	    	
	        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(token);
	        Entity tokenEntity = datastore.get(tokenKey);
	        
	        if (tokenEntity == null) {
	            throw new WebApplicationException(
	                Response.status(Status.UNAUTHORIZED).entity("{\"error\": \"invalid token\"}").build());
	        }

	        long validTo = tokenEntity.getLong("validTo");
	        if (System.currentTimeMillis() > validTo) {
	            throw new WebApplicationException(
	                Response.status(Status.UNAUTHORIZED).entity("{\"error\": \"expired token\"}").build());
	        }

	        String userRole = tokenEntity.getString("role");
	        if (allowedRoles.length > 0 && !Arrays.asList(allowedRoles).contains(userRole)) {
	            throw new WebApplicationException(
	                Response.status(Status.FORBIDDEN).entity("{\"error\": \"unauthorized access\"}").build());
	        }

	        return tokenEntity;
	    }
	    
	    @Path("/initRoot")
	    @GET
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response forceInitRootUser() {
	        try {
	            Key rootKey = datastore.newKeyFactory().setKind("User").newKey("root");
	            Entity rootUser = datastore.get(rootKey);
	            
	            if (rootUser == null) {
	                initRootUser();
	                rootUser = datastore.get(rootKey);
	            }

	            String tokenId = UUID.randomUUID().toString().replace("-", "");
	            long validTo = System.currentTimeMillis() + (3600 * 1000);
	            
	            Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(tokenId);
	            Entity authToken = Entity.newBuilder(tokenKey)
	                    .set("username", "root")
	                    .set("role", "admin")
	                    .set("creationTime", System.currentTimeMillis())
	                    .set("validTo", validTo)
	                    .build();
	            
	            datastore.put(authToken);
	            
	            LOG.info("Root user initialized and logged in successfully");
	            return Response.ok()
	                    .entity("{\"token\": \"" + tokenId + "\"}")
	                    .build();
	            
	        } catch (Exception e) {
	            LOG.severe("Error initializing root user: " + e.getMessage());
	            return Response.serverError()
	                    .entity("{\"error\": \"Failed to initialize root user\"}")
	                    .build();
	        }
	    }
	    
	    @PostConstruct
	    public void initRootUser() {
	        Transaction txn = datastore.newTransaction();
	        try {
	            Key rootKey = datastore.newKeyFactory().setKind("User").newKey("root");
	            if (txn.get(rootKey) == null) {
	                Entity rootUser = Entity.newBuilder(rootKey)
	                		.set("password", DigestUtils.sha1Hex("adminPass123!"))
	                        .set("username", "root")
	                        .set("email", "root@admin.pt")
	                        .set("fullName", "System Administrator")
	                        .set("phoneNumber", "000000000")
	                        .set("privacy", "private")
	                        .set("role", "admin")
	                        .set("state", "ATIVADA")
	                    .build();
	                
	                txn.put(rootUser);
	                txn.commit();
	                LOG.info("Utilizador root/admin criado com sucesso!");
	            }
	        } catch (Exception e) {
	            if (txn.isActive()) txn.rollback();
	            LOG.severe("Erro ao criar root user: " + e.getMessage());
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
