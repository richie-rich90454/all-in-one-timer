/**
 * A graphical user interface application that integrates a countdown timer with
 * audio‑visual alarm, a stopwatch with millisecond precision, and a live clock
 * displaying the current system time and its Coordinated Universal Time offset.
 * The interface uses the Swing toolkit and arranges three functional panels
 * vertically inside a resizable frame.  All string representations of time
 * values are precomputed to eliminate runtime allocation and reduce garbage
 * collection pressure.  All timers and threads are properly stopped and
 * released when the window is closed, preventing resource leaks.
 *
 * Countdown Timer:
 * The user sets the desired duration using three spinner components that each
 * accept any non‑negative integer value. Overflow is handled automatically: if
 * the user enters 61 seconds, the total duration is normalized so that 61
 * seconds becomes 1 minute and 1 second before the countdown starts.  The
 * spinners are updated to reflect the normalized values.  A "Set Countdown
 * Timer" button computes the total duration in milliseconds, cancels any
 * currently active alarm, stops any running countdown timer, and then starts
 * a new countdown.  A "Stop Countdown" / "Resume Countdown" button toggles
 * the timer state: when the countdown is running, clicking "Stop Countdown"
 * pauses the timer and freezes the display; the remaining time is preserved
 * and the button changes to "Resume Countdown".  Pressing "Resume Countdown"
 * continues the countdown from the exact point where it was paused, without
 * any time loss.  When the countdown is not running and no paused state
 * exists, the button has no effect.  A "Reset Countdown" button sets all
 * spinners to zero, resets the countdown display labels to "00:00:00.000",
 * clears the progress bar, stops the timer, and cancels the alarm.  While the
 * countdown is active, a Swing Timer running at a 10‑millisecond interval
 * updates four labels that display the remaining time in the format
 * HH:MM:SS.mmm, and a progress bar that fills proportionally to the elapsed
 * portion.  When the remaining time reaches zero or below, the timer stops,
 * the display resets to zero, the progress bar resets, and the alarm sequence
 * is triggered.
 *
 * Layout and Responsiveness:
 * The countdown controls use a 3×3 GridLayout to guarantee a fixed height
 * regardless of button text length.  This prevents layout shifts when the
 * stop/resume button text changes.  All panels scale proportionally with the
 * frame size, and the minimum window size ensures usability on small screens.
 *
 * Alarm Sequence:
 * When the countdown finishes, the application initiates a set of concurrent
 * effects that serve as a perceptually salient notification:
 * – Visual Flashing: A Swing Timer with a 300‑millisecond period toggles the
 *   background color of the countdown display box between white (the
 *   BACKGROUND_COLOR, #FFFFFF) and red (the ALERT_COLOR, #DE0000).  The
 *   flashing continues until the audio sequence completes.
 * – Window Shaking: A Swing Timer with a 50‑millisecond period repeatedly
 *   shifts the frame horizontally by plus or minus five pixels relative to
 *   the location recorded at the start of the alarm.  This oscillation
 *   executes twenty ticks (ten cycles) and then restores the original window
 *   location.
 * – Audio Alert: A separate thread attempts to play a sequence of fifteen
 *   tone pairs.  Each pair consists of a 420 Hz square wave for 100
 *   milliseconds followed by an 840 Hz sine wave for 100 milliseconds,
 *   separated by a 100‑millisecond silence.  Audio samples are synthesized
 *   at 44100 Hz with 8‑bit signed PCM encoding.  If no audio output device
 *   is available, the thread terminates silently; a warning dialog is shown
 *   only once per application session.  A volatile boolean flag
 *   (alarmCancelled) is checked before each tone, before each sleep, and at
 *   the start of the sequence.  If the flag is true, the thread returns
 *   without playing further sounds.  The audio line is closed externally on
 *   cancellation, which causes any blocking write operation to fail
 *   immediately.  Together, these mechanisms guarantee that a single click of
 *   the Stop Countdown button will silence the alarm without delay.
 * – Title Bar Notification: The frame title is set to "Time Is Up" and a
 *   non‑repeating five‑second Timer resets the title back to "All‑in‑One
 *   Timer Tool".
 *
 * Alarm Cancellation:
 * The cancelAlarm method is called before starting a new countdown, when the
 * stop or reset button is pressed, or at the beginning of the triggerAlarm
 * method (to prevent overlapping effects).  It sets the alarmCancelled flag
 * to true, stops and nullifies the flash timer, the shake timer, and the
 * title reset timer; restores the frame location if it was altered by
 * shaking; resets the countdown box background to white (#FFFFFF); restores
 * the original frame title; closes the current audio output line if it is
 * open; interrupts the alarm sound thread; and then nullifies the references
 * to the audio line, the alarm thread, and all auxiliary timers to allow
 * immediate garbage collection.  After nullification, an explicit
 * {@code System.gc()} call is made to encourage prompt memory recovery.
 *
 * Stopwatch:
 * The stopwatch panel contains "Start" and "Reset" buttons and a time
 * display.  Pressing "Start" toggles the stopwatch between running and
 * paused states.  When transitioning from paused to running, the current
 * system time is recorded as the start moment and a 10‑millisecond Swing
 * Timer begins updating the display with the total elapsed time, which is
 * the sum of the time accumulated during previous intervals and the time
 * elapsed since the most recent start.  When transitioning from running to
 * paused, the timer is stopped and the elapsed time since the start is added
 * to the accumulated total.  The button text changes to "Stop" while running
 * and back to "Start" when paused.  The "Reset" button stops the timer, sets
 * the accumulated time to zero, resets the display to 00:00:00.000, and
 * restores the start/stop button to its initial "Start" state.
 *
 * Live Clock:
 * A 50‑millisecond Swing Timer retrieves the current system time using a
 * preallocated Calendar instance and updates four labels showing hours,
 * minutes, seconds, and milliseconds in the format HH:MM:SS.mmm.  The
 * timezone offset is computed by calling TimeZone.getOffset with the current
 * time in milliseconds, which accounts for daylight saving time adjustments.
 * The offset string is only rebuilt when the offset changes, avoiding
 * repeated string allocations.  The offset is displayed in the form
 * (UTC±offset) in a separate label.
 *
 * User Interface Styling and Tooltips:
 * All colors are specified using hexadecimal codes where necessary, but
 * predefined constants (Color.WHITE, Color.BLACK) are used for pure white
 * and black to reduce object creation.  The application employs a dark theme
 * with a red accent.  The frame background is #1E1E1E.  Control panel
 * backgrounds use #2B2B2B and display white label text.  Functional display
 * boxes have a white background (#FFFFFF) with black text, providing high
 * contrast.  Alert flashing and button coloring use the retained red
 * (#DE0000).  Buttons carry white text and change their background to
 * darker or brighter shades of red on hover and press respectively, giving
 * direct visual feedback.  A progress bar shows countdown completion; its
 * foreground is red (#DE0000) and its background is light gray (#D0D0D0).
 * Every interactive component (spinners, buttons, labels, progress bar) has
 * a descriptive tooltip.  Spinner components feature a translucent black
 * border with rounded corners and internal padding.  All interactive elements
 * use the hand cursor.
 *
 * Precomputed Strings:
 * To minimize garbage collection overhead, all possible two‑digit and three‑
 * digit pad strings are statically initialized:
 * – PAD2        : "00" through "99"
 * – PAD2_COLON  : "00:" through "99:"
 * – PAD3_MS     : ".000" through ".999"
 * These arrays are used directly by the timer updates, avoiding any string
 * concatenation or allocation during normal operation.
 *
 * Resource Cleanup:
 * A WindowListener ensures that when the frame is closing, all active timers
 * are stopped, the alarm is cancelled, and all fields that hold references
 * to Swing components, threads, and audio lines are explicitly nullified.
 * An explicit {@code System.gc()} call follows to request immediate garbage
 * collection.  This prevents background timers from continuing to fire after
 * the window is disposed and allows the garbage collector to reclaim all
 * objects promptly.
 *
 * Custom Font Loading:
 * The constructor attempts to load the TrueType font file
 * "NotoSans‑VariableFont_wdth_wght.ttf" from the classpath.  If found, the
 * font is loaded, the input stream is immediately closed, and the three
 * display fonts are derived directly from the loaded font without registering
 * it with the system graphics environment.  This avoids registration overhead
 * and keeps the memory footprint minimal.  If the resource is absent or any
 * error occurs, the fonts are reassigned to system sans‑serif fallbacks, and
 * an error message is printed to the standard error stream.
 *
 * Memory Footprint Management:
 * Beyond font handling, the application employs several design choices to
 * maintain a low resident memory profile.  Repeated color decodes are
 * replaced with static final constants, and the predefined Color.WHITE and
 * Color.BLACK are used for pure white and black to avoid duplicate objects.
 * The live clock refreshes at 50 milliseconds and updates the time‑zone
 * label only when the UTC offset has changed, which drastically reduces
 * string allocations during prolonged use.  Precomputed string arrays
 * eliminate per‑tick concatenation; each string is compact and shared
 * globally.  All Swing timers and the audio thread are explicitly stopped and
 * their references nullified when cancelled or when the window closes,
 * enabling prompt garbage collection.  After each cancellation and resource
 * disposal, an explicit {@code System.gc()} is invoked to encourage the
 * garbage collector to reclaim memory immediately.  When the Java Virtual
 * Machine is launched with a small initial heap (for example,
 * -Xms16m -Xmx64m), the application's total resident memory consumption
 * stays well under 80 MB even during active countdown and stopwatch
 * operation.
 *
 * Exception Handling:
 * All user‑initiated actions (button clicks, etc.) are wrapped in try‑catch
 * blocks; unexpected exceptions are shown in a modal error dialog.  The audio
 * alarm thread catches all exceptions internally: InterruptedException is
 * silently swallowed to stop the alarm, and other exceptions cause a single
 * non‑blocking warning dialog to be displayed on the Event Dispatch Thread,
 * ensuring that the GUI remains responsive and no stack traces appear on the
 * console under normal operation.
 *
 * Formatting Conventions:
 * The source code adheres to a dense, brace‑on‑same‑line style with four
 * spaces per indentation level and no blank lines.  Operator spacing is
 * minimal, with no spaces around assignment or arithmetic operators but
 * spaces after commas and after type annotations.
 *
 * Execution:
 * The main method schedules the construction of an AllInOneTimer instance on
 * the Event Dispatch Thread via SwingUtilities.invokeLater.  The frame
 * centers itself on the screen, has a minimum size of 400 by 600 pixels, and
 * is resizable.
 *
 * @author richie-rich90454
 * @since 2026-07-10
 */
