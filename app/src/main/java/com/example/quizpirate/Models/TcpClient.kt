package com.example.appesp32.Models

import android.util.Log
import com.example.quizpirate.Utils.WaitNotify
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket


class TcpClient
(private val mHost: String, private val mPort: Int, private val mNotify: WaitNotify, ) {
    var isRun = false
        private set


    var mMessageListener : ((message: String) -> Unit)? = null

    private  var mBufferOut: PrintWriter? = null
    private  var mBufferIn: BufferedReader? = null

    fun host() : String {
        return mHost
    }

    fun port() : Int {
        return mPort
    }

    fun sendMessage(message: String) {
        Thread {
            Log.e("TAG", message)
            if (mBufferOut != null) {
                if (!mBufferOut!!.checkError()) {
                    mBufferOut!!.println(message)
                    mBufferOut!!.flush()
                }
            }

        }.start()
    }

    /**
     * Close the connection and release the members
     */
    fun stopClient() {
        Thread {
            isRun = false
            sendMessage("Test")
            mBufferOut?.flush()
            mBufferOut?.close()
        }.start()
    }

    fun run() {
        Thread {
            isRun = true
            try {
                //here you must put your computer's IP address.
                Log.e("TCP Client", "C: Connecting... to Ip: $mHost Port: $mPort")

                //create a socket to make the connection with the server
                val socket = Socket()
                socket.connect(InetSocketAddress(mHost, mPort), CONNECTION_TIMEOUT)

                try {
                    mBufferOut = PrintWriter(socket.getOutputStream())
                    mBufferIn = BufferedReader(InputStreamReader(socket.getInputStream()))
                    mNotify.doNotify()
                    //in this while the client listens for the messages sent by the server
                    var message : String? = null

                    while (isRun) {
                        message = mBufferIn!!.readLine()
                        println(message)
                        if (message != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener?.let { it(message) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TCP", "R: Error", e)
                    socket.close()
                }
            } catch (e: Exception) {
                Log.e("TCP", "S: Error", e)
            }
            stopClient()

            isRun = false
            mNotify.doNotify()
        }.start()
    }

    companion object {
        private const val CONNECTION_TIMEOUT = 1000
    }
}