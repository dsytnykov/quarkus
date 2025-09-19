package com.example;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/users")
@RegisterRestClient(configKey = "user-api")
public interface UserClient {
    @GET
    @Path("/{id}")
    UserDto getUser(@PathParam("id") String id);
}
