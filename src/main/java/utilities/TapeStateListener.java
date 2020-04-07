package utilities;

import utilities.Tape.TapeState;


/**
 * @author jsanchez
 */
public interface TapeStateListener {

    void stateChanged(final TapeState state);
}
