package il.co.superclick.data

data class CreditCard(
    val holderName: String,
    val holderId: String,
    val lastDigits: String,
    val expDate: String,
    var token: String
) {

    enum class Status(val code: String, var msg: String) {
        Valid("0", "תקין"),
        Blocked("1", "חסום"),
        Stolen("2", "גנוב"),
        Call("3", "התקשר"),
        Refused("4", "סירוב"),
        Fake("5", "כרטיס מזוייף"),
        Mismatch("6", "ת.ז. או CVV שגויים"),
        Other("", "בסליקת כרטיס האשראי");

        companion object {
            fun find(code: String): Status? = values().find { it.code == code }
        }
    }
}
