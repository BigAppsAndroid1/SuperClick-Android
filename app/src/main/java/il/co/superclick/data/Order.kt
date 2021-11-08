package il.co.superclick.data

interface Order {
    val shopId: Int
    val userId: Int?
    val products: List<OrderProduct>?
    val type: OrderType?
    val paymentType: PaymentType?
    val time: Time?
    val comment: String?
    val deliveryComment: String?
}