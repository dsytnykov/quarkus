package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OrderResource {

    @Inject
    @RestClient
    UserClient userClient;

    @Inject
    @Channel("orders-out")
    Emitter<String> emitter;

    @POST
    @Transactional
    public Response create(Order order) {

        var user = userClient.getUser(order.userId);
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User not found").build();
        }
        String msg = """
                {
                    "event":"ORDER_CREATED",
                    "orderId":"%s",
                    "userId":"%s"
                }
                """.formatted(order.orderId, order.userId);
        emitter.send(msg);
        return Response.status(Response.Status.CREATED).entity(order).build();
    }
}
