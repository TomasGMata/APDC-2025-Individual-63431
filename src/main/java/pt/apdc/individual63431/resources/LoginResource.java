package pt.apdc.individual63431.resources;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.apache.commons.codec.digest.DigestUtils;

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
import pt.apdc.individual63431.util.UserData;
import pt.apdc.individual63431.util.LoginData;
import pt.apdc.individual63431.util.UserEntity;
import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

@Path("/welcome")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Gson g = new Gson();

    public LoginResource() {
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)    
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
    	boolean isEmail = data.username.contains("@");
    	
        try {
        	
        	Entity user = getUserEntity(data.username, isEmail);
        	if (user == null) {
        		LOG.warning("User does not exist");
        		return Response.status(Status.FORBIDDEN).entity("User doesn't exist, please get registered").build();
        	}
        	
        	String hashedPass = user.getString("password");
        	
        	if(!hashedPass.equals(DigestUtils.sha1Hex(data.password))) {
        		return Response.status(Status.FORBIDDEN).entity("Incorrect password.").build();
        	}
        	
        	if(user.getString("state").equals("DESATIVADA")) {
        		return Response.status(Status.FORBIDDEN).entity("Account is not active").build();
        	}
        	
        	AuthToken token = new AuthToken(user.getString("username"), user.getString("role"));
    		Key tokenK = datastore.newKeyFactory().setKind("AuthToken").newKey(token.getTokenID());
    		
    		Entity tokenEntity = Entity.newBuilder(tokenK).set("username", token.getUsername())
    				.set("validTo", token.getValid().VALID_TO).set("role", token.getRole())
    				.set("validFrom", token.getValid().VALID_FROM).set("verifier", token.getVerifier()).build();
    		datastore.put(tokenEntity);
    		
    		return Response.ok().entity("{\"token\": " + g.toJson(token)).build();	
        	
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\": \"Ocorreu um erro durante o login: " + e.getMessage() + "\"}")
                .build();
        } 
    }
    
    private Entity getUserEntity (String login, boolean isEmail) {
    	Query<Entity> query;
        
        if (isEmail) {
            query = Query.newEntityQueryBuilder().setKind("User")
                .setFilter(StructuredQuery.PropertyFilter.eq("email", login))
                .build();
            QueryResults<Entity> results = datastore.run(query);
            return results.hasNext() ? results.next() : null;
        } else {
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(login);
            return datastore.get(userKey);
        }
        
    }

}
