package eu.gload.tpmtool.logic;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.Base64;
import java.util.List;

import eu.gload.tpmtool.App;
import eu.gload.tpmtool.logic.database.AppDatabase;
import eu.gload.tpmtool.logic.database.Device;

public class ThreadRunner {

    private static ThreadRunner INSTANCE;
    public static synchronized ThreadRunner getInstance(Callback callback) {
        if (INSTANCE == null) {
            INSTANCE = new ThreadRunner();
            INSTANCE.startWorkThread();
        }
        if (callback == null) { return INSTANCE;}
        INSTANCE.callback = callback;
        return INSTANCE;
    }

    private Callback callback;
    private Handler WorkThreadHandler;

    private void startWorkThread() {
        HandlerThread thread = new HandlerThread("WorkThread");
        thread.start();
        WorkThreadHandler = new Handler(thread.getLooper());
    }


    private void runOnMainThread(Runnable r) {
        Looper l = App.getMContext().getMainLooper();
        Handler h = new Handler(l);
        h.post(r);
    }


    public static class CustomError{
        CustomError(String errorMsg){
            this.ErrorMessage = errorMsg;
        }
        private final String ErrorMessage;

        public String getErrorMessage() {
            return ErrorMessage;
        }
    }


    public void requestDeviceList() {
        final Runnable r  = () -> {
            List<Device> list = AppDatabase.Companion.getInstance(App.getMContext()).devicesDao().getDeviceList();
            runOnMainThread(() -> callback.DeviceListReady(list));
        };
        WorkThreadHandler.post(r);

    }

    public void DeleteDevice(Device device) {
        final Runnable r  = () -> {
            AppDatabase.Companion.getInstance(App.getMContext()).devicesDao().deleteDevice(device);
            runOnMainThread(() -> callback.DeviceDeleted());
        };
        WorkThreadHandler.post(r);
    }


    public void PerformAttestation(Device device, String input, String nonce) {
        final Runnable r  = () -> {
            Attestation.AttestationResult result = Attestation.Perform(input,device.getBase64Pem(),nonce,device);
            runOnMainThread(() -> callback.AttestationResultReady(result));
        };
        WorkThreadHandler.post(r);
    }

    public void AddDevice(String name, String pubKey) {
        final Runnable r  = () -> {

            if (name.isEmpty() || pubKey.isEmpty()) {
                runOnMainThread(() -> callback.DeviceAdded(new CustomError("Some values not filled in")));
                return;
            }
            try {
                if (!Base64.getEncoder().encodeToString(Base64.getDecoder().decode(pubKey)).equals(pubKey)) {
                    throw new Exception();
                }
            } catch (Exception ignored) {
                runOnMainThread(() -> callback.DeviceAdded(new CustomError("Invalid public key value")));
                return;
            }

            Device device = new Device();
            device.setName(name);
            device.setBase64Pem(pubKey);
            device.setAttestationJson("");
            AppDatabase.Companion.getInstance(App.getMContext()).devicesDao().insertDevice(device);
            runOnMainThread(() -> callback.DeviceAdded(null));
        };
        WorkThreadHandler.post(r);
    }

    public void AcceptChanges(Attestation.AttestationResult result) {
        final Runnable r  = () -> {
            Device newDevice = new Device();
            newDevice.setId(result.DeviceID);

            newDevice.setName(result.DeviceName);
            newDevice.setBase64Pem(result.pemBase64);
            newDevice.setAttestationJson(result.NewAttestationJson);
            AppDatabase.Companion.getInstance(App.getMContext()).devicesDao().updateDevice(newDevice);
            runOnMainThread(() -> callback.ChangesAccepted());
        };
        WorkThreadHandler.post(r);
    }
}

