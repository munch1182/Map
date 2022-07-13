package com.munch.lib.amap


data class LaLng(
    // 纬度
    var latitude: Double,
    // 经度
    var longitude: Double,
    // 海拔
    var altitude: Double
)

data class Address(
    val code: AddressCode,
    val desc: AddressDesc
)

data class AddressCode(
    //城市编码
    val codeCity: String?,
    //区域编码
    val codeAddress: String?,
)

data class AddressDesc(
    //国家
    val country: String?,
    //省
    val province: String?,
    //市
    val city: String?,
    //城区
    val district: String?,
    //街道
    val street: String?
)

data class Location(val laLng: LaLng?, var address: Address)

fun interface OnLocationUpdateListener {
    fun onLocationUpdate(location: Location?)
}