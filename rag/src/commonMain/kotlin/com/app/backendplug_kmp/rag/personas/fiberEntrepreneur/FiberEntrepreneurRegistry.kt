package com.app.backendplug_kmp.rag.personas.fiberEntrepreneur

/**
    * Fiber Entrepreneur persona: verified data sources for Module 1 (Market
    * Opportunity Intelligence) of the Kumbaya PRD.

    * Live today: FCC Form 477 broadband deployment data (opendata.fcc.gov,
    * resource whue-6pnt), confirmed working and filterable by census-block
    * prefix (state+county FIPS). Nationwide — works for any 2-digit state
    * FIPS + 3-digit county FIPS combination.
*/
object FiberEntrepreneurRegistry {

    /**
        * FCC Form 477 broadband deployment records for one US county, filtered
        * by census-block prefix. stateFips is 2 digits, countyFips is 3 digits
        * (standard US Census FIPS codes).
    */
    fun broadbandDeploymentUrl(stateFips: String, countyFips: String): String =
        "https://opendata.fcc.gov/resource/whue-6pnt.json" +
                "?\$where=blockcode+like+%27$stateFips$countyFips%25%27" +
                "&\$limit=2000"
}