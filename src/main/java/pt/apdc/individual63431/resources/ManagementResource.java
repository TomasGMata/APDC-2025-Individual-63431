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
	import pt.apdc.individual63431.util.RequestManagementChange;
import pt.apdc.individual63431.util.UserEntity;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import java.util.logging.Logger;
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
		        Entity targetUsr = txn.get(targetKey);
		        
		        if(targetUsr==null) {
		        	return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
		        }
		        
		        try {
		        	Entity updatedUser = Entity.newBuilder(targetUsr)
		            		.set("role", rq.getUserChange()).build();
		        	txn.update(updatedUser);
		        	txn.commit();
		        	
		        	LOG.info("Role changed with success");
	        		return Response.ok().entity("User role updated successfully").build();
		        } finally {
		        	if (txn.isActive())
		        		txn.rollback();
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
		        Entity targetUsr = txn.get(targetKey);
		        
		        if(targetUsr==null) {
		        	return Response.status(Status.NOT_FOUND).entity("Target user doesn't exist").build();
		        }
		        
		        try {
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
	    
	    private AuthToken isTokenValid(AuthToken token) {
	    	return null;
	    }
	    
	}
