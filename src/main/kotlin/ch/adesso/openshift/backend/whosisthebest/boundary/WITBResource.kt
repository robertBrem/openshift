package ch.adesso.openshift.backend.whosisthebest.boundary

import ch.adesso.openshift.backend.whosisthebest.service.WITBService
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/whoisthebest")
class WITBResource {

    @Inject
    private lateinit var witbService: WITBService

    @GET
    fun get() = witbService.whoIsTheBest()

}