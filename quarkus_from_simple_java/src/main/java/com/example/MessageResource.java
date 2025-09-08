package com.example;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageResource {

    @POST
    @Transactional
    public Message create(Message m) {
        m.persist();
        return m;
    }

    @GET
    public List<Message> all() {
        return Message.listAll();
    }
}
