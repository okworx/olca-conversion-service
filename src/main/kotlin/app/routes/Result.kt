package app.routes

import app.Server
import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("result")
class Result {

    private val log = LoggerFactory.getLogger(Result::class.java)

    @GET
    @Path("{name}")
    fun get(@PathParam("name") name: String): Response {
        log.trace("looking for {}", name)
        val file = Server.cache!!.getFile(name)
        log.trace("corresponds to {}", file)

        if (!file.exists()) {
            log.trace("file not found")
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("File $name does not exist")
                    .build()
        }
        return Response.ok(file as Any).type("application/zip")
                .header("Content-Disposition","attachment; " +
                        "filename=\"${file.name}\"").build()
    }
}