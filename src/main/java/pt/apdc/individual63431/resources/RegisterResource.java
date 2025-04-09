package pt.unl.fct.di.apdc.firstwebapp.resources;

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

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
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
        if(!data.isValid())
            return Response.status(Status.FORBIDDEN).entity("Missing required fields.").build();

        Key userKey = datastore.newKeyFactory().setKind(UserEntity.KIND).newKey(data.username);

        if (datastore.get(userKey) != null)
            return Response.status(Status.FORBIDDEN).entity("User arleady exists.").build();

        Entity newUser = UserEntity.toEntity(data, datastore);
        datastore.put(newUser);

        return Response.ok().entity("User regisred").build();
    }

}