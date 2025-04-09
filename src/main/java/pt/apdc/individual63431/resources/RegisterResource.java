package pt.apdc.individual63431.resources;

import java.util.logging.Logger;

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

    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(UserData data) {
        if(!data.isDataValid())
            return Response.status(Status.FORBIDDEN).entity("Missing required fields.").build();

        Key userKey = datastore.newKeyFactory().setKind(UserEntity.Kind).newKey(data.username);

        if (datastore.get(userKey) != null)
            return Response.status(Status.FORBIDDEN).entity("User arleady exists.").build();
        
        data.role="enduser";
        data.state="DESATIVADA";
        Entity newUser = UserEntity.toEntity(data, datastore);
        datastore.put(newUser);
        

        return Response.ok().entity("User regisred").build();
    }

}