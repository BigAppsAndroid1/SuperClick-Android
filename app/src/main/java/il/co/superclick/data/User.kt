package il.co.superclick.data

data class User(
    val id: Int,
    val authToken: String,
    val name: String,
    val phone: String,
    val email: String,
    val shopId: Int,
    val streetName: String,
    val streetNumber: String,
    val entranceCode: String?,
    val floorNumber: String?,
    val apartmentNumber: Int?,
    val city: String,
    val preferredOrderType: String? = null,
    val orderComment: String? = null,
    val deliveryComment: String? = null,
    val hasReadPrivacyPolicy: Boolean = false,
    val creditCard: CreditCard? = null
) {
    companion object {

        @Suppress("FunctionName")
        fun Empty(): User =
            User(
                id = -1,
                authToken = "",
                name = "",
                phone = "",
                email = "",
                shopId = -1,
                streetName = "",
                streetNumber = "",
                entranceCode = null,
                floorNumber = null,
                apartmentNumber = null,
                city = ""
            )
    }

    fun update(block: User.() -> User) {
        Database.user = block(this)
    }

    fun getFullAddress(): String{
        return "$streetNumber $streetName, $city"
    }

}