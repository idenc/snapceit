package com.idenc.snapceit

class Person(val name: String) {
    // Holds this person's assigned items
    // Key is the index of the item in the recycler list
    // Value is (price_string, num_people_splitting)
    var owedPrice = 0.0

    constructor(name: String, owedPrice: Double) : this(name) {
        this.owedPrice = owedPrice
    }

    override fun toString(): String {
        return "Person(name='$name', owedPrice=$owedPrice)"
    }

    fun getPriceString(): String {
        return String.format("\$%.2f", owedPrice)
    }
}
