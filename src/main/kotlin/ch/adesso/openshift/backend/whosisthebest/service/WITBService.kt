package ch.adesso.openshift.backend.whosisthebest.service

import javax.ejb.Stateless

@Stateless
open class WITBService {

    fun whoIsTheBest() = "Thomas"

}