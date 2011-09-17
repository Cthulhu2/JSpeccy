/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import gui.JSpeccyScreen;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import utilities.Snapshots;
import utilities.Tape;
import z80core.Z80;

/**
 *
 * @author jsanchez
 */
public class Spectrum extends Thread implements z80core.MemIoOps {

    private Z80 z80;
    private Memory memory;
    private boolean[] contendedRamPage = new boolean[4];
    private boolean[] contendedIOPage = new boolean[4];
    public int portFE, earBit = 0xbf, port7ffd;
    private long nFrame, framesByInt, speedometer, speed, prevSpeed;
    private boolean soundOn, enabledAY, resetPending;
    private static final byte delayTstates[] =
            new byte[MachineTypes.SPECTRUM128K.tstatesFrame + 100];
    public MachineTypes spectrumModel;
    private Timer timerFrame;
    private SpectrumTimer taskFrame;
    private JSpeccyScreen jscr;
    private Keyboard keyboard;
    private Audio audio;
    private AY8912 ay8912;
    public Tape tape;
    private boolean paused;
    private javax.swing.JLabel modelLabel, speedLabel;
    private JMenuItem hardwareMenu48k, hardwareMenu128k;
    private JMenuItem joystickNone, joystickKempston,
        joystickSinclair1, joystickSinclair2, joystickCursor;
    public static enum Joystick { NONE, KEMPSTON, SINCLAIR1, SINCLAIR2, CURSOR };
    private Joystick joystick;
    private boolean issue3;

    public Spectrum() {
        super("SpectrumThread");
        z80 = new Z80(this);
        memory = new Memory();
        Arrays.fill(contendedRamPage, false);
        Arrays.fill(contendedIOPage, false);
        contendedRamPage[1] = contendedIOPage[1] = true;
        memory.loadRoms();
        initGFX();
        nFrame = speedometer = 0;
        framesByInt = 1;
        portFE = 0;
        port7ffd = 0;
        timerFrame = new Timer("SpectrumClock", true);
        ay8912 = new AY8912();
        audio = new Audio();
        tape = new Tape(z80);
        soundOn = true;
        paused = true;
        select48kHardware();
        resetPending = false;
        joystick = Joystick.NONE;
        keyboard = new Keyboard();
        keyboard.setJoystick(joystick);
    }

