package com.gantree.cab.mailbox

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthConfig(
  val mode: String?,
  val google: Boolean,
)

data class Me(
  val sub: String,
  val email: String?,
  val cranes: List<String>,
)

data class NativeSession(
  val token: String,
  val sub: String,
  val email: String?,
  val exp: Long,
)

class AuthApi(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build(),
) {
  fun config(origin: String): AuthConfig {
    val body = get(httpOrigin(origin) + "/api/auth/config")
    return AuthConfig(
      mode = body.optStringOrNull("mode"),
      google = body.optBoolean("google", false),
    )
  }

  fun me(origin: String, token: String): Me {
    val body = get(httpOrigin(origin) + "/api/auth/me", token)
    val cranes = body.optJSONArray("cranes")
    val list = buildList {
      if (cranes != null) {
        for (i in 0 until cranes.length()) {
          val s = cranes.optString(i)
          if (s.isNotEmpty()) add(s)
        }
      }
    }
    return Me(
      sub = body.getString("sub"),
      email = body.optStringOrNull("email"),
      cranes = list,
    )
  }

  fun token(origin: String, idToken: String, nonce: String): NativeSession {
    val payload = JSONObject()
      .put("id_token", idToken)
      .put("nonce", nonce)
      .toString()
    val body = post(httpOrigin(origin) + "/api/auth/token", payload)
    return NativeSession(
      token = body.getString("token"),
      sub = body.getString("sub"),
      email = body.optStringOrNull("email"),
      exp = body.optLong("exp", 0L),
    )
  }

  private fun get(url: String, token: String? = null): JSONObject {
    val req = Request.Builder().url(url).get()
    if (!token.isNullOrEmpty()) {
      req.header("Authorization", "Bearer $token")
    }
    return execute(req.build())
  }

  private fun post(url: String, json: String): JSONObject {
    val req = Request.Builder()
      .url(url)
      .post(json.toRequestBody(JSON))
      .build()
    return execute(req)
  }

  private fun execute(req: Request): JSONObject {
    client.newCall(req).execute().use { res ->
      val raw = res.body?.string().orEmpty()
      if (!res.isSuccessful) {
        throw AuthException(res.code, raw)
      }
      return JSONObject(raw)
    }
  }

  companion object {
    private val JSON = "application/json; charset=utf-8".toMediaType()
  }
}

class AuthException(val code: Int, val body: String) : RuntimeException("auth $code")

private fun JSONObject.optStringOrNull(key: String): String? {
  if (!has(key) || isNull(key)) {
    return null
  }
  val v = optString(key)
  return v.takeIf { it.isNotEmpty() }
}