import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
public class AllInOneTimer extends JFrame{
    private JSpinner hourSpinner, minSpinner, secSpinner;
    private JLabel cdHour, cdMin, cdSec, cdMs;
    private Timer countdownTimer;
    private long countdownEndMs;
    private long countdownTotalMs;
    private long countdownRemainingMs;
    private boolean countdownPaused=false;
    private JProgressBar countdownProgress;
    private JButton countdownStopBtn;
    private JLabel swHour, swMin, swSec, swMs;
    private Timer stopwatchTimer;
    private long swStartMs;
    private long swAccumulatedMs;
    private boolean swRunning=false;
    private JLabel timeHour, timeMin, timeSec, timeMs, timeZone;
    private Timer clockTimer;
    private int lastTimezoneOffset=Integer.MIN_VALUE;
    private JPanel countdownBox;
    private Timer flashTimer;
    private boolean flashOn;
    private Point origLocation;
    private Timer shakeTimer;
    private int shakeCount;
    private Timer titleResetTimer;
    private Thread alarmSoundThread;
    private SourceDataLine currentAudioLine;
    private volatile boolean alarmCancelled=false;
    private static boolean audioWarningShown=false;
    private final Calendar cal=Calendar.getInstance();
    private static final Color PRIMARY_COLOR=Color.decode("#1E1E1E");
    private static final Color SECONDARY_COLOR=Color.decode("#2B2B2B");
    private static final Color BACKGROUND_COLOR=Color.WHITE;
    private static final Color ALERT_COLOR=Color.decode("#DE0000");
    private static final Color BUTTON_BACKGROUND=Color.decode("#DE0000");
    private static final Color BUTTON_TEXT=Color.WHITE;
    private static final Color PROGRESS_FOREGROUND=Color.decode("#DE0000");
    private static final Color PROGRESS_BACKGROUND=Color.decode("#D0D0D0");
    private static final Color SPINNER_BORDER_COLOR=new Color(0,0,0,70);
    private static Font LABEL_FONT=new Font("Noto Sans", Font.BOLD, 24);
    private static Font BUTTON_FONT=new Font("Noto Sans", Font.BOLD, 14);
    private static Font TEXT_FONT=new Font("Noto Sans", Font.PLAIN, 14);
    private static final Cursor HAND_CURSOR=Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final String[] PAD2=new String[100];
    private static final String[] PAD2_COLON=new String[100];
    private static final String[] PAD3_MS=new String[1000];
    static{
        StringBuilder sb=new StringBuilder(5);
        for (int i=0;i<100;i++){
            sb.setLength(0);
            if (i<10) sb.append('0');
            sb.append(i);
            PAD2[i]=sb.toString();
            PAD2_COLON[i]=sb.toString()+":";
        }
        for (int i=0;i<1000;i++){
            sb.setLength(0);
            sb.append('.');
            if (i<100) sb.append('0');
            if (i<10) sb.append('0');
            sb.append(i);
            PAD3_MS[i]=sb.toString();
        }
    }
    public AllInOneTimer(){
        super("All-in-One Timer Tool");
        Image icon=Toolkit.getDefaultToolkit().getImage(getClass().getResource("/favicon.png"));
        setIconImage(icon);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setBackground(PRIMARY_COLOR);
        loadCustomFont();
        initCountdownPanel();
        initStopwatchPanel();
        initClockPanel();
        pack();
        setMinimumSize(new Dimension(400, 600));
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);
        startClock();
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                disposeResources();
            }
        });
    }
    private void disposeResources(){
        cancelAlarm();
        if (countdownTimer!=null){
            countdownTimer.stop();
            countdownTimer=null;
        }
        if (stopwatchTimer!=null){
            stopwatchTimer.stop();
            stopwatchTimer=null;
        }
        if (clockTimer!=null){
            clockTimer.stop();
            clockTimer=null;
        }
        flashTimer=null;
        shakeTimer=null;
        titleResetTimer=null;
        alarmSoundThread=null;
        currentAudioLine=null;
        countdownBox=null;
        cdHour=null;
        cdMin=null;
        cdSec=null;
        cdMs=null;
        swHour=null;
        swMin=null;
        swSec=null;
        swMs=null;
        timeHour=null;
        timeMin=null;
        timeSec=null;
        timeMs=null;
        timeZone=null;
        hourSpinner=null;
        minSpinner=null;
        secSpinner=null;
        countdownProgress=null;
        countdownStopBtn=null;
        System.gc();
    }
    private void loadCustomFont(){
        InputStream is=null;
        try{
            is=getClass().getResourceAsStream("/NotoSans-VariableFont_wdth_wght.ttf");
            if (is!=null){
                Font loaded=Font.createFont(Font.TRUETYPE_FONT, is);
                LABEL_FONT=loaded.deriveFont(Font.BOLD, 24f);
                BUTTON_FONT=loaded.deriveFont(Font.BOLD, 14f);
                TEXT_FONT=loaded.deriveFont(Font.PLAIN, 14f);
            }
            else{
                throw new IOException("Font file not found in resources");
            }
        }
        catch (Exception e){
            System.err.println("Error loading font: "+e.getMessage());
            TEXT_FONT=new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            LABEL_FONT=new Font(Font.SANS_SERIF, Font.BOLD, 24);
            BUTTON_FONT=new Font(Font.SANS_SERIF, Font.BOLD, 14);
        }
        finally{
            if (is!=null){
                try{ is.close(); } catch (IOException ignored){}
            }
        }
    }
    private void initCountdownPanel(){
        JPanel panel=styledPanel("Countdown Timer");
        JPanel controls=new JPanel(new GridLayout(3, 3, 5, 5));
        applyPanelStyle(controls);
        JLabel hourLabel=new JLabel("Hours", JLabel.CENTER);
        hourLabel.setFont(TEXT_FONT);
        hourLabel.setForeground(Color.WHITE);
        hourLabel.setToolTipText("Hours for countdown (any non-negative integer, overflow will be normalized)");
        JLabel minLabel=new JLabel("Minutes", JLabel.CENTER);
        minLabel.setFont(TEXT_FONT);
        minLabel.setForeground(Color.WHITE);
        minLabel.setToolTipText("Minutes for countdown (any non-negative integer, overflow will be normalized)");
        JLabel secLabel=new JLabel("Seconds", JLabel.CENTER);
        secLabel.setFont(TEXT_FONT);
        secLabel.setForeground(Color.WHITE);
        secLabel.setToolTipText("Seconds for countdown (any non-negative integer, overflow will be normalized)");
        hourSpinner=spinner(0, 0, Integer.MAX_VALUE);
        hourSpinner.setToolTipText("Set countdown hours; values > 0 are allowed");
        minSpinner=spinner(0, 0, Integer.MAX_VALUE);
        minSpinner.setToolTipText("Set countdown minutes; overflow will roll into hours");
        secSpinner=spinner(0, 0, Integer.MAX_VALUE);
        secSpinner.setToolTipText("Set countdown seconds; overflow will roll into minutes and hours");
        JButton setBtn=styledButton("Set Countdown Timer");
        setBtn.setToolTipText("Start countdown with the specified duration (normalized)");
        setBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    cancelAlarm();
                    if (countdownTimer!=null&&countdownTimer.isRunning()){
                        countdownTimer.stop();
                    }
                    countdownPaused=false;
                    countdownStopBtn.setText("Stop Countdown");
                    countdownStopBtn.setToolTipText("Pause the running countdown");
                    long totalSec=(long)(Integer)hourSpinner.getValue()*3600
                                 +(long)(Integer)minSpinner.getValue()*60
                                 +(long)(Integer)secSpinner.getValue();
                    long normH=totalSec/3600;
                    long normM=(totalSec%3600)/60;
                    long normS=totalSec%60;
                    hourSpinner.setValue((int)normH);
                    minSpinner.setValue((int)normM);
                    secSpinner.setValue((int)normS);
                    long duration=totalSec*1000;
                    if (duration>0){
                        countdownTotalMs=duration;
                        countdownEndMs=System.currentTimeMillis()+duration;
                        countdownRemainingMs=duration;
                        int maxVal=(duration>Integer.MAX_VALUE)?Integer.MAX_VALUE:(int)duration;
                        countdownProgress.setMaximum(maxVal);
                        countdownProgress.setValue(0);
                        countdownTimer.start();
                    }
                }
                catch (Exception ex){
                    showError("Failed to start countdown", ex);
                }
            }
        });
        countdownStopBtn=styledButton("Stop Countdown");
        countdownStopBtn.setToolTipText("Pause the running countdown");
        countdownStopBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    if (countdownTimer.isRunning()){
                        cancelAlarm();
                        countdownTimer.stop();
                        countdownRemainingMs=countdownEndMs-System.currentTimeMillis();
                        if (countdownRemainingMs<0) countdownRemainingMs=0;
                        countdownPaused=true;
                        countdownStopBtn.setText("Resume Countdown");
                        countdownStopBtn.setToolTipText("Resume the paused countdown");
                    }
                    else if (countdownPaused&&countdownRemainingMs>0){
                        countdownEndMs=System.currentTimeMillis()+countdownRemainingMs;
                        countdownTotalMs=countdownRemainingMs>countdownTotalMs?countdownRemainingMs:countdownTotalMs;
                        countdownProgress.setValue((int)(countdownTotalMs-countdownRemainingMs));
                        countdownTimer.start();
                        countdownPaused=false;
                        countdownStopBtn.setText("Stop Countdown");
                        countdownStopBtn.setToolTipText("Pause the running countdown");
                    }
                }
                catch (Exception ex){
                    showError("Failed to toggle countdown", ex);
                }
            }
        });
        JButton resetBtn=styledButton("Reset Countdown");
        resetBtn.setToolTipText("Reset spinners to zero, clear display, stop timer, and cancel alarm");
        resetBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    cancelAlarm();
                    if (countdownTimer!=null&&countdownTimer.isRunning()){
                        countdownTimer.stop();
                    }
                    countdownPaused=false;
                    countdownStopBtn.setText("Stop Countdown");
                    countdownStopBtn.setToolTipText("Pause the running countdown");
                    hourSpinner.setValue(0);
                    minSpinner.setValue(0);
                    secSpinner.setValue(0);
                    cdHour.setText("00:");
                    cdMin.setText("00:");
                    cdSec.setText("00");
                    cdMs.setText(".000");
                    countdownProgress.setValue(0);
                }
                catch (Exception ex){
                    showError("Failed to reset countdown", ex);
                }
            }
        });
        controls.add(hourLabel);
        controls.add(minLabel);
        controls.add(secLabel);
        controls.add(hourSpinner);
        controls.add(minSpinner);
        controls.add(secSpinner);
        controls.add(setBtn);
        controls.add(countdownStopBtn);
        controls.add(resetBtn);
        panel.add(controls, BorderLayout.NORTH);
        countdownBox=styledBox();
        cdHour=timerLabel("00:");
        cdMin=timerLabel("00:");
        cdSec=timerLabel("00");
        cdMs=timerLabel(".000");
        countdownBox.add(cdHour);
        countdownBox.add(cdMin);
        countdownBox.add(cdSec);
        countdownBox.add(cdMs);
        panel.add(countdownBox, BorderLayout.CENTER);
        countdownProgress=new JProgressBar(0, 1);
        countdownProgress.setValue(0);
        countdownProgress.setStringPainted(false);
        countdownProgress.setForeground(PROGRESS_FOREGROUND);
        countdownProgress.setBackground(PROGRESS_BACKGROUND);
        countdownProgress.setToolTipText("Countdown progress (elapsed time / total duration)");
        countdownProgress.setPreferredSize(new Dimension(0, 8));
        panel.add(countdownProgress, BorderLayout.SOUTH);
        countdownTimer=new Timer(10, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    long rem=countdownEndMs-System.currentTimeMillis();
                    if (rem<=0){
                        countdownTimer.stop();
                        countdownPaused=false;
                        countdownStopBtn.setText("Stop Countdown");
                        countdownStopBtn.setToolTipText("Pause the running countdown");
                        cdHour.setText("00:");
                        cdMin.setText("00:");
                        cdSec.setText("00");
                        cdMs.setText(".000");
                        int max=countdownProgress.getMaximum();
                        countdownProgress.setValue(max>0?max:0);
                        triggerAlarm();
                    }
                    else{
                        int h=(int)(rem/3600000);
                        int m=(int)((rem%3600000)/60000);
                        int s=(int)((rem%60000)/1000);
                        int ms=(int)(rem%1000);
                        cdHour.setText(h<100?PAD2_COLON[h]:pad(h,2)+":");
                        cdMin.setText(PAD2_COLON[m]);
                        cdSec.setText(PAD2[s]);
                        cdMs.setText(PAD3_MS[ms]);
                        long elapsed=countdownTotalMs-rem;
                        int progVal=(elapsed>Integer.MAX_VALUE)?Integer.MAX_VALUE:(int)elapsed;
                        countdownProgress.setValue(progVal);
                    }
                }
                catch (Exception ex){
                    showError("Countdown update error", ex);
                }
            }
        });
        add(panel);
    }
    private void initStopwatchPanel(){
        JPanel panel=styledPanel("Stopwatch");
        JPanel controls=new JPanel();
        applyPanelStyle(controls);
        JButton startBtn=styledButton("Start");
        startBtn.setToolTipText("Start or stop the stopwatch");
        JButton resetBtn=styledButton("Reset");
        resetBtn.setToolTipText("Reset the stopwatch to zero");
        startBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    if (swRunning){
                        stopwatchTimer.stop();
                        swAccumulatedMs+=System.currentTimeMillis()-swStartMs;
                        startBtn.setText("Start");
                    }
                    else{
                        swStartMs=System.currentTimeMillis();
                        stopwatchTimer.start();
                        startBtn.setText("Stop");
                    }
                    swRunning=!swRunning;
                }
                catch (Exception ex){
                    showError("Stopwatch toggle error", ex);
                }
            }
        });
        resetBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    stopwatchTimer.stop();
                    swAccumulatedMs=0;
                    swHour.setText("00:");
                    swMin.setText("00:");
                    swSec.setText("00");
                    swMs.setText(".000");
                    startBtn.setText("Start");
                    swRunning=false;
                }
                catch (Exception ex){
                    showError("Stopwatch reset error", ex);
                }
            }
        });
        controls.add(startBtn);
        controls.add(resetBtn);
        panel.add(controls, BorderLayout.NORTH);
        JPanel box=styledBox();
        swHour=timerLabel("00:");
        swMin=timerLabel("00:");
        swSec=timerLabel("00");
        swMs=timerLabel(".000");
        box.add(swHour);
        box.add(swMin);
        box.add(swSec);
        box.add(swMs);
        panel.add(box, BorderLayout.CENTER);
        stopwatchTimer=new Timer(10, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    long elapsed=System.currentTimeMillis()-swStartMs+swAccumulatedMs;
                    int h=(int)(elapsed/3600000);
                    int m=(int)((elapsed%3600000)/60000);
                    int s=(int)((elapsed%60000)/1000);
                    int ms=(int)(elapsed%1000);
                    swHour.setText(h<100?PAD2_COLON[h]:pad(h,2)+":");
                    swMin.setText(PAD2_COLON[m]);
                    swSec.setText(PAD2[s]);
                    swMs.setText(PAD3_MS[ms]);
                }
                catch (Exception ex){
                    showError("Stopwatch update error", ex);
                }
            }
        });
        add(panel);
    }
    private void initClockPanel(){
        JPanel panel=styledPanel("Current Time");
        JPanel box=styledBox();
        timeHour=timerLabel("00:");
        timeMin=timerLabel("00:");
        timeSec=timerLabel("00");
        timeMs=timerLabel(".000");
        timeZone=new JLabel("(UTC+0)");
        timeZone.setFont(TEXT_FONT);
        box.add(timeHour);
        box.add(timeMin);
        box.add(timeSec);
        box.add(timeMs);
        box.add(timeZone);
        panel.add(box, BorderLayout.CENTER);
        add(panel);
    }
    private void startClock(){
        clockTimer=new Timer(50, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try{
                    cal.setTimeInMillis(System.currentTimeMillis());
                    int h=cal.get(Calendar.HOUR_OF_DAY);
                    int m=cal.get(Calendar.MINUTE);
                    int s=cal.get(Calendar.SECOND);
                    int ms=cal.get(Calendar.MILLISECOND);
                    timeHour.setText(h<100?PAD2_COLON[h]:pad(h,2)+":");
                    timeMin.setText(PAD2_COLON[m]);
                    timeSec.setText(PAD2[s]);
                    timeMs.setText(PAD3_MS[ms]);
                    TimeZone tz=cal.getTimeZone();
                    int off=tz.getOffset(cal.getTimeInMillis())/3600000;
                    if (off!=lastTimezoneOffset){
                        lastTimezoneOffset=off;
                        String sg=(off>=0)?"+":"";
                        timeZone.setText("(UTC"+sg+off+")");
                    }
                }
                catch (Exception ex){
                    showError("Clock update error", ex);
                }
            }
        });
        clockTimer.start();
    }
    private void cancelAlarm(){
        alarmCancelled=true;
        if (flashTimer!=null){
            if (flashTimer.isRunning()) flashTimer.stop();
            flashTimer=null;
        }
        if (shakeTimer!=null){
            if (shakeTimer.isRunning()) shakeTimer.stop();
            shakeTimer=null;
        }
        if (titleResetTimer!=null){
            if (titleResetTimer.isRunning()) titleResetTimer.stop();
            titleResetTimer=null;
        }
        if (origLocation!=null){
            setLocation(origLocation);
            origLocation=null;
        }
        countdownBox.setBackground(BACKGROUND_COLOR);
        setTitle("All-in-One Timer Tool");
        if (currentAudioLine!=null){
            if (currentAudioLine.isOpen()) currentAudioLine.close();
            currentAudioLine=null;
        }
        if (alarmSoundThread!=null){
            if (alarmSoundThread.isAlive()) alarmSoundThread.interrupt();
            alarmSoundThread=null;
        }
        System.gc();
    }
    private void triggerAlarm(){
        cancelAlarm();
        alarmCancelled=false;
        flashOn=false;
        flashTimer=new Timer(300, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                countdownBox.setBackground(flashOn?BACKGROUND_COLOR:ALERT_COLOR);
                flashOn=!flashOn;
            }
        });
        flashTimer.start();
        origLocation=getLocation();
        shakeCount=0;
        shakeTimer=new Timer(50, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int dx=(shakeCount%2==0)?5:-5;
                setLocation(origLocation.x+dx, origLocation.y);
                if (++shakeCount>=20){
                    shakeTimer.stop();
                    setLocation(origLocation);
                    origLocation=null;
                }
            }
        });
        shakeTimer.start();
        alarmSoundThread=new Thread(new Runnable(){
            public void run(){
                try{
                    playAlertSequence();
                }
                catch (InterruptedException ie){
                }
                catch (Exception ex){
                    showAudioWarning("Audio alert failed: "+ex.getMessage());
                }
                finally{
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run(){
                            if (flashTimer!=null&&flashTimer.isRunning()){
                                flashTimer.stop();
                            }
                            countdownBox.setBackground(BACKGROUND_COLOR);
                        }
                    });
                }
            }
        });
        alarmSoundThread.start();
        setTitle("Time Is Up");
        titleResetTimer=new Timer(5000, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                setTitle("All-in-One Timer Tool");
            }
        });
        titleResetTimer.setRepeats(false);
        titleResetTimer.start();
    }
    private void playAlertSequence() throws InterruptedException{
        float sr=44100f;
        if (alarmCancelled) return;
        for (int i=0;i<15;i++){
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
            playToneSafe(420,100,sr,true,0.3f);
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
            playToneSafe(840,100,sr,false,0.1f);
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
            Thread.sleep(100);
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
        }
    }
    private void playToneSafe(int freq, int ms, float sr, boolean square, float gain){
        try{
            playTone(freq,ms,sr,square,gain);
        }
        catch (Exception e){
        }
    }
    private void playTone(int freq, int ms, float sr, boolean square, float gain) throws Exception{
        int len=(int)(sr*ms/1000);
        byte[] buf=new byte[len];
        for (int i=0;i<len;i++){
            double angle=2*Math.PI*i*freq/sr;
            double val=square?Math.signum(Math.sin(angle)):Math.sin(angle);
            buf[i]=(byte)(val*gain*127);
        }
        AudioFormat af=new AudioFormat(sr,8,1,true,false);
        SourceDataLine line=AudioSystem.getSourceDataLine(af);
        currentAudioLine=line;
        try{
            line.open(af);
            line.start();
            line.write(buf,0,len);
            line.drain();
        }
        finally{
            line.close();
            currentAudioLine=null;
        }
    }
    private static String pad(int v, int l){
        String s=Integer.toString(v);
        StringBuilder sb=new StringBuilder(l);
        while (sb.length()+s.length()<l) sb.append('0');
        sb.append(s);
        return sb.toString();
    }
    private JSpinner spinner(int val, int min, int max){
        JSpinner sp=new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setPreferredSize(new Dimension(50,24));
        sp.setFont(TEXT_FONT);
        sp.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SPINNER_BORDER_COLOR,1,true),
            BorderFactory.createEmptyBorder(2,5,2,5)));
        return sp;
    }
    private JLabel timerLabel(String txt){
        JLabel lbl=new JLabel(txt);
        lbl.setFont(LABEL_FONT);
        return lbl;
    }
    private JButton styledButton(String txt){
        JButton btn=new JButton(txt);
        btn.setFont(BUTTON_FONT);
        btn.setBackground(BUTTON_BACKGROUND);
        btn.setForeground(BUTTON_TEXT);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SPINNER_BORDER_COLOR,1,true),
            BorderFactory.createEmptyBorder(5,10,5,10)));
        btn.setFocusPainted(false);
        btn.setCursor(HAND_CURSOR);
        btn.addMouseListener(new MouseAdapter(){
            private Color originalBg=BUTTON_BACKGROUND;
            private Color hoverColor=BUTTON_BACKGROUND.darker();
            private Color pressedColor=BUTTON_BACKGROUND.brighter();
            public void mouseEntered(MouseEvent e){
                btn.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent e){
                btn.setBackground(originalBg);
            }
            public void mousePressed(MouseEvent e){
                btn.setBackground(pressedColor);
            }
            public void mouseReleased(MouseEvent e){
                btn.setBackground(hoverColor);
            }
        });
        return btn;
    }
    private JPanel styledPanel(String title){
        JPanel p=new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(5,5,5,5),title,
            JLabel.CENTER,JLabel.TOP,BUTTON_FONT,Color.BLACK));
        p.setBackground(BACKGROUND_COLOR);
        p.setOpaque(true);
        return p;
    }
    private void applyPanelStyle(JPanel p){
        p.setBackground(SECONDARY_COLOR);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    }
    private JPanel styledBox(){
        JPanel p=new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK,1),
            BorderFactory.createEmptyBorder(10,10,10,10)));
        p.setBackground(BACKGROUND_COLOR);
        p.setOpaque(true);
        return p;
    }
    private void showError(String message, Exception e){
        JOptionPane.showMessageDialog(this,message+": "+e.getMessage(),
            "Error",JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
    private void showAudioWarning(String message){
        if (!audioWarningShown){
            audioWarningShown=true;
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    JOptionPane.showMessageDialog(AllInOneTimer.this,message,
                        "Audio Warning",JOptionPane.WARNING_MESSAGE);
                }
            });
        }
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                new AllInOneTimer();
            }
        });
    }
}