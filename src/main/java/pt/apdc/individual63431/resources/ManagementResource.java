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
import pt.apdc.individual63431.util;
import com.google.cloud.datastore.*;
import java.util.logging.Logger;

@Path("/{username}")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ManagementReource(){

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Gson g = new Gson();

    public ManagementResorce() {

    }

    @POST
    @Path("/changeRoles")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserRole()
}
