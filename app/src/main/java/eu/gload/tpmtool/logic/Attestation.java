package eu.gload.tpmtool.logic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import eu.gload.tpmtool.logic.database.Device;
import tpm2_tool_mobile.Tpm2_tool_mobile;

public class Attestation {
    public static String GetNonce() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        long unixTime = System.currentTimeMillis() / 1000L;
        String result = encoder.encodeToString(bytes);
        return result + unixTime;
    }

    public static String differencesToString(Map<Integer, Attestation.SerializablePair<String, String>> differences) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Attestation.SerializablePair<String, String>> entry : differences.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().first).append(" --> ").append(entry.getValue().second).append("\n");
        }
        return sb.toString();
    }

    public enum AttestationResultType {
        OK,
        CHANGED,
        FAILED
    }
    public static class AttestationResult implements Serializable {
        public AttestationResultType type = AttestationResultType.FAILED;
        public Map<Integer, SerializablePair<String,String>> differences = new HashMap<>();
        public String DeviceName = "N/A";
        public int DeviceID;
        public String pemBase64;
        public String NewAttestationJson;
        public String FailReason = "";
    }

    public static class SerializablePair<F, S> implements Serializable {
        public final F first;
        public final S second;
        SerializablePair(F first, S second){
            this.first = first;
            this.second = second;
        }
    }

    public static AttestationResult Perform(String in, String pemPublicKey, String nonce, Device oldDevice) {
        try{
            byte[] pubKey = Base64.getDecoder().decode(pemPublicKey);

            String[] arr = in.split("\\|");
            byte[] signature = Base64.getDecoder().decode(arr[0].trim());
            byte[] message = Base64.getDecoder().decode(arr[1].trim());
            byte[] pcr = Base64.getDecoder().decode(arr[2].trim());
            byte[] bNonce = nonce.getBytes(StandardCharsets.UTF_8);

            final String result = Tpm2_tool_mobile.parseAndValidate(pubKey, message, pcr, signature, bNonce);

            Map<Integer, String> newMap = parseAttestationJson(result);
            Map<Integer, String> oldMap = parseAttestationJson(oldDevice.getAttestationJson());

            AttestationResult attestationResult = compare(oldMap, newMap);
            attestationResult.DeviceName = oldDevice.getName();
            attestationResult.DeviceID = oldDevice.getId();
            attestationResult.pemBase64 = oldDevice.getBase64Pem();
            attestationResult.NewAttestationJson = new JSONObject(result).toString(2);
            return attestationResult;
        } catch (Exception ex) {
            AttestationResult r = new AttestationResult();
            r.type = AttestationResultType.FAILED;
            r.FailReason = ex.getMessage();
            r.DeviceName = oldDevice.getName();
            r.DeviceID = oldDevice.getId();
            r.pemBase64 = oldDevice.getBase64Pem();
            return r;
        }
    }


    private static Map<Integer, String> parseAttestationJson(String jsonString) throws JSONException {
        if (jsonString.isEmpty()) {
            return new HashMap<>();
        }
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject pcrValues = jsonObject.
                getJSONObject("PCRValues");
        JSONArray pcrSelection = jsonObject.
                getJSONObject("TPMData").
                getJSONObject("Attested").
                getJSONObject("Quote").
                getJSONObject("PcrSelect").
                getJSONArray("PcrSelections").
                getJSONObject(0).
                getJSONArray("PcrSelect");
        Map<Integer, String> map = new HashMap<>();

        for (int i = 0; i < pcrSelection.length(); i++) {
            int selectedPcr = pcrSelection.getInt(i);
            String pcrValue = pcrValues.getString(String.valueOf(selectedPcr));
            map.put(selectedPcr, pcrValue);
        }
        return map;
    }
    private static AttestationResult compare(Map<Integer, String> oldMap, Map<Integer, String> newMap) {
        Map<Integer, SerializablePair<String,String>> differenceMap = new HashMap<>();
        ArrayList<Integer> VisitedPCRs = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : oldMap.entrySet()) {
            String oldValue = entry.getValue();
            VisitedPCRs.add(entry.getKey());
            String newValue = newMap.getOrDefault(entry.getKey(), "");
            assert newValue != null;
            if (!newValue.equals(oldValue)) {
                differenceMap.put(entry.getKey(),new SerializablePair<>(oldValue,newValue));
            }
        }

        for (Map.Entry<Integer, String> entry : newMap.entrySet()) {
            if (!VisitedPCRs.contains(entry.getKey())) {
                differenceMap.put(entry.getKey(),new SerializablePair<>("",newMap.get(entry.getKey())));
            }
        }


            AttestationResult result = new AttestationResult();
        if (differenceMap.isEmpty()) {
            result.type = AttestationResultType.OK;
        } else {
            result.type = AttestationResultType.CHANGED;
            result.differences = differenceMap;
        }
        return result;
    }

}
