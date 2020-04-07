package joystickinput;

/**
 * @author jsanchez
 */
public interface JoystickRawListener {

    void buttonEvent(int joystickId, int buttonId, boolean state);

    void axisEvent(int joystickId, int axisId, short value);
}
