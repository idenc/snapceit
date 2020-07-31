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
        var total = 0.0
        for ((_, itemInfo) in itemPrices) {
            val price = itemInfo.first
            val numPeople = itemInfo.second
            val priceString = price.removePrefix("$")
            val priceDouble = priceString.toDouble()
            total += (priceDouble / numPeople)
        }
        owedPrice = total
    }

    fun getPriceString(): String {
        return String.format("\$%.2f", owedPrice)
    }
}
