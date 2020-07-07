package com.idenc.snapceit

class Person(val name: String) {
    // Holds this person's assigned items
    // Key is the index of the item in the recycler list
    // Value is (price_string, num_people_splitting)
    var itemPrices = HashMap<Int, Pair<String, Int>>()
    var owedPrice = 0.0

    override fun toString(): String {
        return "Person(name='$name', itemPrices=$itemPrices)"
    }

    fun accumulatePrice() {
        var totalCents = 0.0
        for ((_, itemInfo) in itemPrices) {
            val price = itemInfo.first
            val numPeople = itemInfo.second
            val priceString = price.removePrefix("$")
            val idx = priceString.indexOf('.')
            val dollars = priceString.substring(0, idx).toInt()
            var cents = priceString.substring(idx + 1, priceString.length).toFloat()
            cents += dollars * 100
            cents /= numPeople
            totalCents += cents
        }
        owedPrice = totalCents
    }

    fun getPriceString(): String {
        val finalDollars = kotlin.math.floor(owedPrice / 100).toInt()
        val finalCents = kotlin.math.round(owedPrice % 100).toInt()
        return "\$$finalDollars.$finalCents"
    }
}

//fun main() {
//    val test = Person("Iden")
//    val test2 = Person("Jada")
//    test.itemPrices[0] = Pair("$10.35", 2)
//    test2.itemPrices[0] = Pair("$10.35", 2)
//    println(test.accumulatePrice())
//}