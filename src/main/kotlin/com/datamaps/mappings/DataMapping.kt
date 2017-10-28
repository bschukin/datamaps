package com.datamaps.mappings

import com.google.gson.annotations.SerializedName

/**
 * Created by Щукин on 28.10.2017.
 *
 * Маппинг
 *
 */
class DataMapping(var name: String) {

    var table: String? = null
    var fields: List<DataField> = mutableListOf();
}

class DataField(var field: String) {

    var group: String? = null;
    var type: FieldType? = null
    var sqlcolumn: String? = null;

    @SerializedName("m-1")
    var manyToOne: ManyToOne?=null;

    @SerializedName("1-m")
    var oneToMany: OneToMany?=null;
}

class ManyToOne(var to: String,
                @SerializedName("join-column") var joinColumn:String) {

}

class OneToMany(var to: String,
                @SerializedName("their-join-column") var theirJoinColumn:String) {

}

enum class FieldType
{
    long,
    string,
    list,
    set,
    map
}