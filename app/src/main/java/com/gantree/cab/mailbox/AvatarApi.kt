package com.gantree.cab.mailbox

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed class AvatarUpload {
  data class Ok(val rev: Int) : AvatarUpload()
  data class Err(val error: String) : AvatarUpload()
}

/** One GET of a room blob. Missing = the room has none; Failed = we do not know. */
private sealed class Fetched {
  class Got(val bytes: ByteArray) : Fetched()
  object Missing : Fetched()
  object Failed : Fetched()
}

class AvatarApi(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build(),
  private val cache: BlobCache? = null,
) {
  /** What [fetch] last kept for this room — paint it before the mailbox answers. */
  fun cached(origin: String, slug: String, path: String = "/api/avatar"): ByteArray? =
    cache?.read(blobCacheKey(origin, slug, path))

  /**
   * Current bytes, or null when the room has none. A failed request (offline,
   * 5xx, expired session) answers with the cached bytes instead of blanking
   * the face; a 404 forgets them.
   */
  suspend fun fetch(
    origin: String,
    slug: String,
    bearer: String,
    rev: Int,
    path: String = "/api/avatar",
  ): ByteArray? {
    val key = blobCacheKey(origin, slug, path)
    val req = Request.Builder().url(blobUrl(origin, path, slug, rev)).get()
    if (bearer.isNotBlank()) {
      req.header("Authorization", "Bearer $bearer")
    }
    val call = client.newCall(req.build())
    return suspendCancellableCoroutine { cont ->
      cont.invokeOnCancellation { call.cancel() }
      call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          if (cont.isActive) cont.resume(cache?.read(key))
        }

        override fun onResponse(call: Call, response: Response) {
          val got = try {
            response.use { res ->
              when {
                res.isSuccessful ->
                  res.body?.bytes()?.takeIf { it.isNotEmpty() }?.let { Fetched.Got(it) } ?: Fetched.Missing
                res.code == 404 -> Fetched.Missing
                else -> Fetched.Failed
              }
            }
          } catch (_: Exception) {
            Fetched.Failed
          }
          val bytes = when (got) {
            is Fetched.Got -> {
              cache?.write(key, got.bytes)
              got.bytes
            }
            Fetched.Missing -> {
              cache?.write(key, null)
              null
            }
            Fetched.Failed -> cache?.read(key)
          }
          if (cont.isActive) cont.resume(bytes)
        }
      })
    }
  }

  fun upload(origin: String, slug: String, bearer: String, jpeg: ByteArray): AvatarUpload {
    val check = acceptJpeg(jpeg)
    if (check is JpegCheck.Err) {
      return AvatarUpload.Err(check.detail)
    }
    val file = jpeg.toRequestBody(JPEG)
    val body = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart("file", "avatar.jpg", file)
      .build()
    val req = Request.Builder().url(avatarUrl(origin, slug)).post(body)
    if (bearer.isNotBlank()) {
      req.header("Authorization", "Bearer $bearer")
    }
    return try {
      client.newCall(req.build()).execute().use { res ->
        val raw = res.body?.string().orEmpty()
        val data = try {
          JSONObject(raw)
        } catch (_: Exception) {
          JSONObject()
        }
        val rev = data.optInt("rev", -1)
        if (!res.isSuccessful || !data.optBoolean("ok", false) || rev <= 0) {
          val err = data.optString("detail").ifBlank { data.optString("error") }.ifBlank { res.message }.ifBlank { "upload failed" }
          return AvatarUpload.Err(err)
        }
        AvatarUpload.Ok(rev)
      }
    } catch (_: Exception) {
      AvatarUpload.Err("upload failed")
    }
  }

  companion object {
    private val JPEG = "image/jpeg".toMediaType()
  }
}
