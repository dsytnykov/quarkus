package com.example;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/users")
@RegisterRestClient(configKey = "user-api")
public interface UserClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    UserDto getUser(@PathParam("id") String id);
}
