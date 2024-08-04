package eu.gload.tpmtool.logic

import eu.gload.tpmtool.logic.database.Device
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import tpm2_tool_mobile.Tpm2_tool_mobile
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

object Attestation {
    fun getNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        val encoder = Base64.getEncoder().withoutPadding()
        val unixTime = System.currentTimeMillis() / 1000L
        val result = encoder.encodeToString(bytes)
        return result + unixTime
    }

    enum class AttestationResultType {
        OK,
        CHANGED,
        FAILED,
        REPLAY
    }

    class AttestationResult : Serializable {
        var type: AttestationResultType = AttestationResultType.FAILED
        var newAttestationJson: String = ""
        var failReason: String = ""
        var differencesString : String = ""
        var device :Device? = null
    }

    class SerializablePair<F, S> internal constructor(val first: F, val second: S) : Serializable

    internal fun perform(data: String, nonce: String, oldDevice: Device): AttestationResult {
        try {
            val pubKey = Base64.getDecoder().decode(oldDevice.base64Pem)

            val arr = data.split('|')
            if (arr.count() != 3) {
                throw Exception("Invalid data")
            }
            val signature = Base64.getDecoder().decode(arr[0].trim { it <= ' ' })
            val message = Base64.getDecoder().decode(arr[1].trim { it <= ' ' })
            val pcr = Base64.getDecoder().decode(arr[2].trim { it <= ' ' })
            val bNonce = nonce.toByteArray(StandardCharsets.UTF_8)

            val result = Tpm2_tool_mobile.parseAndValidate(pubKey, message, pcr, signature, bNonce)

            val newMap: Map<Int, String> = parseAttestationJSON(result)
            val oldMap: Map<Int, String> = parseAttestationJSON(oldDevice.attestationJson)

            val attestationResult = compare(oldMap, newMap)
            val newAttestationJson = JSONObject(result).toString(2)

            attestationResult.newAttestationJson = newAttestationJson
            attestationResult.device = oldDevice
            return attestationResult
        } catch (ex: Exception) {
            val r = AttestationResult()
            r.type = AttestationResultType.FAILED
            r.failReason = ex.message.toString()
            r.device = oldDevice
            return r
        }
    }


    private fun differencesToString(differences: Map<Int, SerializablePair<String, String>>): String {
        val sb = StringBuilder()
        differences.forEach { (key, value) ->
            sb.append(key).append(": ").append(value.first).append(" --> ").append(value.second)
                .append("\n")
        }
        return sb.toString()
    }

    private fun compare(oldMap: Map<Int, String>, newMap: Map<Int, String>): AttestationResult {
        val differenceMap: HashMap<Int, SerializablePair<String, String>> = HashMap()
        val visitedPCRs: MutableList<Int> = mutableListOf()
        oldMap.forEach { (key, oldValue) ->
            visitedPCRs.add(key)
            val newValue = checkNotNull(newMap.getOrDefault(key, ""))
            if (newValue != oldValue) {
                differenceMap[key] = SerializablePair(oldValue, newValue)
            }
        }

        newMap.forEach { (key) ->
            if (!visitedPCRs.contains(key)) {
                differenceMap[key] = SerializablePair("", newMap[key]!!)
            }
        }

        val result = AttestationResult()
        if (differenceMap.isEmpty()) {
            result.type = AttestationResultType.OK
        } else {
            result.type = AttestationResultType.CHANGED
            result.differencesString = differencesToString(differenceMap.toMap())
        }
        return result
    }


    @Throws(JSONException::class)
    private fun parseAttestationJSON(jsonString: String) : Map<Int, String> {
        val map: HashMap<Int, String> = HashMap()
        if (jsonString.isEmpty()) {
            return map
        }
        val rootObject = JSONObject(jsonString)
        val pcrValues: JSONObject = rootObject
            .getJSONObject("PCRValues")
        val pcrSelection: JSONArray = rootObject
            .getJSONObject("TPMData")
            .getJSONObject("Attested")
            .getJSONObject("Quote")
            .getJSONObject("PcrSelect")
            .getJSONArray("PcrSelections")
            .getJSONObject(0)
            .getJSONArray("PcrSelect")

        (0 until pcrSelection.length()).forEach { i ->
            val selectedPcr: Int = pcrSelection.getInt(i)
            val pcrValue : String = pcrValues.getString(selectedPcr.toString())
            map[selectedPcr] = pcrValue
        }
        return map
    }
}