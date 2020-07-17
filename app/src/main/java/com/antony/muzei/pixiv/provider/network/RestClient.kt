/*
 *     This file is part of PixivforMuzei3.
 *
 *     PixivforMuzei3 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program  is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.antony.muzei.pixiv.provider.network

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import com.antony.muzei.pixiv.BuildConfig
import com.antony.muzei.pixiv.PixivProviderConst.PIXIV_HOST_URL
import com.antony.muzei.pixiv.provider.network.interceptor.NetworkTrafficLogInterceptor
import com.antony.muzei.pixiv.provider.network.interceptor.PixivAuthHeaderInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager

object RestClient {

    private const val HASH_SECRET = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    private const val PIXIV_API_HOST = "https://app-api.pixiv.net"

    private val x509TrustManager: X509TrustManager = object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    private val okHttpClientAuthBuilder = OkHttpClient.Builder()
        .apply {
            addNetworkInterceptor(PixivAuthHeaderInterceptor())
            addInterceptor(CustomClientHeaderInterceptor())
            logOnDebug()
        }

    fun getPixivAjaxRetrofit(): Retrofit = OkHttpClient.Builder()
        .apply {
            addNetworkInterceptor(PixivAuthHeaderInterceptor())
            addInterceptor(CustomClientHeaderInterceptor())
            logOnDebug()
        }
        .let {
            Retrofit.Builder()
                .client(it.build())
                .baseUrl("$PIXIV_HOST_URL/ajax")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
        }

    // Used for acquiring Ranking JSON
    fun getRetrofitRankingInstance(bypass: Boolean): Retrofit {
        val okHttpClientRankingBuilder = OkHttpClient.Builder() // Debug logging interceptor
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val originalHttpUrl = original.url
                val url = originalHttpUrl.newBuilder()
                    .addQueryParameter("format", "json")
                    .build()
                val request =
                    original.newBuilder() // Using the Android User-Agent returns a HTML of the ranking page, instead of the JSON I need
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:59.0) Gecko/20100101 Firefox/59.0"
                        )
                        .header("Referer", PIXIV_HOST_URL)
                        .url(url)
                        .build()
                chain.proceed(request)
            })
            .logOnDebug()
            .connectTimeout(60L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
        if (bypass) {
            okHttpClientRankingBuilder
                .sslSocketFactory(RubySSLSocketFactory(), x509TrustManager)
                .hostnameVerifier(HostnameVerifier { _: String?, _: SSLSession? -> true })
                .dns(RubyHttpDns())
        }
        return Retrofit.Builder()
            .client(okHttpClientRankingBuilder.build())
            .baseUrl(PIXIV_HOST_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    // Used for acquiring auth feed mode JSON
    fun getRetrofitAuthInstance(bypass: Boolean): Retrofit {
        if (bypass) {
            okHttpClientAuthBuilder
                .sslSocketFactory(RubySSLSocketFactory(), x509TrustManager)
                .hostnameVerifier(HostnameVerifier { _: String?, _: SSLSession? -> true })
                .dns(RubyHttpDns())
        }
        return Retrofit.Builder()
            .client(okHttpClientAuthBuilder.build())
            .baseUrl(PIXIV_API_HOST)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    // Downloads images from any source
    fun getRetrofitImageInstance(bypass: Boolean): Retrofit {
        val imageHttpClientBuilder = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header(
                        "User-Agent",
                        "PixivAndroidApp/5.0.155 (Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ")"
                    )
                    .header("Referer", PIXIV_HOST_URL)
                    .build()
                chain.proceed(request)
            })
            .logOnDebug()
        if (bypass) {
            imageHttpClientBuilder
                .sslSocketFactory(RubySSLSocketFactory(), x509TrustManager)
                .hostnameVerifier(HostnameVerifier { _: String?, _: SSLSession? -> true })
                .dns(RubyHttpDns())
        }
        return Retrofit.Builder()
            .client(imageHttpClientBuilder.build())
            .baseUrl("https://i.pximg.net")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    // Used for getting an accessToken from a refresh token or username / password
    @JvmStatic
    fun getRetrofitOauthInstance(bypass: Boolean): Retrofit {
        if (bypass) {
            Log.v("REST", "bypass active")
            okHttpClientAuthBuilder
                .sslSocketFactory(RubySSLSocketFactory(), x509TrustManager)
                .hostnameVerifier(HostnameVerifier { _: String?, _: SSLSession? -> true })
                .dns(RubyHttpDns())
        }
        return Retrofit.Builder()
            .baseUrl("https://oauth.secure.pixiv.net")
            .client(okHttpClientAuthBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    fun getRetrofitBookmarkInstance(bypass: Boolean): Retrofit {
        val okHttpClientBookmarkBuilder = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header(
                        "User-Agent",
                        "PixivAndroidApp/5.0.155 (Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ")"
                    )
                    .build()
                chain.proceed(request)
            })
            .logOnDebug()
        return Retrofit.Builder()
            .baseUrl(PIXIV_API_HOST)
            .client(okHttpClientBookmarkBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private fun getHashSecret(dateSecretConcat: String): String {
        try {
            val digestInstance = MessageDigest.getInstance("MD5")
            val messageDigest = digestInstance.digest(dateSecretConcat.toByteArray())
            val hexString = StringBuilder()
            // this loop is horrifically inefficient on CPU and memory
            // but is only executed once to acquire a new access token
            // i.e. at most once per hour for normal use case
            for (aMessageDigest in messageDigest) {
                val h = StringBuilder(Integer.toHexString(0xFF and aMessageDigest.toInt()))
                while (h.length < 2) {
                    h.insert(0, "0")
                }
                hexString.append(h)
            }
            return hexString.toString()
        } catch (ex: NoSuchAlgorithmException) {
            ex.printStackTrace()
        }
        // TODO replace this place holder
        return ""
    }

    private fun OkHttpClient.Builder.logOnDebug(): OkHttpClient.Builder =
        this.apply {
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(NetworkTrafficLogInterceptor())
            }
        }


    /**
     * Custom app client request-header [Interceptor]
     */
    private class CustomClientHeaderInterceptor : Interceptor {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.getDefault())

        override fun intercept(chain: Interceptor.Chain): Response {
            // Suppressed because I'm supplying a format string, no locale is implied or used
            val rfc3339Date = dateFormat.format(Date())
            val dateSecretConcat = rfc3339Date + HASH_SECRET
            val hashSecret = getHashSecret(dateSecretConcat)
            val original = chain.request()
            val request = original.newBuilder()
                .header(
                    "User-Agent",
                    "PixivAndroidApp/5.0.155 (Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ")"
                )
                .header("App-OS", "Android")
                .header("App-OS-Version", Build.VERSION.RELEASE)
                .header("App-Version", "5.0.166") //.header("Accept-Language", Locale.getDefault().toString())
                .header("X-Client-Time", rfc3339Date)
                .header("X-Client-Hash", hashSecret)
                .build()
            return chain.proceed(request)
        }
    }

    private class CustomAjaxRequestInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36"
                )
        }
    }

}
