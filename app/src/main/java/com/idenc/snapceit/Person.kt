package com.idenc.snapceit

class Person(public val name: String) {
    // Holds this person's assigned items
    // Key is the index of the item in the recycler list
    // Value is the price string
    val itemPrices = HashMap<Int, String>()

    override fun toString(): String {
        return "Person(name='$name', itemPrices=$itemPrices)"
    }

//    fun addItem(item: String) {
//        // Given a price in the format $XX.YY add the total number of cents to itemPrices array
//        val priceString = item.removePrefix("$")
//        val idx = priceString.indexOf('.')
//        val dollars = priceString.substring(0, idx).toInt()
//        var cents = priceString.substring(idx + 1, priceString.length).toInt()
//        println(dollars)
//        println(cents)
//        cents += dollars * 100
//        itemPrices.add(cents)
//    }
}

//fun main() {
//    val test = Person("Iden")
//    test.addItem("$10.35")
//}