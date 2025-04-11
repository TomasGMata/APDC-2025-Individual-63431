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
import com.google.cloud.datastore.*;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Gson g = new Gson();

    public LoginResource() {
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)    
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.fine("Login attempt by user: " + data.identifier);
        
        Key userKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(data.identifier);
        Transaction txn = datastore.newTransaction();
        
        try {
        	Entity user = txn.get(userKey);
        	if (user == null) {
        		LOG.warning("User does not exist");
        		return Response.status(Status.FORBIDDEN).entity("User doesn't exist, please get registered").build();
        	}
        	
        	String hashedPass = user.getString("password");
        	
        	if(hashedPass.equals(DigestUtils.sha3_512Hex(data.password))) {
        		AuthToken at = new AuthToken(data.identifier, user.getString("role"));
        		LOG.info("User login was successfull");
        		
        		Key tokenK = datastore.newKeyFactory().setKind("AuthToken").newKey(at.getTokenID());
        		Entity token = Entity.newBuilder(tokenK).set("username", data.identifier)
        				.set("expirationTime", at.getValidTo()).build();
        		datastore.put(token);
        		
        		return Response.ok(g.toJson(at)).build();
        	}
        	else {
        		return Response.status(Status.FORBIDDEN).entity("Incorrect username or password.").build();
        	}
        	
        } catch(Exception e) {
        	txn.rollback();
            LOG.log(Level.SEVERE, "Erro durante o login", e);
        	return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
        	if (txn.isActive()) txn.rollback();
        }
    }

}
