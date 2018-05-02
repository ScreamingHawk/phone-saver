package link.standen.michael.phonesaver.util.data

/**
 * A data class for holding a location and whether or not it can be deleted
 */
data class LocationWithData(val location: String, val deletable: Boolean = true)
