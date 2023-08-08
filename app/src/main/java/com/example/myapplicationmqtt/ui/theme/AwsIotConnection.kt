package com.example.myapplicationmqtt.ui.theme

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.example.myapplicationmqtt.R
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class AwsIotConnection(private val context: Context) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun configurarConexionAWS() {
        val clientId = "androidmqq11" // Puede ser cualquier identificador único
        val endpoint = "a2axh58ph1yz86-ats.iot.us-east-1.amazonaws.com" // Sustituye esto con tu Endpoint de AWS IoT

        val mqttManager = AWSIotMqttManager(clientId, endpoint)

        // Configurar opciones de conexión
        mqttManager.connect(loadKeyStore(context)) { status, throwable ->
            Log.d("AWS IoT", "Estado de conexión: $status")
            throwable?.printStackTrace()

            // Una vez conectados, suscribirse al tópico
            if (status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected) {
                val topic = "mi/topico"
                Log.d("AWS IoT", "Suscrito al tópico: $topic")
                mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0) { topic, data ->
                    // Manejar el mensaje recibido aquí
                    val message = String(data)
                    Log.d("AWS IoT", "Mensaje recibido - Tópico: $topic, Mensaje: $message")
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadKeyStore(context: Context): KeyStore? {
        try {
            // Cargar el archivo de certificado y la clave privada
            val certificateInputStream = context.resources.openRawResource(R.raw.certificado)
            val privateKeyInputStream = context.resources.openRawResource(R.raw.private_key)

            // Leer el certificado y la clave privada (ajustar según tu formato de clave)
            val certificate = CertificateFactory.getInstance("X.509").generateCertificate(certificateInputStream)
            val privateKey = loadPrivateKey(context)

            // Crear y llenar el KeyStore
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null)
            keyStore.setCertificateEntry("alias", certificate)
            keyStore.setKeyEntry("alias", privateKey, null, arrayOf(certificate))

            return keyStore
        } catch (e: Exception) {
            Log.e("AWS IoT", "Error al cargar el KeyStore", e)
        }
        return null
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadPrivateKey(context: Context): PrivateKey? {
        try {
            // Abrir el archivo de la clave privada
            val inputStream = context.resources.openRawResource(R.raw.private_key)
            val reader = InputStreamReader(inputStream)

            // Leer y decodificar la clave
            val privateKeyPem = reader.readText()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
            val privateKeyBytes = Base64.getDecoder().decode(privateKeyPem)

            // Convertir a una clave privada
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA") // Asegúrate de usar el algoritmo correcto para tu clave
            return keyFactory.generatePrivate(keySpec)
        } catch (e: Exception) {
            Log.e("AWS IoT", "Error al cargar la clave privada", e)
        }
        return null
    }
}