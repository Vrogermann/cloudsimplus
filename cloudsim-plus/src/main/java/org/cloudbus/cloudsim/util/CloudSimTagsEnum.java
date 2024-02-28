package org.cloudbus.cloudsim.util;

public enum CloudSimTagsEnum {
    END_OF_SIMULATION(-1),
    BASE(0),
    NET_BASE(100),
    DC_REGISTRATION_REQUEST(BASE.value+ 2),
    DC_LIST_REQUEST(BASE.value+ 4),
    REGISTER_REGIONAL_CIS(BASE.value+ 13),
    REQUEST_REGIONAL_CIS(BASE.value+ 14),
    ICMP_PKT_SUBMIT(NET_BASE.value+ 5),
    ICMP_PKT_RETURN(NET_BASE.value+ 6),
    CLOUDLET_RETURN(BASE.value+ 15),
    CLOUDLET_SUBMIT(BASE.value+ 16),
    CLOUDLET_SUBMIT_ACK(BASE.value+ 17),
    CLOUDLET_CANCEL(BASE.value+ 18),
    CLOUDLET_PAUSE(BASE.value+ 19),
    CLOUDLET_PAUSE_ACK(BASE.value+ 20),
    CLOUDLET_RESUME(BASE.value+ 21),
    CLOUDLET_RESUME_ACK(BASE.value+ 22),
    CLOUDLET_READY(BASE.value+ 23),
    CLOUDLET_FAIL(BASE.value+ 24),
    CLOUDLET_FINISH(-(BASE.value+ 25)),
    CLOUDLET_KILL(BASE.value+ 26),
    CLOUDLET_UPDATE_ATTRIBUTES(BASE.value+ 27),
    VM_CREATE_RETRY(BASE.value+ 31),
    VM_CREATE_ACK(BASE.value+ 32),
    VM_DESTROY(BASE.value+ 33),
    VM_DESTROY_ACK(BASE.value+ 34),
    VM_MIGRATE(BASE.value+ 35),
    VM_MIGRATE_ACK(BASE.value+ 36),
    VM_UPDATE_CLOUDLET_PROCESSING(BASE.value+ 41),
    VM_VERTICAL_SCALING(BASE.value+ 42),
    NETWORK_EVENT_UP(BASE.value+ 43),
    NETWORK_EVENT_SEND(BASE.value+ 44),
    NETWORK_EVENT_DOWN(BASE.value+ 46),
    NETWORK_EVENT_HOST(BASE.value+ 47),
    FAILURE(BASE.value+ 48),
    HOST_FAILURE(FAILURE.value+ 1),
    HOST_ADD(BASE.value+ 60),
    HOST_REMOVE(BASE.value+ 61),
    POWER_MEASUREMENT(BASE.value+ 70),
    HOST_POWER_ON(BASE.value+ 71),
    HOST_POWER_OFF(BASE.value+ 72);

    private final int value;

    CloudSimTagsEnum(int value) {
        this.value = value;
    }

    public static CloudSimTagsEnum findByValue(int value) {
        for (CloudSimTagsEnum tag : CloudSimTagsEnum.values()) {
            if (tag.getValue() == value) {
                return tag;
            }

        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
