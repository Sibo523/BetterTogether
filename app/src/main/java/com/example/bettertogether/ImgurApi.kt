package com.example.bettertogether

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ImgurApi {
    @POST("3/image")
    fun uploadImage(
        @Header("Authorization") auth: String,
        @Body image: Map<String, String>
    ): Call<ImgurResponse>
}