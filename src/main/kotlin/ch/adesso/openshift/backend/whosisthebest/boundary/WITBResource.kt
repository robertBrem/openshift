package ch.adesso.openshift.backend.whosisthebest.boundary

import ch.adesso.openshift.backend.whosisthebest.service.WITBService
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/whoisthebest")
class WITBResource @Inject constructor(private val helloBean: WITBService) {

    @GET
    fun get() = helloBean.whoIsTheBest()

}