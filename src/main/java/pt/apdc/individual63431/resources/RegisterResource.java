package pt.apdc.individual63431.resources;

import java.util.logging.Logger;

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
import pt.apdc.individual63431.util.LoginData;
import pt.apdc.individual63431.util.UserData;
import pt.apdc.individual63431.util.UserEntity;
import com.google.gson.Gson;
import com.google.cloud.datastore.*;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Gson g = new Gson();

    public RegisterResource() {
    	initRootUser();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(UserData data) {
    	Transaction txn = datastore.newTransaction();
    	
    	LOG.fine("Attempt to register User: " + data.username);
        if(!data.isDataValid()) {
            return Response.status(Status.FORBIDDEN).entity("Missing required fields.").build();
        }

        Key userKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(data.username);

        if (datastore.get(userKey) != null) {
        	txn.rollback();
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
        txn.put(newUser);
        txn.commit();

        return Response.ok().entity("User regisred").build();
    }
    
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
            rootUser.role = "ADMIN";
            rootUser.state = "ATIVADA";
            
            Entity root = Entity.newBuilder(rootKey)
            		.set("password", DigestUtils.sha256Hex(rootUser.password))
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
    		return;
    	} finally {
    		if(txn.isActive()) txn.rollback();
    	}
        
    }

}