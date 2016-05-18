package cz.tul.dic.engine;

/**
 *
 * @author user
 */
public abstract class AbstractDeviceManager {

    public abstract void prepareDevice(final DeviceType device);

    public abstract void clearMemory();

}
