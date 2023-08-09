package com.example.myapplicationmqtt.ui.theme

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.example.myapplicationmqtt.R
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class AwsIotConnection(private val context: Context) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun configurarConexionAWS() {
        val clientId = "myClient_" + System.currentTimeMillis()
        val serverUri = "ssl://a2axh58ph1yz86-ats.iot.us-east-1.amazonaws.com:8443" // Cambia el puerto según corresponda
        val topic = "esp8266/pub"

        val mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())

        try {
            val keyStore = loadKeyStore(context)
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            val sslContext = SSLContext.getInstance("TLSv1.3")
            sslContext.init(null, trustManagerFactory.trustManagers, null)

            val sslSocketFactory = sslContext.socketFactory

            val mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.socketFactory = sslSocketFactory

            mqttClient.connect(mqttConnectOptions)

            mqttClient.subscribe(topic) { _, message ->
                val receivedMessage = String(message.payload)
                Log.d("AWS IoT", "Mensaje recibido - Tópico: $topic, Mensaje: $receivedMessage")
            }
        } catch (e: Exception) {
            Log.e("AWS IoT", "Error en la configuración de MQTT", e)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadKeyStore(context: Context): KeyStore? {
        try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null)

            // Cargar el certificado CA
            val caInputStream = context.resources.openRawResource(R.raw.ca_cert)
            val caCertificate = CertificateFactory.getInstance("X.509").generateCertificate(caInputStream)
            keyStore.setCertificateEntry("ca", caCertificate)


            // Cargar el certificado del cliente
            val certificateInputStream = context.resources.openRawResource(R.raw.certificado)
            val certificate = CertificateFactory.getInstance("X.509").generateCertificate(certificateInputStream)
            keyStore.setCertificateEntry("alias", certificate)

            // Cargar la clave privada
            val privateKey = loadPrivateKey(context)
            keyStore.setKeyEntry("alias", privateKey, null, arrayOf(certificate))


            Log.d("AWS IoT", "KeyStore cargado correctamente")

            return keyStore

        } catch (e: KeyStoreException) {
            Log.e("AWS IoT", "Error de KeyStore", e)
        } catch (e: CertificateException) {
            Log.e("AWS IoT", "Error de certificado", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e("AWS IoT", "Error de algoritmo", e)
        } catch (e: IOException) {
            Log.e("AWS IoT", "Error de entrada/salida", e)
        } catch (e: UnrecoverableKeyException) {
            Log.e("AWS IoT", "Error de clave no recuperable", e)
        } catch (e: Exception) {
            Log.e("AWS IoT", "Error desconocido", e)
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
    private fun loadCA(): KeyStore? {
        try {
            // Cargar el certificado CA
            val caInputStream = context.resources.openRawResource(R.raw.ca_cert)
            val caCertificate = CertificateFactory.getInstance("X.509").generateCertificate(caInputStream)

            // Crear un KeyStore conteniendo nuestro certificado CA
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", caCertificate)

            return keyStore
        } catch (e: Exception) {
            Log.e("AWS IoT", "Error al cargar el certificado CA", e)
        }
        return null
    }

    private fun createTrustManager(keyStore: KeyStore): TrustManagerFactory {
        val trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm)
        trustManagerFactory.init(keyStore)
        return trustManagerFactory
    }

    private fun createSslContext(trustManagerFactory: TrustManagerFactory): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        return sslContext
    }
}