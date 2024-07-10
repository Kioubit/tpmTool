package eu.gload.tpmtool.logic;

import java.util.List;

import eu.gload.tpmtool.logic.database.Device;

public interface Callback {
    void DeviceListReady(List<Device> deviceList);
    void DeviceDeleted();
    void DeviceAdded(ThreadRunner.CustomError error);
    void ChangesAccepted();
    void AttestationResultReady(Attestation.AttestationResult result);
}
