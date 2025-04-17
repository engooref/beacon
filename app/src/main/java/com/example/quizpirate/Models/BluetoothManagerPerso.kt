import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManagerPerso(
    private val bluetoothAdapter: BluetoothAdapter,
    private val device: BluetoothDevice,
    private val onDataReceivedCallback: ((String) -> Unit)
) {

    private val TAG = "BluetoothManager"

    // UUID SPP standard
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // Thread de lecture des données
    private var readThread: ReadThread? = null

    /**
     * Établit la connexion Bluetooth avec le périphérique.
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun connect(): Boolean {
        return try {
            // Créer le socket Bluetooth
            socket = device.createRfcommSocketToServiceRecord(sppUUID)
            // Annuler la découverte peut accélérer la connexion
            bluetoothAdapter.cancelDiscovery()
            socket?.connect()
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            // Démarrer le thread de lecture dès que la connexion est établie
            readThread = ReadThread()
            readThread?.start()

            Log.i(TAG, "Connexion établie avec ${device.name}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Erreur lors de la connexion Bluetooth", e)
            close()
            false
        }
    }

    /**
     * Envoie une chaîne de caractères via le Bluetooth.
     */
    fun write(data: String): Boolean {
        if (outputStream == null) {
            Log.e(TAG, "Impossible d'envoyer les données : stream de sortie nul")
            return false
        }
        return try {
            outputStream?.write(data.toByteArray())
            Log.i(TAG, "Données envoyées: $data")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Erreur lors de l'envoi des données", e)
            false
        }
    }

    /**
     * Ferme le socket et arrête la lecture.
     */
    fun close() {
        try {
            readThread?.interrupt()
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            Log.i(TAG, "Connexion Bluetooth fermée")
        } catch (e: IOException) {
            Log.e(TAG, "Erreur lors de la fermeture de la connexion", e)
        }
    }

    /**
     * Thread interne pour lire les données en continu.
     */
    private inner class ReadThread : Thread() {
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            // Boucle infinie pour écouter les données entrantes
            while (!isInterrupted) {
                try {
                    if (inputStream != null) {
                        // Lire les données depuis le stream
                        bytes = inputStream!!.read(buffer)
                        if (bytes > 0) {
                            val received = String(buffer, 0, bytes)
                            onDataReceivedCallback(received)
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Erreur lors de la lecture des données", e)
                    break
                }
            }
        }
    }


}
