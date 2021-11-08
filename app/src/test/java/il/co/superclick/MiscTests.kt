package il.co.superclick

import org.json.JSONObject
import org.junit.Test

class MiscTests {

    data class C1(
        val id: Int,
        val value: String
    )

    @Test
    fun distinct() {
        val list1 = listOf(
            C1(0, "a"),
            C1(1, "b"),
            C1(2, "c")
        )
        val list2 = listOf(
            C1(0, "d"),
            C1(1, "e"),
            C1(2, "f")
        )
        val union = list2.union(list1)
        println("union: $union")

        val distinct = union.distinctBy { it.id }
        println("distinct: $distinct")
    }

    @Test
    fun `tranzila response to json`() {
        val res = """
            Response=004&TranzilaTK=bdb374de97b71200000&currency=1&cred_type=1&DclickTK=&supplier=basketapp&expdate=0322&tranmode=V&sum=3.75&ConfirmationCode=0000000&index=4&Responsesource=2&Responsecvv=0&Responseid=0&Tempref=01470001&DBFIsForeign=0&DBFcard=2&cardtype=2&DBFcardtype=2&cardissuer=2&DBFsolek=6&cardaquirer=6&tz_parent=basketapp&a=
        """.trimIndent()

        val jsonObject =
            JSONObject(
                "{${res.replace("(=&|=\$)".toRegex(), "=null&")
                    .replace("=", ":").replace("&", ",")}}"
            )
        println(jsonObject)
    }

}