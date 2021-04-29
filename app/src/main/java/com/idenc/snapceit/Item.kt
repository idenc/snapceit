package com.idenc.snapceit

data class Item(
    var itemName: String = "",
    var itemPrice: String = "",
    var peopleSplitting: ArrayList<Person> = ArrayList()
)