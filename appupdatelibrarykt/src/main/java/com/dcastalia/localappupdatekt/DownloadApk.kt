package com.dcastalia.localappupdatekt

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by piashsarker on 1/16/18, rewritten by Cyb3rKo on 12/21/20.
 */

class DownloadApk(context: Context) : AppCompatActivity() {
    private var bar: ProgressDialog? = null
    private var context = context
    private var downloadUrl = ""
    private val TAG = "DownloadApk"

    fun startDownloadingApk(url: String) {
        downloadUrl = url
        if (URLUtil.isValidUrl(downloadUrl)) {
            DownloadNewVersion().execute()
        }
    }

    private inner class DownloadNewVersion : AsyncTask<String, Int, Boolean>() {
        override fun onPreExecute() {
            super.onPreExecute()
            bar = ProgressDialog(context)
            bar!!.setCancelable(false)
            bar!!.setMessage("Downloading...")
            bar!!.isIndeterminate = true
            bar!!.setCanceledOnTouchOutside(false)
            bar!!.show()
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            if (bar != null) {
                bar!!.isIndeterminate = false
                bar!!.max = 100
                var msg = ""
                val progress = values[0]
                if (progress != null && bar != null) {
                    bar!!.progress = progress
                    msg = if (progress > 99) {
                        "Finishing... "
                    } else {
                        "Downloading... $progress%"
                    }
                }
                bar!!.setMessage(msg)
            }
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if (bar != null && bar?.isShowing == true) {
                bar!!.dismiss()
                bar = null
            }

            if (result != null && result) {
                Toast.makeText(context, "Update Done", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(context, "Error: Try Again", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        override fun doInBackground(vararg p0: String?): Boolean {
            var flag = false

            try {
                val url = URL(downloadUrl)
                val c = url.openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connect()
                val path = Environment.getExternalStorageDirectory().toString() + "/Download/"
                val file = File(path)
                file.mkdirs()
                val outputFile = File(file, "app-debug.apk")

                if (outputFile.exists()) outputFile.delete()

                val fos = FileOutputStream(outputFile)
                val inputStream = c.inputStream
                val totalSize = c.contentLength //size of apk

                val buffer = ByteArray(1024)
                var len1: Int
                var per: Int
                var downloaded = 0
                while (inputStream.read(buffer).also { len1 = it } != -1) {
                    fos.write(buffer, 0, len1)
                    downloaded += len1
                    per = (downloaded * 100 / totalSize)
                    publishProgress(per)
                }
                fos.close()
                inputStream.close()
                openNewVersion(path)
                flag = true
            } catch (e: MalformedURLException) {
                Log.e(TAG, "Update Error: " + e.message)
                flag = false
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return flag
        }
    }

    private fun openNewVersion(location: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            getUriFromFile(location),
            "application/vnd.android.package-archive"
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun getUriFromFile(location: String): Uri {
        return if (Build.VERSION.SDK_INT < 24) {
            Uri.fromFile(File(location + "app-debug.apk"))
        } else {
            FileProvider.getUriForFile(
                context, context.packageName + ".provider",
                File(location + "app-debug.apk")
            )
        }
    }
}