package com.antony.muzei.pixiv.repo

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Created by alvince on 2020/7/15
 *
 * @author alvince.zy@gmail.com
 */
interface PixivIllustApi {

    /**
     * add illust to bookmark
     *
     * params:
     * - illust_id
     * - restrict: 0
     * - comment
     * - tags: array
     */
    @POST("/ajax/illusts/bookmarks/add")
    @Headers("accept: application/json")
    fun addFavors(@Body params: Map<*, *>)

}
