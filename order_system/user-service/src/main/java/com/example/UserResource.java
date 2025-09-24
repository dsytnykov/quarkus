package com.example;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserResource {

    @Inject
    @Channel("users-out")
    Emitter<String> userEmitter;

    @POST
    @RolesAllowed({"USER", "ADMIN"})
    @Transactional
    public Response create(User user, @Context UriInfo uriInfo) {
        user.persist();
        userEmitter.send("""
                {
                    "event": "USER_CREATED",
                    "id": %s
                }
                """.formatted(user.id));
        return Response.created(uriInfo.getAbsolutePathBuilder().path(user.id.toString()).build()).entity(user).build();
    }

    @PermitAll
    @GET
    @Path("/{id}")
    public User get(@PathParam("id") Long id) {
        return User.findById(id);
    }
}
