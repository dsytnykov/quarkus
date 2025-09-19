package com.example;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
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
    @RolesAllowed({"user", "admin"})
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

    @GET
    @Path("/{id}")
    public User get(@PathParam("id") Long id) {
        return User.findById(id);
    }
}