    public final void select48kHardware() {
        if (spectrumModel == MachineTypes.SPECTRUM48K) {
            return;
        }

        spectrumModel = MachineTypes.SPECTRUM48K;
        memory.setSpectrumModel(spectrumModel);
        tape.setSpectrumModel(spectrumModel);
        enabledAY = spectrumModel.hasAY8912();
        buildScreenTables48k();
        issue3 = true;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                modelLabel.setToolTipText(spectrumModel.getLongModelName());
                modelLabel.setText(spectrumModel.getShortModelName());
            }
        });
    }

    public final void select128kHardware() {
        if (spectrumModel == MachineTypes.SPECTRUM128K)
            return;

        spectrumModel = MachineTypes.SPECTRUM128K;
        memory.setSpectrumModel(spectrumModel);
        tape.setSpectrumModel(spectrumModel);
        enabledAY = spectrumModel.hasAY8912();
        buildScreenTables128k();
        issue3 = true;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                modelLabel.setToolTipText(spectrumModel.getLongModelName());
                modelLabel.setText(spectrumModel.getShortModelName());
            }
        });
    }

    /*
     * Esto es necesario para conseguir un mejor funcionamiento en Windows.
     * Para los sistemas Unix deber�a ser una modificaci�n inocua. La raz�n del
     * hack est� explicada en:
     * http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks
     * y en
     * http://www.javamex.com/tutorials/threads/sleep.shtml
     * 
     */
    @Override
    public void run() {
        try {
            sleep(Long.MAX_VALUE);
        } catch (InterruptedException excpt) {
            Logger.getLogger(Spectrum.class.getName()).log(Level.SEVERE, null, excpt);
        }
    }

    public void startEmulation() {
        z80.setTEstados(0);
        audio.reset();
        invalidateScreen(true);
        taskFrame = new SpectrumTimer(this);
        timerFrame.scheduleAtFixedRate(taskFrame, 50, 20);
        paused = false;
        if (soundOn) {
            audio.open(spectrumModel, ay8912, enabledAY);
        }
    }

    public void stopEmulation() {
        taskFrame.cancel();
        paused = true;
        if (soundOn) {
            audio.close();
        }
    }

    public void reset() {
        resetPending = true;
    }

    private void doReset() {
        z80.reset();
        memory.reset();
        ay8912.reset();
        audio.reset();
        keyboard.reset();
        contendedRamPage[3] = contendedIOPage[3] = false;
        nFrame = 0;       
        portFE = 0;
        port7ffd = 0;
        invalidateScreen(true);
    }

    public boolean isPaused() {
        return paused;
    }

    public void triggerNMI() {
        z80.triggerNMI();
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public void setJoystick(Joystick type) {
        joystick = type;
        keyboard.setJoystick(type);
    }

    public void setJoystickMenuItems(JMenuItem jNone, JMenuItem jKempston,
        JMenuItem jSinclair1, JMenuItem jSinclair2, JMenuItem jCursor) {
        joystickNone = jNone;
        joystickKempston = jKempston;
        joystickSinclair1 = jSinclair1;
        joystickSinclair2 = jSinclair2;
        joystickCursor = jCursor;
    }

    public void setHardwareMenuItems(JMenuItem hw48k, JMenuItem hw128k) {
        hardwareMenu48k = hw48k;
        hardwareMenu128k = hw128k;
    }

    public void setScreenComponent(JSpeccyScreen jScr) {
        this.jscr = jScr;
    }

    public void setInfoLabels(JLabel nameComponent, JLabel speedComponent) {
        modelLabel = nameComponent;
        speedLabel = speedComponent;
    }

    public void generateFrame() {
//        long startFrame, endFrame, sleepTime;
//        startFrame = System.currentTimeMillis();
//        System.out.println("Start frame: " + startFrame);

        //z80.tEstados = frameStart;
        //System.out.println(String.format("Begin frame. t-states: %d", z80.tEstados));
        if (resetPending) {
            resetPending = false;
            doReset();
        }

        long counter = framesByInt;

        do {
            z80.setINTLine(true);
            z80.execute(spectrumModel.lengthINT);
            z80.setINTLine(false);
            z80.execute(spectrumModel.firstScrByte);
            updateInterval(spectrumModel.firstScrUpdate, z80.tEstados);
            //System.out.println(String.format("t-states: %d", z80.tEstados));
            //int fromTstates;
            // El �ltimo byte de pantalla se muestra en el estado 57236
            while (z80.tEstados < spectrumModel.lastScrUpdate) {
                int fromTstates = z80.tEstados + 1;
                z80.execute(fromTstates + 15);
                updateInterval(fromTstates, z80.tEstados);
            }

            z80.execute(spectrumModel.tstatesFrame);

            if (soundOn) {
                if (enabledAY) {
                    ay8912.updateAY(z80.tEstados);
                    ay8912.endFrame();
                }
                audio.updateAudio(z80.tEstados, speaker);
                audio.endFrame();
            }

            z80.tEstados -= spectrumModel.tstatesFrame;

            if (++nFrame % 16 == 0) {
                toggleFlash();
            }

            if (nFrame % 50 == 0) {
                long now = System.currentTimeMillis();
                speed = 100000 / (now - speedometer);
                speedometer = now;
                if (speed != prevSpeed) {
                    prevSpeed = speed;
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            speedLabel.setText(String.format("%4d%%", speed));
                        }
                    });
//                    System.out.println(String.format("Time: %d Speed: %d%%",now, speed));
                }
            }
        } while (--counter > 0);

        
        if (screenUpdated || nBorderChanges > 0) {
//            System.out.print(System.currentTimeMillis() + " ");
            if (nBorderChanges > 0) {
                if (nBorderChanges == 1) {
                    intArrayFill(imgData, Paleta[portFE & 0x07]);
                    nBorderChanges = 0;
                } else {
                    nBorderChanges = 1;
                }

                jscr.notifyNewFrame(true);
                jscr.repaint();
            } else {
                jscr.notifyNewFrame(false);
                jscr.repaint(BORDER_WIDTH, BORDER_WIDTH, 256, 192);
            }

            if (nBorderChanges == 0) {
                screenUpdated = false;
            }
        }

        //endFrame = System.currentTimeMillis();
        //System.out.println("End frame: " + endFrame);
        //sleepTime = endFrame - startFrame;
        //System.out.println("execFrame en: " + sleepTime);
    }

    public int fetchOpcode(int address) {

        if (contendedRamPage[address >>> 14]) {
//            System.out.println(String.format("getOpcode: %d %d %d",
//                    z80.tEstados, address, delayTstates[z80.tEstados]));
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
        }
        z80.tEstados += 4;
//        z80.addTEstados(4);

//        if ((z80.getRegI() & 0xC0) == 0x40) {
//            m1contended = z80.tEstados;
////            m1contended = z80.getTEstados();
//            m1regR = z80.getRegR();
//        }

        // LD_BYTES routine in Spectrum ROM at address 0x0556
        if (address == 0x0556 && tape.isTapeInserted() && tape.isStopped()) {
            if (!tape.isFastload() || tape.isTzxTape()) {
                tape.play();
            } else {
                tape.fastload(memory);
                invalidateScreen(true); // thanks Andrew Owen
                return 0xC9; // RET opcode
            }
        }
        return memory.readByte(address);
    }

    public int peek8(int address) {

        if (contendedRamPage[address >>> 14]) {
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
        }
        z80.tEstados += 3;
//        z80.addTEstados(3);

        return memory.readByte(address);
    }

    public void poke8(int address, int value) {

        if (contendedRamPage[address >>> 14]) {
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
            if (memory.isScreenByte(address)) {
                screenUpdated(address);
            }
        }
        z80.tEstados += 3;
//        z80.addTEstados(3);

        memory.writeByte(address, value);
    }

    public int peek16(int address) {

        int lsb;
        if (contendedRamPage[address >>> 14]) {
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
        }
        z80.tEstados += 3;
//        z80.addTEstados(3);
        lsb = memory.readByte(address);

        address = (address + 1) & 0xffff;
        if (contendedRamPage[address >>> 14]) {
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
        }
        z80.tEstados += 3;
//        z80.addTEstados(3);

        //msb = z80Ram[address];
        return (memory.readByte(address) << 8) | lsb;
    }

    public void poke16(int address, int word) {
//        poke8(address, word & 0xff);
//        poke8((address + 1) & 0xffff, word >>> 8);

        if (contendedRamPage[address >>> 14]) {
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
            if (memory.isScreenByte(address)) {
                screenUpdated(address);
            }
        }
        z80.tEstados += 3;
//        z80.addTEstados(3);

        memory.writeByte(address, word & 0xff);

        address = (address + 1) & 0xffff;

        if (contendedRamPage[address >>> 14]) {
            z80.tEstados += delayTstates[z80.tEstados];
//            z80.addTEstados(delayTstates[z80.getTEstados()]);
            if (memory.isScreenByte(address)) {
                screenUpdated(address);
            }
        }
        z80.tEstados += 3;
//        z80.addTEstados(3);

        memory.writeByte(address, word >>> 8);
    }

    public void contendedStates(int address, int tstates) {
        if (contendedRamPage[address >>> 14]) {
            for (int idx = 0; idx < tstates; idx++) {
                z80.tEstados += delayTstates[z80.tEstados] + 1;
//                z80.addTEstados(delayTstates[z80.getTEstados()] + 1);
            }
        } else {
            z80.tEstados += tstates;
//            z80.addTEstados(tstates);
        }
    }

    public int inPort(int port) {
//        int res = port >>> 8;

//        if ((port & 0xff) == 0xff) {
//            System.out.println(String.format("inPort -> t-state: %d\tPC: %04x",
//                    z80.tEstados, z80.getRegPC()));
//        }
        //int tstates = z80.tEstados;
        //postIO(port);
//        System.out.println(String.format("InPort: %04X", port));
        preIO(port);
        postIO(port);
//        tape.notifyTstates(nFrame, z80.tEstados);

//        System.out.println(String.format("inPort -> t-state: %d\tPC: %04x",
//                    z80.tEstados, z80.getRegPC()));
        /*
         * El interfaz Kempston solo (deber�a) decodificar A5=0...
         * Las Tres Luces de Glaurung leen el puerto #DF (223) y si se decodifica
         * algo m�s no funciona con el joystick. Si decodificamos solo A5==0
         * es Buggy Boy el que se cuelga.
         */
        if (((port & 0x00e0) == 0 || (port & 0x0020) == 0) &&
                joystick == Joystick.KEMPSTON) {
//            System.out.println(String.format("InPort: %04X, PC: %04X", port, z80.getRegPC()));
            return keyboard.readKempstonPort();
        }

        if ((port & 0x0001) == 0) {
            return keyboard.readKeyboardPort(port) & tape.getEarBit();
        }

        if (enabledAY) {
            if ((port & 0xC002) == 0xC000) {
                return ay8912.readRegister();
            }
        }

//        System.out.println(String.format("InPort: %04X at %d t-states", port, z80.tEstados));
        int floatbus = 0xff;
        int addr = 0;
        int tstates = z80.tEstados;
        if (tstates < spectrumModel.firstScrByte || tstates > spectrumModel.lastScrUpdate) {
            return floatbus;
        }

        int col = (tstates % spectrumModel.tstatesLine) - spectrumModel.outOffset;
        if (col > 124) {
            return floatbus;
        }

        int row = tstates / spectrumModel.tstatesLine - spectrumModel.upBorderWidth;

        switch (col % 8) {
            case 0:
                addr = scrAddr[row] + col / 4;
                floatbus = memory.readScreenByte(addr);
                break;
            case 1:
                addr = scr2attr[(scrAddr[row] + col / 4) & 0x1fff];
                floatbus = memory.readScreenByte(addr);
                break;
            case 2:
                addr = scrAddr[row] + col / 4 + 1;
                floatbus = memory.readScreenByte(addr);
                break;
            case 3:
                addr = scr2attr[(scrAddr[row] + col / 4 + 1) & 0x1fff];
                floatbus = memory.readScreenByte(addr);
                break;
        }

        /*
         * Solo en el modelo 128K, pero no en los +2A/+3, si se lee el puerto
         * 0x7ffd, el valor le�do es reescrito en el puerto 0x7ffd.
         */
        if (spectrumModel == MachineTypes.SPECTRUM128K) {
            if ((port & 0x8002) == 0) {
                memory.setPort7ffd(floatbus);
                // En el 128k las p�ginas impares son contended
                contendedRamPage[3] = contendedIOPage[3] = (floatbus & 0x01) != 0 ? true : false;
                // Si ha cambiado la pantalla visible hay que invalidar
                if ((port7ffd & 0x08) != (floatbus & 0x08)) {
                    invalidateScreen(false);
                }
                port7ffd = floatbus;
            }
        }
//        floatbus = memory.readScreenByte(addr);
//            System.out.println(String.format("tstates = %d, addr = %d, floatbus = %02x",
//                    tstates, addr, floatbus));
        return floatbus;
    }

    public void outPort(int port, int value) {

        preIO(port);

        if ((port & 0x0001) == 0) {
            if ((portFE & 0x07) != (value & 0x07)) {
//                if( (value & 0x07) == 0x07 )
//                    System.out.println(String.format("tstates: %d border: %d",
//                        z80.tEstados, value&0x07));
                updateBorder(z80.tEstados);
            }

            int spkMic = sp_volt[value >> 3 & 3];
            if (soundOn && spkMic != speaker) {
                audio.updateAudio(z80.tEstados, speaker);
                speaker = spkMic;
            }

            if (tape.isStopped()) {
                // and con 0x18 para emular un Issue 2
                // and con 0x10 para emular un Issue 3
                int issueMask = issue3 ? 0x10 : 0x18;
                if ((value & issueMask) == 0) {
                    tape.setEarBit(false);
                } else {
                    tape.setEarBit(true);
                }
            }
            //System.out.println(String.format("outPort: %04X %02x", port, value));         
            portFE = value;
        }

        if (spectrumModel != MachineTypes.SPECTRUM48K) {
            if ((port & 0x8002) == 0) {
//            System.out.println(String.format("outPort: %04X %02x at %d t-states",
//            port, value, z80.tEstados));

                memory.setPort7ffd(value);
                // En el 128k las p�ginas impares son contended
                contendedRamPage[3] = contendedIOPage[3] = (value & 0x01) != 0 ? true : false;
                // Si ha cambiado la pantalla visible hay que invalidar
                if (((port7ffd ^ value) & 0x08) != 0) {
                    invalidateScreen(false);
                }
                port7ffd = value;
            }
        }

        if (enabledAY && (port & 0x8002) == 0x8000) {
            if ((port & 0x4000) != 0) {
                ay8912.setAddressLatch(value);
            } else {
                if (soundOn && ay8912.getAddressLatch() < 14) {
                    ay8912.updateAY(z80.tEstados);
                }
                ay8912.writeRegister(value);
            }
        }
        //preIO(port);
        postIO(port);
//        tape.notifyTstates(nFrame, z80.tEstados);
    }

    /*
     * Las operaciones de I/O se producen entre los estados T3 y T4 de la CPU,
     * y justo ah� es donde podemos encontrar la contenci�n en los accesos. Los
     * ciclos de contenci�n son exactamente iguales a los de la memoria, con los
     * siguientes condicionantes dependiendo del estado del bit A0 y de si el
     * puerto accedido se encuentra entre las direcciones 0x4000-0x7FFF:
     *
     * High byte in 0x40 (0xc0) to 0x7f (0xff)? 	Low bit  Contention pattern
     *                                      No      Reset    N:1, C:3
     *                                      No      Set      N:4
     *                                      Yes     Reset    C:1, C:3
     *                                      Yes 	Set      C:1, C:1, C:1, C:1
     *
     * La columna 'Contention Pattern' se lee 'N:x', no contenci�n x ciclos
     * 'C:n' se lee contenci�n seguido de n ciclos sin contenci�n.
     * As� pues se necesitan dos rutinas, la que a�ade los primeros 3 t-estados
     * con sus contenciones cuando procede y la que a�ade el estado final con
     * la contenci�n correspondiente.
     */
    private void postIO(int port) {
        if ((port & 0x0001) != 0) {
            if (contendedIOPage[port >>> 14]) {
                // A0 == 1 y es contended RAM
                z80.tEstados += delayTstates[z80.tEstados] + 1;
                z80.tEstados += delayTstates[z80.tEstados] + 1;
                z80.tEstados += delayTstates[z80.tEstados] + 1;

            } else {
                // A0 == 1 y no es contended RAM
                z80.tEstados += 3;
            }
        } else {
//            if ((port & 0xc000) == 0x4000) {
//                // A0 == 0 y es contended RAM
//                z80.tEstados += delayTstates[z80.tEstados];
//                z80.tEstados += 3;
//            } else {
            // A0 == 0 y no es contended RAM
            z80.tEstados += delayTstates[z80.tEstados] + 3;
//            }
        }
    }

    private void preIO(int port) {
        if (contendedIOPage[port >>> 14]) {
            // A0 == 1 y es contended RAM
            z80.tEstados += delayTstates[z80.tEstados];
        }
        z80.tEstados++;
//        z80.addTEstados(1);
    }

    public void execDone(int tstates) {
        tape.notifyTstates(nFrame, z80.tEstados);
        if (soundOn && tape.isPlaying()) {
            earBit = tape.getEarBit();
            int spkMic = (earBit == 0xbf) ? -2000 : 2000;
            if (spkMic != speaker) {
                audio.updateAudio(z80.tEstados, speaker);
                speaker = spkMic;
            }
        }
    }

    public void loadSnapshot(File filename) {
        Snapshots snap = new Snapshots();
        if (snap.loadSnapshot(filename, memory)) {
            doReset();

            switch(snap.getSnapshotModel()) {
                case SPECTRUM48K:
                    select48kHardware();
                    hardwareMenu48k.setSelected(true);
                    break;
                case SPECTRUM128K:
                    select128kHardware();
                    hardwareMenu128k.setSelected(true);
                    break;
            }

            z80.setRegI(snap.getRegI());
            z80.setRegHLalt(snap.getRegHLalt());
            z80.setRegDEalt(snap.getRegDEalt());
            z80.setRegBCalt(snap.getRegBCalt());
            z80.setRegAFalt(snap.getRegAFalt());
            z80.setRegHL(snap.getRegHL());
            z80.setRegDE(snap.getRegDE());
            z80.setRegBC(snap.getRegBC());
            z80.setRegIY(snap.getRegIY());
            z80.setRegIX(snap.getRegIX());

            z80.setIFF2(snap.getIFF2());
            z80.setIFF1(snap.getIFF1());

            z80.setRegR(snap.getRegR());
            z80.setRegAF(snap.getRegAF());
            z80.setRegSP(snap.getRegSP());
            z80.setIM(snap.getModeIM());

            int border = snap.getBorder();
            portFE &= 0xf8;
            portFE |= border;

            // Solo los 48k pueden ser Issue2. El resto son todos Issue3.
            if (snap.getSnapshotModel() == MachineTypes.SPECTRUM48K) {
                issue3 = snap.isIssue3();
            }

            z80.setRegPC(snap.getRegPC());
            z80.setTEstados(snap.getTstates());
            joystick = snap.getJoystick();
            keyboard.setJoystick(joystick);
            switch (joystick) {
                case NONE:
                    joystickNone.setSelected(true);
                    break;
                case KEMPSTON:
                    joystickKempston.setSelected(true);
                    break;
                case SINCLAIR1:
                    joystickSinclair1.setSelected(true);
                    break;
                case SINCLAIR2:
                    joystickSinclair2.setSelected(true);
                    break;
                case CURSOR:
                    joystickCursor.setSelected(true);
                    break;
            }

            if (snap.getSnapshotModel() != MachineTypes.SPECTRUM48K) {
                port7ffd = snap.getPort7ffd();
                memory.setPort7ffd(port7ffd);
                contendedRamPage[3] = contendedIOPage[3] =
                    (port7ffd & 0x01) != 0 ? true : false;
            }

            if (snap.getAYEnabled() || snap.getSnapshotModel().hasAY8912()) {
                enabledAY = true;
                for(int reg = 0; reg < 16; reg++) {
                    ay8912.setAddressLatch(reg);
                    ay8912.writeRegister(snap.getPsgReg(reg));
                }
                ay8912.setAddressLatch(snap.getPortFffd());
            }

            System.out.println(ResourceBundle.getBundle("machine/Bundle").getString(
                    "SNAPSHOT_LOADED"));
        } else {
            JOptionPane.showMessageDialog(jscr.getParent(), snap.getErrorString(),
                    ResourceBundle.getBundle("machine/Bundle").getString(
                    "SNAPSHOT_LOAD_ERROR"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveSnapshot(File filename) {
        Snapshots snap = new Snapshots();

        snap.setSnapshotModel(spectrumModel);
        snap.setRegI(z80.getRegI());
        snap.setRegHLalt(z80.getRegHLalt());
        snap.setRegDEalt(z80.getRegDEalt());
        snap.setRegBCalt(z80.getRegBCalt());
        snap.setRegAFalt(z80.getRegAFalt());
        snap.setRegHL(z80.getRegHL());
        snap.setRegDE(z80.getRegDE());
        snap.setRegBC(z80.getRegBC());
        snap.setRegIY(z80.getRegIY());
        snap.setRegIX(z80.getRegIX());

        snap.setIFF2(z80.isIFF2());
        snap.setIFF1(z80.isIFF1());

        snap.setRegR(z80.getRegR());
        snap.setRegAF(z80.getRegAF());
        snap.setRegSP(z80.getRegSP());
        snap.setModeIM(z80.getIM());
        snap.setBorder(portFE & 0x07);

        snap.setRegPC(z80.getRegPC());
        snap.setTstates(z80.getTEstados());
        snap.setJoystick(joystick);
        snap.setIssue3(issue3);

        if (spectrumModel != MachineTypes.SPECTRUM48K) {
            snap.setPort7ffd(port7ffd);
            int ayLatch = ay8912.getAddressLatch();
            snap.setPortfffd(ayLatch);
            for (int reg = 0; reg < 16; reg++) {
                ay8912.setAddressLatch(reg);
                snap.setPsgReg(reg, ay8912.readRegister());
            }
            ay8912.setAddressLatch(ayLatch);
        }

        if (snap.saveSnapshot(filename, memory)) {
            System.out.println(
                    ResourceBundle.getBundle("machine/Bundle").getString("SNAPSHOT_SAVED"));
        } else {
            JOptionPane.showMessageDialog(jscr.getParent(), snap.getErrorString(),
                    ResourceBundle.getBundle("machine/Bundle").getString(
                    "SNAPSHOT_SAVE_ERROR"), JOptionPane.ERROR_MESSAGE);
        }
    }
//    static final int CHANNEL_VOLUME = 26000;
    static final int SPEAKER_VOLUME = 7000;
    private int speaker;
    private static final int sp_volt[];
    static boolean muted = false;
    static int volume = 40; // %

    void mute(boolean v) {
        muted = v;
        setvol();
    }

    int volume(int v) {
        if (v < 0) {
            v = 0;
        } else if (v > 100) {
            v = 100;
        }
        volume = v;
        setvol();
        return v;
    }

    int volumeChg(int chg) {
        return volume(volume + chg);
    }

    public void toggleSound() {
        soundOn = !soundOn;
        if (soundOn) {
            audio.open(spectrumModel, ay8912, enabledAY);
        } else {
            audio.flush();
            audio.close();
        }
    }

    public void toggleSpeed() {
        if (framesByInt == 1) {
            if (soundOn)
                toggleSound();
            framesByInt = 10;
        } else {
            framesByInt = 1;
            toggleSound();
        }
    }

    public void toggleTape() {
        if (tape.isStopped()) {
            tape.play();
        } else {
            tape.stop();
        }
    }

    static {
        sp_volt = new int[4];
        setvol();
    }

    static void setvol() {
        double a = muted ? 0 : volume / 100.;
        a *= a;

//      sp_volt[0] = (int)(-SPEAKER_VOLUME*a);
//		sp_volt[1] = (int)(-SPEAKER_VOLUME*1.06*a);
//		sp_volt[2] = (int)(SPEAKER_VOLUME*a);
//		sp_volt[3] = (int)(SPEAKER_VOLUME*1.06*a);
        sp_volt[0] = (int) -SPEAKER_VOLUME;
        sp_volt[1] = (int) (-SPEAKER_VOLUME * 1.06);
        sp_volt[2] = (int) SPEAKER_VOLUME;
        sp_volt[3] = (int) (SPEAKER_VOLUME * 1.06);
    }

    /* Secci�n gr�fica */
    //Vector con los valores correspondientes a lo colores anteriores
    private static final int[] Paleta = {
        0x000000, /* negro */
        0x0000c0, /* azul */
        0xc00000, /* rojo */
        0xc000c0, /* magenta */
        0x00c000, /* verde */
        0x00c0c0, /* cyan */
        0xc0c000, /* amarillo */
        0xc0c0c0, /* blanco */
        0x000000, /* negro brillante */
        0x0000ff, /* azul brillante */
        0xff0000, /* rojo brillante	*/
        0xff00ff, /* magenta brillante */
        0x00ff00, /* verde brillante */
        0x00ffff, /* cyan brillante */
        0xffff00, /* amarillo brillante */
        0xffffff /* blanco brillante */};
    // Tablas de valores de Paper/Ink. Para cada valor general de atributo,
    // corresponde una entrada en la tabla que hace referencia al color
    // en la paleta. Para los valores superiores a 127, los valores de Paper/Ink
    // ya est�n cambiados, lo que facilita el tratamiento del FLASH.
    private static final int Paper[] = new int[256];
    private static final int Ink[] = new int[256];
    // Tabla de correspondencia entre la direcci�n de pantalla y su atributo
    public final int scr2attr[] = new int[0x1800];
    // Tabla de correspondencia entre cada atributo y el primer byte del car�cter
    // en la pantalla del Spectrum (la contraria de la anterior)
    private final int attr2scr[] = new int[768];
    // Tabla de correspondencia entre la direcci�n de pantalla del Spectrum
    // y la direcci�n que le corresponde en el BufferedImage.
    private final int bufAddr[] = new int[0x1800];
    // Tabla que contiene la direcci�n de pantalla del primer byte de cada
    // car�cter en la columna cero.
    public final int scrAddr[] = new int[192];
    // Tabla que indica si un byte de la pantalla ha sido modificado y habr� que
    // redibujarlo.
    private final boolean dirtyByte[] = new boolean[0x1800];
    // Tabla de traslaci�n entre t-states y la direcci�n de la pantalla del
    // Spectrum que se vuelca en ese t-state o -1 si no le corresponde ninguna.
    private final int states2scr[] = new int[MachineTypes.SPECTRUM128K.tstatesFrame + 100];
    // Tabla de traslaci�n de t-states al pixel correspondiente del borde.
    private final int states2border[] = new int[MachineTypes.SPECTRUM128K.tstatesFrame + 100];
    public static final int BORDER_WIDTH = 32;
    public static final int SCREEN_WIDTH = BORDER_WIDTH + 256 + BORDER_WIDTH;
    public static final int SCREEN_HEIGHT = BORDER_WIDTH + 192 + BORDER_WIDTH;

    static {
        // Inicializaci�n de las tablas de Paper/Ink
        /* Para cada valor de atributo, hay dos tablas, donde cada una
         * ya tiene el color que le corresponde, para no tener que extraerlo
         */
        for (int idx = 0; idx < 256; idx++) {
            int ink = (idx & 0x07) | ((idx & 0x40) != 0 ? 0x08 : 0x00);
            int paper = ((idx >>> 3) & 0x07) | ((idx & 0x40) != 0 ? 0x08 : 0x00);
            if (idx < 128) {
                Ink[idx] = Paleta[ink];
                Paper[idx] = Paleta[paper];
            } else {
                Ink[idx] = Paleta[paper];
                Paper[idx] = Paleta[ink];
            }
        }
    }
    private int flash = 0x7f; // 0x7f == ciclo off, 0xff == ciclo on
    private BufferedImage borderImage; // imagen del borde
    private int imgData[];
    private BufferedImage screenImage; // imagen de la pantalla
    private int imgDataScr[];
    private Graphics2D gcBorderImage, gcTvImage;
    // t-states del �ltimo cambio de border
    private int lastChgBorder;
    // veces que ha cambiado el borde en el �ltimo frame
    public int nBorderChanges;
    public boolean screenUpdated;
    // t-states del ciclo contended por I=0x40-0x7F o -1
    public int m1contended;
    // valor del registro R cuando se produjo el ciclo m1
    public int m1regR;

    private void initGFX() {
        
        borderImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        gcBorderImage = borderImage.createGraphics();
        imgData = ((DataBufferInt) borderImage.getRaster().getDataBuffer()).getBankData()[0];
        screenImage = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
        imgDataScr = ((DataBufferInt) screenImage.getRaster().getDataBuffer()).getBankData()[0];

        lastChgBorder = 0;
        m1contended = -1;
        Arrays.fill(dirtyByte, true);
        screenUpdated = false;
    }

    public BufferedImage getBorderImage() {
        return borderImage;
    }

    public BufferedImage getScreenImage() {
        return screenImage;
    }

    public synchronized void toggleFlash() {
        flash = (flash == 0x7f ? 0xff : 0x7f);
        for (int addrAttr = 0x5800; addrAttr < 0x5b00; addrAttr++) {
            if (memory.readScreenByte(addrAttr) > 0x7f) {
                screenUpdated(addrAttr);
            }
        }
    }

    /*
     * Cada l�nea completa de imagen dura 224 T-Estados, divididos en:
     * 128 T-Estados en los que se dibujan los 256 pixeles de pantalla
     * 24 T-Estados en los que se dibujan los 48 pixeles del borde derecho
     * 48 T-Estados iniciales de H-Sync y blanking
     * 24 T-Estados en los que se dibujan 48 pixeles del borde izquierdo
     *
     * Cada pantalla consta de 312 l�neas divididas en:
     * 16 l�neas en las cuales el haz vuelve a la parte superior de la pantalla
     * 48 l�neas de borde superior
     * 192 l�neas de pantalla
     * 56 l�neas de borde inferior de las cuales se ven solo 48
     */
    private int tStatesToScrPix48k(int tstates) {

        // Si los tstates son < 3584 (16 * 224), no estamos en la zona visible
        if (tstates < (3584 + ((48 - BORDER_WIDTH) * spectrumModel.tstatesLine))) {
            return 0;
        }

        // Se evita la zona no visible inferior
        if (tstates > (256 + BORDER_WIDTH) * spectrumModel.tstatesLine) {
            return imgData.length - 1;
        }

        tstates -= 3584 + ((48 - BORDER_WIDTH) * spectrumModel.tstatesLine);

        int row = tstates / spectrumModel.tstatesLine;
        int col = tstates % spectrumModel.tstatesLine;

        // No se puede dibujar en el borde con precisi�n de pixel.
        int mod = col % 8;
        col -= mod;
        if (mod > 3) {
            col += 4;
        }

//        System.out.println(String.format("t-states: %d\trow: %d\tcol: %d\tmod: %d",
//                tstates+3584, row, col, mod));

        int pix = row * SCREEN_WIDTH;

        if (col < (128 + BORDER_WIDTH / 2)) {
            return pix + col * 2 + BORDER_WIDTH;
        }
        if (col > (199 + (48 - BORDER_WIDTH) / 2)) {
            return pix + (col - (200 + (48 - BORDER_WIDTH) / 2)) * 2 + SCREEN_WIDTH;
        } else {
            return pix + SCREEN_WIDTH;
        }
    }

    private int tStatesToScrPix128k(int tstates) {

        // Si los tstates son < 3420 (15 * 228), no estamos en la zona visible
        if (tstates < (3420 + ((48 - BORDER_WIDTH) * spectrumModel.tstatesLine))) {
            return 0;
        }

        // Se evita la zona no visible inferior
        if (tstates > (256 + BORDER_WIDTH) * spectrumModel.tstatesLine) {
            return imgData.length - 1;
        }

        tstates -= 3420 + ((48 - BORDER_WIDTH) * spectrumModel.tstatesLine);

        int row = tstates / spectrumModel.tstatesLine;
        int col = tstates % spectrumModel.tstatesLine;

        // No se puede dibujar en el borde con precisi�n de pixel.
        int mod = col % 8;
        col -= mod;
        if (mod > 3) {
            col += 4;
        }

//        System.out.println(String.format("t-states: %d\trow: %d\tcol: %d\tmod: %d",
//                tstates+3584, row, col, mod));

        int pix = row * SCREEN_WIDTH;

        if (col < (128 + BORDER_WIDTH / 2)) {
            return pix + col * 2 + BORDER_WIDTH;
        }
        if (col > (199 + (48 - BORDER_WIDTH) / 2)) {
            return pix + (col - (200 + (48 - BORDER_WIDTH) / 2)) * 2 + SCREEN_WIDTH;
        } else {
            return pix + SCREEN_WIDTH;
        }
    }

    public void updateBorder(int tstates) {
        int startPix, endPix, color;

        if (tstates < lastChgBorder) {
            //startPix = tStatesToScrPix(lastChgBorder);
            startPix = states2border[lastChgBorder];
            if (startPix < imgData.length - 1) {
                color = Paleta[portFE & 0x07];
                for (int count = startPix; count < imgData.length - 1; count++) {
                    imgData[count] = color;
                }
            }
            lastChgBorder = 0;
            nBorderChanges++;
        }

        startPix = states2border[lastChgBorder];
        //startPix = tStatesToScrPix(lastChgBorder);
        if (startPix > imgData.length - 1) {
            lastChgBorder = tstates;
            return;
        }

        //endPix = tStatesToScrPix(tstates);
        endPix = states2border[tstates];
        if (endPix > imgData.length - 1) {
            endPix = imgData.length - 1;
        }

        if (startPix < endPix) {
            color = Paleta[portFE & 0x07];
            for (int count = startPix; count < endPix; count++) {
                imgData[count] = color;
            }
        }
        lastChgBorder = tstates;
        nBorderChanges++;
    }

    public void updateInterval(int fromTstates, int toTstates) {
        int fromAddr, addrBuf;
        int paper, ink;
        int scrByte, attr;
        //System.out.println(String.format("from: %d\tto: %d", fromTstates, toTstates));

        while (fromTstates % 4 != 0) {
            fromTstates++;
        }

        while (fromTstates <= toTstates) {
            fromAddr = states2scr[fromTstates];
            if (fromAddr == -1 || !dirtyByte[fromAddr & 0x1fff]) {
                fromTstates += 4;
                continue;
            }

            scrByte = attr = 0;
            // si m1contended != -1 es que hay que emular el efecto snow.
            if (m1contended == -1) {
                scrByte = memory.readScreenByte(fromAddr);
                fromAddr &= 0x1fff;
                attr = memory.readScreenByte(scr2attr[fromAddr]);
            } else {
                int addr;
                int mod = m1contended % 8;
                if (mod == 0 || mod == 1) {
                    addr = (fromAddr & 0xff00) | m1regR;
                    scrByte = memory.readScreenByte(addr);
                    attr = memory.readScreenByte(scr2attr[fromAddr & 0x1fff]);
                    //System.out.println("Snow even");
                }
                if (mod == 2 || mod == 3) {
                    addr = (scr2attr[fromAddr & 0x1fff] & 0xff00) | m1regR;
                    scrByte = memory.readScreenByte(fromAddr);
                    attr = memory.readScreenByte(addr & 0x1fff);
                    //System.out.println("Snow odd");
                }
                fromAddr &= 0x1fff;
                m1contended = -1;
            }

            addrBuf = bufAddr[fromAddr];
            if (attr > 0x7f) {
                attr &= flash;
            }
            ink = Ink[attr];
            paper = Paper[attr];
            for (int mask = 0x80; mask != 0; mask >>= 1) {
                if ((scrByte & mask) != 0) {
                    imgDataScr[addrBuf++] = ink;
                } else {
                    imgDataScr[addrBuf++] = paper;
                }
            }
            dirtyByte[fromAddr] = false;
            screenUpdated = true;
            fromTstates += 4;
        }
    }

    public void screenUpdated(int address) {
        address &= 0x1fff;
        if (address < 6144) {
            dirtyByte[address] = true;
        } else {
            int addr = attr2scr[address & 0x3ff] & 0x1fff;
            // cuando esto lo hace un compilador, se le llama loop-unrolling
            // cuando lo hace un pogramad�, se le llama shapusa :P
            dirtyByte[addr] = true;
            dirtyByte[addr + 256] = true;
            dirtyByte[addr + 512] = true;
            dirtyByte[addr + 768] = true;
            dirtyByte[addr + 1024] = true;
            dirtyByte[addr + 1280] = true;
            dirtyByte[addr + 1536] = true;
            dirtyByte[addr + 1792] = true;
        }
    }

    public void invalidateScreen(boolean invalidateBorder) {
        if(invalidateBorder)
            nBorderChanges = 1;
        screenUpdated = true;
        Arrays.fill(dirtyByte, true);
        //intArrayFill(imgData, Paleta[portFE & 0x07]);
    }

    public void intArrayFill(int[] array, int value) {
        int len = array.length;
        if (len > 0) {
            array[0] = value;
        }

        for (int idx = 1; idx < len; idx += idx) {
            System.arraycopy(array, 0, array, idx, ((len - idx) < idx) ? (len - idx) : idx);
        }
    }

    private void buildScreenTables48k() {
        int row, col, scan;

        //Inicializaci�n de la tabla de direcciones de pantalla
        /* Hay una entrada en la tabla con la direcci�n del primer byte
         * de cada fila de la pantalla.
         */
        for (int linea = 0; linea < 24; linea++) {
            int idx, lsb, msb, addr;
            lsb = ((linea & 0x07) << 5);
            msb = linea & 0x18;
            addr = (msb << 8) + lsb;
            idx = linea << 3;
            for (scan = 0; scan < 8; scan++, addr += 256) {
                scrAddr[scan + idx] = 0x4000 + addr;
            }
        }

        for (int address = 0x4000; address < 0x5800; address++) {
            row = ((address & 0xe0) >>> 5) | ((address & 0x1800) >>> 8);
            col = address & 0x1f;
            scan = (address & 0x700) >>> 8;

            bufAddr[address & 0x1fff] = row * 2048 + scan * 256 + col * 8;
            scr2attr[address & 0x1fff] = 0x5800 + row * 32 + col;
        }

        for (int address = 0x5800; address < 0x5B00; address++) {
            attr2scr[address & 0x3ff] = 0x4000 | ((address & 0x300) << 3) | (address & 0xff);
        }

        Arrays.fill(states2scr, -1);
        for (int tstates = 14336; tstates < 57344; tstates += 4) {
            col = (tstates % 224) / 4;
            if (col > 31) {
                continue;
            }

            scan = tstates / 224 - 64;
            states2scr[tstates - 8] = scrAddr[scan] + col;
        }

        for (int tstates = 0; tstates < states2border.length - 1; tstates++) {
            states2border[tstates] = tStatesToScrPix48k(tstates);
        }

        Arrays.fill(delayTstates, (byte) 0x00);
        for (int idx = 14335; idx < 57343; idx += 224) {
            for (int ndx = 0; ndx < 128; ndx += 8) {
                int frame = idx + ndx;
                delayTstates[frame++] = 6;
                delayTstates[frame++] = 5;
                delayTstates[frame++] = 4;
                delayTstates[frame++] = 3;
                delayTstates[frame++] = 2;
                delayTstates[frame++] = 1;
                delayTstates[frame++] = 0;
                delayTstates[frame++] = 0;
            }
        }
    }

    private void buildScreenTables128k() {
        int row, col, scan;

        //Inicializaci�n de la tabla de direcciones de pantalla
        /* Hay una entrada en la tabla con la direcci�n del primer byte
         * de cada fila de la pantalla.
         */
        for (int linea = 0; linea < 24; linea++) {
            int idx, lsb, msb, addr;
            lsb = ((linea & 0x07) << 5);
            msb = linea & 0x18;
            addr = (msb << 8) + lsb;
            idx = linea << 3;
            for (scan = 0; scan < 8; scan++, addr += 256) {
                scrAddr[scan + idx] = 0x4000 + addr;
            }
        }

        for (int address = 0x4000; address < 0x5800; address++) {
            row = ((address & 0xe0) >>> 5) | ((address & 0x1800) >>> 8);
            col = address & 0x1f;
            scan = (address & 0x700) >>> 8;

            bufAddr[address & 0x1fff] = row * 2048 + scan * 256 + col * 8;
            scr2attr[address & 0x1fff] = 0x5800 + row * 32 + col;
        }

        for (int address = 0x5800; address < 0x5B00; address++) {
            attr2scr[address & 0x3ff] = 0x4000 | ((address & 0x300) << 3) | (address & 0xff);
        }

        Arrays.fill(states2scr, -1);
        for (int tstates = 14364; tstates < 58140; tstates += 4) {
            col = (tstates % 228) / 4;
            if (col > 31) {
                continue;
            }

            scan = tstates / 228 - 63;
            states2scr[tstates - 12] = scrAddr[scan] + col;
        }

        for (int tstates = 0; tstates < states2border.length - 1; tstates++) {
            states2border[tstates] = tStatesToScrPix128k(tstates);
        }

        Arrays.fill(delayTstates, (byte) 0x00);
        for (int idx = 14361; idx < 58040; idx += 228) {
            for (int ndx = 0; ndx < 128; ndx += 8) {
                int frame = idx + ndx;
                delayTstates[frame++] = 6;
                delayTstates[frame++] = 5;
                delayTstates[frame++] = 4;
                delayTstates[frame++] = 3;
                delayTstates[frame++] = 2;
                delayTstates[frame++] = 1;
                delayTstates[frame++] = 0;
                delayTstates[frame++] = 0;
            }
        }
    }
}
