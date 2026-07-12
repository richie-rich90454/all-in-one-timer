/**
 * A comprehensive graphical user interface application that integrates a countdown timer with an
 * audio‑visual alarm, a stopwatch with millisecond precision, and a live clock displaying the
 * current system time and its Coordinated Universal Time (UTC) offset. The interface is built
 * using the Swing toolkit and arranges three functional panels vertically inside a resizable
 * frame, providing a clean, dark‑themed user experience with red accent colors.
 *
 * <p><b>Memory‑Efficient Design</b><br>
 * The application employs extensive memory‑saving design choices to keep the resident footprint
 * well under 80 MB even with a small Java heap (e.g., {@code -Xms16m -Xmx64m}). All string
 * representations of time values are statically precomputed, eliminating runtime allocation and
 * garbage collection pressure. Each time display (countdown, stopwatch, live clock) is rendered by
 * a single lightweight custom painting component ({@link CustomTimeDisplayComponent}) instead of
 * four separate {@code JLabel} instances, reducing the total component count by twelve. A single
 * master Swing {@link Timer} ticks every 10 ms and conditionally updates the active displays; the
 * live clock updates only every fifth tick (50 ms). All {@link Border} objects are shared as
 * {@code static final} constants, and a single shared {@link MouseAdapter} handles button hover
 * and press effects for every button, avoiding per‑button listener allocations. Explicit
 * {@code System.gc()} invocations are omitted, relying instead on timely nullification of
 * references to allow standard garbage collection without introducing stop‑the‑world pauses.
 *
 * <p><b>Countdown Timer</b><br>
 * The user sets the desired duration using three spinner components that each accept any
 * non‑negative integer value. Overflow is automatically normalized before the countdown begins,
 * and the spinners are updated to reflect the normalized values. The master {@code Timer} drives
 * the countdown logic: when active, the remaining time is computed every 10 ms and displayed on
 * the custom {@code TimeDisplay} component; a progress bar fills proportionally. Pause and resume
 * are supported without loss of time. A reset button clears all state. When the remaining time
 * reaches zero, the display stops and the alarm sequence is triggered.
 *
 * <p><b>Alarm Sequence</b><br>
 * A set of concurrent effects serves as the notification: background flashing of the countdown
 * {@code TimeDisplay} component between white and red at 300 ms intervals, window shaking
 * (20 ticks at 50 ms each), flashing of the system‑tray icon between normal and red, repeated
 * requests for window focus (causing the taskbar button to highlight), and a separate thread that
 * synthesizes and plays 15 tone pairs (420 Hz square wave then 840 Hz sine wave). The frame title
 * is temporarily set to "Time Is Up" and restored after five seconds. The alarm can be cancelled
 * instantly via a {@code volatile} flag; the audio line is closed externally, causing any blocking
 * write to fail immediately.
 *
 * <p><b>Stopwatch</b><br>
 * The stopwatch panel contains Start/Stop and Reset buttons. The master {@code Timer} updates the
 * stopwatch {@code TimeDisplay} when the stopwatch is running, accumulating elapsed time.
 * The accumulated value is preserved across pauses.
 *
 * <p><b>Live Clock</b><br>
 * Every 50 ms the master {@code Timer} updates the clock {@code TimeDisplay} and a separate
 * {@code JLabel} showing the UTC offset. The offset label is rebuilt only when the offset
 * actually changes, minimizing string allocations.
 *
 * <p><b>User Interface Styling and Tooltips</b><br>
 * A dark theme with red accents is used. Colors are stored as {@code static final} constants,
 * and pure white/black uses the predefined constants {@link Color#WHITE} and {@link Color#BLACK}.
 * Buttons share a single hover/press {@code MouseAdapter} that reads colors from client properties.
 * All interactive components carry tooltips and use the hand cursor. The countdown controls are
 * arranged in a 3×3 {@link GridLayout} to prevent layout shifts when the stop/resume button text
 * changes.
 *
 * <p><b>Precomputed Strings</b><br>
 * All possible two‑digit and three‑digit padded strings (00‑99, 00:‑99:, .000‑.999) are statically
 * initialized in the {@link #ZERO_PADDED_TWO_DIGIT_STRINGS}, {@link #ZERO_PADDED_TWO_DIGIT_COLON_STRINGS},
 * and {@link #ZERO_PADDED_THREE_DIGIT_MILLISECOND_STRINGS} arrays. The {@code TimeDisplay} and the
 * clock offset label use these strings directly, completely avoiding runtime concatenation.
 *
 * <p><b>Custom Font Loading</b><br>
 * The constructor attempts to load "NotoSans‑VariableFont_wdth_wght.ttf" from the classpath.
 * On success the font is derived without registering it with the graphics environment.
 * On failure, system sans‑serif fallbacks are used.
 *
 * <p><b>Resource Cleanup</b><br>
 * A {@link WindowAdapter} stops the master {@code Timer}, cancels the alarm, removes the tray icon,
 * and nullifies all component and thread references when the window is closed.
 *
 * <p><b>Exception Handling</b><br>
 * User actions are wrapped in try‑catch blocks and errors shown in modal dialogs. The audio thread
 * swallows {@link InterruptedException} silently and displays a warning dialog (once per session)
 * for other audio failures.
 *
 * @author richie-rich90454
 * @since 2026-07-12
 * @see javax.swing
 * @see javax.sound.sampled
 * @see java.awt
 */
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.border.Border;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.AWTException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
public class AllInOneTimer extends JFrame{
    // UI Components
    private JSpinner countdownHoursSpinnerComponent, countdownMinutesSpinnerComponent, countdownSecondsSpinnerComponent;
    private CustomTimeDisplayComponent countdownTimeDisplayComponent, stopwatchTimeDisplayComponent, liveClockTimeDisplayComponent;
    private JButton countdownStopAndResumeButtonComponent;
    private JProgressBar countdownProgressBarComponent;
    private JLabel timezoneOffsetDisplayLabel;
    // Countdown State
    private long countdownTimerEndTimeInMilliseconds, countdownTotalDurationInMilliseconds, countdownRemainingTimeWhenPausedInMilliseconds;
    private boolean countdownIsPausedFlag=false;
    private boolean countdownIsRunningFlag=false;
    // Stopwatch State
    private boolean stopwatchIsRunningFlag=false;
    private long stopwatchLastStartTimeInMilliseconds, stopwatchAccumulatedTimeBeforePauseInMilliseconds;
    // Master Timer & Tick
    private Timer masterSwingTimerForAllDisplays;
    private int masterTimerTickCounter;
    // Alarm Resources
    private Timer alarmFlashTimer, alarmShakeTimer, alarmTitleResetTimer;
    private boolean alarmFlashIsOnFlag;
    private Point windowOriginalLocationBeforeShaking;
    private int alarmShakeTickCounter;
    private int taskbarAttentionFlashCounter;
    private Thread alarmSoundPlaybackThread;
    private SourceDataLine currentAudioOutputLine;
    private volatile boolean alarmCancellationRequestedFlag=false;
    private static boolean audioWarningDialogHasBeenShown=false;
    // Clock & Tray
    private final Calendar calendarInstanceForClockUpdates=Calendar.getInstance();
    private int lastDisplayedUtcOffsetHours=Integer.MIN_VALUE;
    private TrayIcon systemTrayIcon;
    private Image normalIconImage, redAlarmIconImage;
    private boolean trayIconHasBeenAddedToSystemTray=false;
    // Audio Buffer
    private byte[] audioToneSynthesisBuffer;
    // Styling Constants
    private static final Color DARK_BACKGROUND_COLOR=Color.decode("#1E1E1E");
    private static final Color DARK_CONTROL_PANEL_COLOR=Color.decode("#2B2B2B");
    private static final Color WHITE_BACKGROUND_COLOR=Color.WHITE;
    private static final Color RED_ALERT_COLOR=Color.decode("#DE0000");
    private static final Color RED_BUTTON_BACKGROUND_COLOR=Color.decode("#DE0000");
    private static final Color WHITE_BUTTON_TEXT_COLOR=Color.WHITE;
    private static final Color RED_PROGRESS_BAR_FOREGROUND_COLOR=Color.decode("#DE0000");
    private static final Color LIGHT_GRAY_PROGRESS_BAR_BACKGROUND_COLOR=Color.decode("#D0D0D0");
    private static final Color TRANSLUCENT_BLACK_SPINNER_BORDER_COLOR=new Color(0, 0, 0, 70);
    // Fonts
    private static Font largeTimeLabelFont=new Font("Noto Sans", Font.BOLD, 24);
    private static Font buttonTextFont=new Font("Noto Sans", Font.BOLD, 14);
    private static Font generalTextFont=new Font("Noto Sans", Font.PLAIN, 14);
    // Shared UI Resources
    private static final Cursor HAND_CURSOR_FOR_INTERACTIVE_COMPONENTS=Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Border SPINNER_COMPONENT_BORDER=BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(TRANSLUCENT_BLACK_SPINNER_BORDER_COLOR, 1, true),
        BorderFactory.createEmptyBorder(2, 5, 2, 5));
    private static final Border BUTTON_COMPONENT_BORDER=BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(TRANSLUCENT_BLACK_SPINNER_BORDER_COLOR, 1, true),
        BorderFactory.createEmptyBorder(5, 10, 5, 10));
    private static final Border DISPLAY_BOX_BORDER=BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.BLACK, 1),
        BorderFactory.createEmptyBorder(10, 10, 10, 10));
    // Precomputed String Arrays
    private static final String[] ZERO_PADDED_TWO_DIGIT_STRINGS=new String[100];
    private static final String[] ZERO_PADDED_TWO_DIGIT_COLON_STRINGS=new String[100];
    private static final String[] ZERO_PADDED_THREE_DIGIT_MILLISECOND_STRINGS=new String[1000];
    static{
        StringBuilder sharedStringBuilderForInitialization=new StringBuilder(5);
        for (int index=0;index<100;index++){
            sharedStringBuilderForInitialization.setLength(0);
            if (index<10){
                sharedStringBuilderForInitialization.append('0');
            }
            sharedStringBuilderForInitialization.append(index);
            ZERO_PADDED_TWO_DIGIT_STRINGS[index]=sharedStringBuilderForInitialization.toString();
            ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[index]=sharedStringBuilderForInitialization.toString()+":";
        }
        for (int index=0;index<1000;index++){
            sharedStringBuilderForInitialization.setLength(0);
            sharedStringBuilderForInitialization.append('.');
            if (index<100){
                sharedStringBuilderForInitialization.append('0');
            }
            if (index<10){
                sharedStringBuilderForInitialization.append('0');
            }
            sharedStringBuilderForInitialization.append(index);
            ZERO_PADDED_THREE_DIGIT_MILLISECOND_STRINGS[index]=sharedStringBuilderForInitialization.toString();
        }
    }
    // Shared Button MouseAdapter
    private static final MouseAdapter SHARED_BUTTON_HOVER_PRESS_LISTENER=new MouseAdapter(){
        public void mouseEntered(MouseEvent mouseEventObject){
            JButton sourceButton=(JButton)mouseEventObject.getSource();
            Color hoverColor=(Color)sourceButton.getClientProperty("buttonHoverBackgroundColor");
            sourceButton.setBackground(hoverColor);
        }
        public void mouseExited(MouseEvent mouseEventObject){
            JButton sourceButton=(JButton)mouseEventObject.getSource();
            Color originalColor=(Color)sourceButton.getClientProperty("buttonOriginalBackgroundColor");
            sourceButton.setBackground(originalColor);
        }
        public void mousePressed(MouseEvent mouseEventObject){
            JButton sourceButton=(JButton)mouseEventObject.getSource();
            Color pressedColor=(Color)sourceButton.getClientProperty("buttonPressedBackgroundColor");
            sourceButton.setBackground(pressedColor);
        }
        public void mouseReleased(MouseEvent mouseEventObject){
            JButton sourceButton=(JButton)mouseEventObject.getSource();
            boolean isMouseInsideButton=sourceButton.contains(mouseEventObject.getPoint());
            if (isMouseInsideButton){
                Color hoverColor=(Color)sourceButton.getClientProperty("buttonHoverBackgroundColor");
                sourceButton.setBackground(hoverColor);
            }
            else{
                Color originalColor=(Color)sourceButton.getClientProperty("buttonOriginalBackgroundColor");
                sourceButton.setBackground(originalColor);
            }
        }
    };
    /**
     * Constructs the main application window, initializes all UI components, loads the custom font,
     * sets up the system tray icon, creates the master timer, and makes the frame visible.
     * The constructor also registers a window listener to clean up resources on close.
     *
     * <p>This is the entry point of the GUI; it is typically called from the {@link #main} method
     * via {@link SwingUtilities#invokeLater} to ensure thread‑safe GUI creation.
     *
     * @throws RuntimeException if a critical error occurs during initialization (e.g., missing
     *         resources), although the constructor attempts to recover gracefully.
     * @see #loadCustomFont()
     * @see #initializeCountdownPanel()
     * @see #initializeStopwatchPanel()
     * @see #initializeLiveClockPanel()
     * @see #initializeSystemTrayIfSupported()
     * @see #createReusableAlarmTimersAndBuffers()
     * @see #disposeAllResourcesAndCleanup()
     */
    public AllInOneTimer(){
        super("All-in-One Timer Tool");
        Image applicationIconImage=Toolkit.getDefaultToolkit().getImage(getClass().getResource("/favicon.png"));
        if (applicationIconImage==null){
            applicationIconImage=createDefaultFallbackIconImage();
        }
        setIconImage(applicationIconImage);
        normalIconImage=applicationIconImage;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(DARK_BACKGROUND_COLOR);
        BoxLayout mainVerticalLayout=new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        setLayout(mainVerticalLayout);
        loadCustomFont();
        initializeCountdownPanel();
        initializeStopwatchPanel();
        initializeLiveClockPanel();
        pack();
        setMinimumSize(new Dimension(400, 600));
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);
        masterSwingTimerForAllDisplays=new Timer(10, new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleMasterTimerTick(actionEventObject);
            }
        });
        masterSwingTimerForAllDisplays.start();
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent windowEventObject){
                disposeAllResourcesAndCleanup();
            }
        });
        initializeSystemTrayIfSupported();
        createReusableAlarmTimersAndBuffers();
    }
    /**
     * Creates and initializes the reusable alarm timers (flash, shake, title reset) and the
     * audio tone synthesis buffer. These timers are stopped and restarted as needed during
     * the alarm sequence. The buffer size is fixed at 4410 bytes (100 ms at 44.1 kHz), which
     * is sufficient for the tones used.
     *
     * <p>This method is called once during construction to avoid repeated object creation.
     *
     * @see #alarmFlashTimer
     * @see #alarmShakeTimer
     * @see #alarmTitleResetTimer
     * @see #audioToneSynthesisBuffer
     */
    private void createReusableAlarmTimersAndBuffers(){
        alarmFlashTimer=new Timer(300, new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleAlarmFlashTimerTick();
            }
        });
        alarmFlashTimer.setRepeats(true);
        alarmShakeTimer=new Timer(50, new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleAlarmShakeTimerTick();
            }
        });
        alarmShakeTimer.setRepeats(true);
        alarmTitleResetTimer=new Timer(5000, new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleAlarmTitleResetTimerExpired();
            }
        });
        alarmTitleResetTimer.setRepeats(false);
        audioToneSynthesisBuffer=new byte[4410];
    }
    /**
     * Adds a tray icon to the system tray if the platform supports it. The icon is initially
     * set to the normal (non‑alarm) icon. If the system tray is not supported, this method
     * does nothing.
     *
     * <p>The red alarm icon is created lazily when needed using
     * {@link #createRedTintedIconFromNormalIcon}.
     *
     * @see SystemTray#isSupported()
     * @see #createRedTintedIconFromNormalIcon(Image)
     * @see #systemTrayIcon
     * @see #trayIconHasBeenAddedToSystemTray
     */
    private void initializeSystemTrayIfSupported(){
        boolean systemTrayIsSupported=SystemTray.isSupported();
        if (!systemTrayIsSupported){
            return;
        }
        if (redAlarmIconImage==null){
            redAlarmIconImage=createRedTintedIconFromNormalIcon(normalIconImage);
        }
        systemTrayIcon=new TrayIcon(normalIconImage, "All-in-One Timer Tool", null);
        systemTrayIcon.setImageAutoSize(true);
        try{
            SystemTray.getSystemTray().add(systemTrayIcon);
            trayIconHasBeenAddedToSystemTray=true;
        }
        catch (AWTException awtExceptionObject){
            System.err.println("Could not add tray icon: "+awtExceptionObject.getMessage());
        }
    }
    /**
     * Creates a fallback 16×16 red icon to use when the application's favicon resource
     * ({@code /favicon.png}) cannot be loaded. The icon is a simple filled red square.
     *
     * @return a {@link BufferedImage} of size 16×16 with red fill.
     * @see #createDefaultFallbackIconImage()
     */
    private Image createDefaultFallbackIconImage(){
        BufferedImage fallbackIconImage=new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphicsContext=fallbackIconImage.createGraphics();
        graphicsContext.setColor(Color.RED);
        graphicsContext.fillRect(0, 0, 16, 16);
        graphicsContext.dispose();
        return fallbackIconImage;
    }
    /**
     * Creates a red‑tinted copy of the original icon by setting the RGB components of each
     * non‑transparent pixel to {@code 0xDE0000} (red), while preserving the alpha channel.
     * This tinted icon is used during the alarm sequence to flash the tray icon.
     *
     * @param originalIconImage the original icon image (may be any size, but must have valid
     *                          width and height; if not, a fallback size of 16×16 is used).
     * @return a new {@link BufferedImage} with the same dimensions as the original, but
     *         with all opaque pixels tinted red.
     * @see #redAlarmIconImage
     * @see #handleAlarmFlashTimerTick()
     */
    private Image createRedTintedIconFromNormalIcon(Image originalIconImage){
        int widthOfOriginalImage=originalIconImage.getWidth(null);
        int heightOfOriginalImage=originalIconImage.getHeight(null);
        if (widthOfOriginalImage<=0||heightOfOriginalImage<=0){
            widthOfOriginalImage=16;
            heightOfOriginalImage=16;
        }
        BufferedImage redTintedIconImage=new BufferedImage(widthOfOriginalImage, heightOfOriginalImage, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphicsContext=redTintedIconImage.createGraphics();
        graphicsContext.drawImage(originalIconImage, 0, 0, null);
        graphicsContext.dispose();
        for (int yCoordinate=0;yCoordinate<heightOfOriginalImage;yCoordinate++){
            for (int xCoordinate=0;xCoordinate<widthOfOriginalImage;xCoordinate++){
                int pixelRgb=redTintedIconImage.getRGB(xCoordinate, yCoordinate);
                int alphaChannel=(pixelRgb>>24)&0xff;
                if (alphaChannel==0){
                    continue;
                }
                int redPixel=(alphaChannel<<24)|0xDE0000;
                redTintedIconImage.setRGB(xCoordinate, yCoordinate, redPixel);
            }
        }
        return redTintedIconImage;
    }
    /**
     * Handles each tick of the master Swing timer (every 10 ms). This method increments the
     * tick counter and, based on the current state, updates the countdown display (if running),
     * the stopwatch display (if running), and the live clock display (every fifth tick).
     *
     * <p>The live clock is updated less frequently to reduce CPU load, as its millisecond
     * precision is not required.
     *
     * @param actionEventObject the event object from the timer (unused but required by
     *                          {@link ActionListener}).
     * @see #updateCountdownDisplayWithCurrentTime(long)
     * @see #updateStopwatchDisplayWithCurrentTime(long)
     * @see #updateLiveClockDisplayWithCurrentTime(long)
     */
    private void handleMasterTimerTick(ActionEvent actionEventObject){
        masterTimerTickCounter=masterTimerTickCounter+1;
        long currentSystemTimeMilliseconds=System.currentTimeMillis();
        if (countdownIsRunningFlag){
            updateCountdownDisplayWithCurrentTime(currentSystemTimeMilliseconds);
        }
        if (stopwatchIsRunningFlag){
            updateStopwatchDisplayWithCurrentTime(currentSystemTimeMilliseconds);
        }
        boolean shouldUpdateClock=(masterTimerTickCounter%5)==0;
        if (shouldUpdateClock){
            updateLiveClockDisplayWithCurrentTime(currentSystemTimeMilliseconds);
        }
    }
    /**
     * Updates the countdown display with the remaining time based on the current system time.
     * If the countdown has finished (remaining time ≤ 0), it stops the countdown, updates the
     * display to "00:00:00.000", sets the progress bar to maximum, and triggers the alarm sequence.
     * Otherwise, it computes the hours, minutes, seconds, and milliseconds using precomputed
     * string arrays and updates the display and progress bar.
     *
     * @param currentTimeMillis the current system time in milliseconds, used to calculate
     *                          remaining time.
     * @see #triggerAlarmSequence()
     * @see #countdownTimerEndTimeInMilliseconds
     * @see #countdownTotalDurationInMilliseconds
     * @see CustomTimeDisplayComponent#setTime(String, String, String, String)
     */
    private void updateCountdownDisplayWithCurrentTime(long currentTimeMillis){
        long remainingTimeMilliseconds=countdownTimerEndTimeInMilliseconds-currentTimeMillis;
        boolean countdownHasFinished=(remainingTimeMilliseconds<=0);
        if (countdownHasFinished){
            countdownIsRunningFlag=false;
            countdownIsPausedFlag=false;
            String stopText="Stop Countdown";
            countdownStopAndResumeButtonComponent.setText(stopText);
            String pauseTooltip="Pause the running countdown";
            countdownStopAndResumeButtonComponent.setToolTipText(pauseTooltip);
            String zeroHoursColon="00:";
            String zeroMinutesColon="00:";
            String zeroSeconds="00";
            String zeroMillisecondsDot=".000";
            countdownTimeDisplayComponent.setTime(zeroHoursColon, zeroMinutesColon, zeroSeconds, zeroMillisecondsDot);
            int maximumProgress=countdownProgressBarComponent.getMaximum();
            boolean maximumIsPositive=(maximumProgress>0);
            int progressValueToSet;
            if (maximumIsPositive){
                progressValueToSet=maximumProgress;
            }
            else{
                progressValueToSet=0;
            }
            countdownProgressBarComponent.setValue(progressValueToSet);
            triggerAlarmSequence();
        }
        else{
            int hoursPart=(int)(remainingTimeMilliseconds/3600000);
            int minutesPart=(int)((remainingTimeMilliseconds%3600000)/60000);
            int secondsPart=(int)((remainingTimeMilliseconds%60000)/1000);
            int millisecondsPart=(int)(remainingTimeMilliseconds%1000);
            String hoursColonString;
            boolean hoursLessThan100=(hoursPart<100);
            if (hoursLessThan100){
                hoursColonString=ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[hoursPart];
            }
            else{
                hoursColonString=padIntegerWithLeadingZeros(hoursPart, 2)+":";
            }
            String minutesColonString=ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[minutesPart];
            String secondsString=ZERO_PADDED_TWO_DIGIT_STRINGS[secondsPart];
            String millisecondsDotString=ZERO_PADDED_THREE_DIGIT_MILLISECOND_STRINGS[millisecondsPart];
            countdownTimeDisplayComponent.setTime(hoursColonString, minutesColonString, secondsString, millisecondsDotString);
            long elapsedTimeMilliseconds=countdownTotalDurationInMilliseconds-remainingTimeMilliseconds;
            boolean elapsedExceedsIntegerMax=(elapsedTimeMilliseconds>Integer.MAX_VALUE);
            int progressBarValue;
            if (elapsedExceedsIntegerMax){
                progressBarValue=Integer.MAX_VALUE;
            }
            else{
                progressBarValue=(int)elapsedTimeMilliseconds;
            }
            countdownProgressBarComponent.setValue(progressBarValue);
        }
    }
    /**
     * Updates the stopwatch display with the total elapsed time since the stopwatch was started,
     * taking into account any paused accumulation. The elapsed time is computed from the current
     * system time and the stored start time and accumulated pause time.
     *
     * @param currentTimeMillis the current system time in milliseconds, used to compute the
     *                          live elapsed portion.
     * @see #stopwatchLastStartTimeInMilliseconds
     * @see #stopwatchAccumulatedTimeBeforePauseInMilliseconds
     * @see CustomTimeDisplayComponent#setTime(String, String, String, String)
     */
    private void updateStopwatchDisplayWithCurrentTime(long currentTimeMillis){
        long elapsedTimeSinceLastStart=currentTimeMillis-stopwatchLastStartTimeInMilliseconds;
        long totalElapsedTimeMilliseconds=elapsedTimeSinceLastStart+stopwatchAccumulatedTimeBeforePauseInMilliseconds;
        int hoursPart=(int)(totalElapsedTimeMilliseconds/3600000);
        int minutesPart=(int)((totalElapsedTimeMilliseconds%3600000)/60000);
        int secondsPart=(int)((totalElapsedTimeMilliseconds%60000)/1000);
        int millisecondsPart=(int)(totalElapsedTimeMilliseconds%1000);
        String hoursColonString;
        boolean hoursLessThan100=(hoursPart<100);
        if (hoursLessThan100){
            hoursColonString=ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[hoursPart];
        }
        else{
            hoursColonString=padIntegerWithLeadingZeros(hoursPart, 2)+":";
        }
        String minutesColonString=ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[minutesPart];
        String secondsString=ZERO_PADDED_TWO_DIGIT_STRINGS[secondsPart];
        String millisecondsDotString=ZERO_PADDED_THREE_DIGIT_MILLISECOND_STRINGS[millisecondsPart];
        stopwatchTimeDisplayComponent.setTime(hoursColonString, minutesColonString, secondsString, millisecondsDotString);
    }
    /**
     * Updates the live clock display with the current system time and the UTC offset. The clock
     * display shows hours, minutes, seconds, and milliseconds. The UTC offset label is updated
     * only when the offset changes (e.g., due to daylight saving transitions) to minimize
     * string allocations.
     *
     * @param currentTimeMillis the current system time in milliseconds, used to set the
     *                          {@link Calendar} instance.
     * @see #calendarInstanceForClockUpdates
     * @see #lastDisplayedUtcOffsetHours
     * @see CustomTimeDisplayComponent#setTime(String, String, String, String)
     */
    private void updateLiveClockDisplayWithCurrentTime(long currentTimeMillis){
        calendarInstanceForClockUpdates.setTimeInMillis(currentTimeMillis);
        int hoursPart=calendarInstanceForClockUpdates.get(Calendar.HOUR_OF_DAY);
        int minutesPart=calendarInstanceForClockUpdates.get(Calendar.MINUTE);
        int secondsPart=calendarInstanceForClockUpdates.get(Calendar.SECOND);
        int millisecondsPart=calendarInstanceForClockUpdates.get(Calendar.MILLISECOND);
        String hoursColonString;
        boolean hoursLessThan100=(hoursPart<100);
        if (hoursLessThan100){
            hoursColonString=ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[hoursPart];
        }
        else{
            hoursColonString=padIntegerWithLeadingZeros(hoursPart, 2)+":";
        }
        String minutesColonString=ZERO_PADDED_TWO_DIGIT_COLON_STRINGS[minutesPart];
        String secondsString=ZERO_PADDED_TWO_DIGIT_STRINGS[secondsPart];
        String millisecondsDotString=ZERO_PADDED_THREE_DIGIT_MILLISECOND_STRINGS[millisecondsPart];
        liveClockTimeDisplayComponent.setTime(hoursColonString, minutesColonString, secondsString, millisecondsDotString);
        TimeZone timeZoneObject=calendarInstanceForClockUpdates.getTimeZone();
        long currentTimeInMillisForOffset=calendarInstanceForClockUpdates.getTimeInMillis();
        int rawOffsetMilliseconds=timeZoneObject.getOffset(currentTimeInMillisForOffset);
        int offsetHours=rawOffsetMilliseconds/3600000;
        boolean offsetHasChanged=(offsetHours!=lastDisplayedUtcOffsetHours);
        if (offsetHasChanged){
            lastDisplayedUtcOffsetHours=offsetHours;
            boolean offsetIsNonNegative=(offsetHours>=0);
            String signString;
            if (offsetIsNonNegative){
                signString="+";
            }
            else{
                signString="";
            }
            String newOffsetLabelText="(UTC"+signString+offsetHours+")";
            timezoneOffsetDisplayLabel.setText(newOffsetLabelText);
        }
    }
    /**
     * Performs a comprehensive cleanup of all resources when the window is closed or when
     * the application is shutting down. This includes stopping the master timer, cancelling
     * any active alarm, removing the tray icon, and nullifying all component and thread
     * references to allow garbage collection.
     *
     * <p>This method is called from the window closing listener and should be the last
     * method invoked before the application exits.
     *
     * @see #cancelAnyActiveAlarmAndCleanup()
     * @see #masterSwingTimerForAllDisplays
     * @see #systemTrayIcon
     */
    private void disposeAllResourcesAndCleanup(){
        cancelAnyActiveAlarmAndCleanup();
        if (masterSwingTimerForAllDisplays!=null){
            masterSwingTimerForAllDisplays.stop();
            masterSwingTimerForAllDisplays=null;
        }
        if (trayIconHasBeenAddedToSystemTray&&systemTrayIcon!=null){
            SystemTray.getSystemTray().remove(systemTrayIcon);
            trayIconHasBeenAddedToSystemTray=false;
        }
        alarmFlashTimer=null;
        alarmShakeTimer=null;
        alarmTitleResetTimer=null;
        alarmSoundPlaybackThread=null;
        currentAudioOutputLine=null;
        countdownTimeDisplayComponent=null;
        stopwatchTimeDisplayComponent=null;
        liveClockTimeDisplayComponent=null;
        timezoneOffsetDisplayLabel=null;
        countdownHoursSpinnerComponent=null;
        countdownMinutesSpinnerComponent=null;
        countdownSecondsSpinnerComponent=null;
        countdownProgressBarComponent=null;
        countdownStopAndResumeButtonComponent=null;
    }
    /**
     * Attempts to load the custom font "NotoSans‑VariableFont_wdth_wght.ttf" from the classpath.
     * If the font is found, it is derived into three styles (bold 24pt for time display,
     * bold 14pt for buttons, and plain 14pt for general text). If loading fails, fallback
     * system sans‑serif fonts are used with the same sizes.
     *
     * <p>The font is not registered with the graphics environment, which avoids global
     * side effects.
     *
     * @see #largeTimeLabelFont
     * @see #buttonTextFont
     * @see #generalTextFont
     */
    private void loadCustomFont(){
        InputStream fontFileInputStream=null;
        try{
            fontFileInputStream=getClass().getResourceAsStream("/NotoSans-VariableFont_wdth_wght.ttf");
            if (fontFileInputStream!=null){
                Font loadedTrueTypeFont=Font.createFont(Font.TRUETYPE_FONT, fontFileInputStream);
                largeTimeLabelFont=loadedTrueTypeFont.deriveFont(Font.BOLD, 24f);
                buttonTextFont=loadedTrueTypeFont.deriveFont(Font.BOLD, 14f);
                generalTextFont=loadedTrueTypeFont.deriveFont(Font.PLAIN, 14f);
            }
            else{
                throw new IOException("Font file not found in resources");
            }
        }
        catch (Exception exceptionObject){
            System.err.println("Error loading font: "+exceptionObject.getMessage());
            generalTextFont=new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            largeTimeLabelFont=new Font(Font.SANS_SERIF, Font.BOLD, 24);
            buttonTextFont=new Font(Font.SANS_SERIF, Font.BOLD, 14);
        }
        finally{
            if (fontFileInputStream!=null){
                try{
                    fontFileInputStream.close();
                }
                catch (IOException ignoredIoException){
                }
            }
        }
    }
    /**
     * Builds and adds the countdown panel to the main frame. The panel contains a control
     * area with spinners for hours, minutes, and seconds, three buttons (Set, Stop/Resume,
     * Reset), a custom time display, and a progress bar. The panel uses a {@link GridLayout}
     * for the controls to maintain alignment.
     *
     * <p>This method also sets up the action listeners for the buttons.
     *
     * @see #createStyledTitledPanel(String)
     * @see #createStyledSpinner(int, int, int)
     * @see #createStyledButton(String)
     * @see #handleSetCountdownButtonPressed()
     * @see #handleStopResumeCountdownButtonPressed()
     * @see #handleResetCountdownButtonPressed()
     */
    private void initializeCountdownPanel(){
        JPanel countdownPanel=createStyledTitledPanel("Countdown Timer");
        JPanel controlsPanel=new JPanel(new GridLayout(3, 3, 5, 5));
        applyDarkControlPanelStyle(controlsPanel);
        JLabel hoursLabel=new JLabel("Hours", JLabel.CENTER);
        hoursLabel.setFont(generalTextFont);
        hoursLabel.setForeground(Color.WHITE);
        hoursLabel.setToolTipText("Hours for countdown (any non-negative integer, overflow will be normalized)");
        JLabel minutesLabel=new JLabel("Minutes", JLabel.CENTER);
        minutesLabel.setFont(generalTextFont);
        minutesLabel.setForeground(Color.WHITE);
        minutesLabel.setToolTipText("Minutes for countdown (any non-negative integer, overflow will be normalized)");
        JLabel secondsLabel=new JLabel("Seconds", JLabel.CENTER);
        secondsLabel.setFont(generalTextFont);
        secondsLabel.setForeground(Color.WHITE);
        secondsLabel.setToolTipText("Seconds for countdown (any non-negative integer, overflow will be normalized)");
        countdownHoursSpinnerComponent=createStyledSpinner(0, 0, Integer.MAX_VALUE);
        countdownHoursSpinnerComponent.setToolTipText("Set countdown hours; values > 0 are allowed");
        countdownMinutesSpinnerComponent=createStyledSpinner(0, 0, Integer.MAX_VALUE);
        countdownMinutesSpinnerComponent.setToolTipText("Set countdown minutes; overflow will roll into hours");
        countdownSecondsSpinnerComponent=createStyledSpinner(0, 0, Integer.MAX_VALUE);
        countdownSecondsSpinnerComponent.setToolTipText("Set countdown seconds; overflow will roll into minutes and hours");
        JButton setCountdownButton=createStyledButton("Set Countdown Timer");
        setCountdownButton.setToolTipText("Start countdown with the specified duration (normalized)");
        setCountdownButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleSetCountdownButtonPressed();
            }
        });
        countdownStopAndResumeButtonComponent=createStyledButton("Stop Countdown");
        countdownStopAndResumeButtonComponent.setToolTipText("Pause the running countdown");
        countdownStopAndResumeButtonComponent.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleStopResumeCountdownButtonPressed();
            }
        });
        JButton resetCountdownButton=createStyledButton("Reset Countdown");
        resetCountdownButton.setToolTipText("Reset spinners to zero, clear display, stop timer, and cancel alarm");
        resetCountdownButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleResetCountdownButtonPressed();
            }
        });
        controlsPanel.add(hoursLabel);
        controlsPanel.add(minutesLabel);
        controlsPanel.add(secondsLabel);
        controlsPanel.add(countdownHoursSpinnerComponent);
        controlsPanel.add(countdownMinutesSpinnerComponent);
        controlsPanel.add(countdownSecondsSpinnerComponent);
        controlsPanel.add(setCountdownButton);
        controlsPanel.add(countdownStopAndResumeButtonComponent);
        controlsPanel.add(resetCountdownButton);
        countdownPanel.add(controlsPanel, BorderLayout.NORTH);
        countdownTimeDisplayComponent=new CustomTimeDisplayComponent();
        countdownTimeDisplayComponent.setBorder(DISPLAY_BOX_BORDER);
        countdownTimeDisplayComponent.setBackground(WHITE_BACKGROUND_COLOR);
        countdownTimeDisplayComponent.setTime("00:", "00:", "00", ".000");
        countdownPanel.add(countdownTimeDisplayComponent, BorderLayout.CENTER);
        countdownProgressBarComponent=new JProgressBar(0, 1);
        countdownProgressBarComponent.setValue(0);
        countdownProgressBarComponent.setStringPainted(false);
        countdownProgressBarComponent.setForeground(RED_PROGRESS_BAR_FOREGROUND_COLOR);
        countdownProgressBarComponent.setBackground(LIGHT_GRAY_PROGRESS_BAR_BACKGROUND_COLOR);
        countdownProgressBarComponent.setToolTipText("Countdown progress (elapsed time / total duration)");
        countdownProgressBarComponent.setPreferredSize(new Dimension(0, 8));
        countdownPanel.add(countdownProgressBarComponent, BorderLayout.SOUTH);
        add(countdownPanel);
    }
    /**
     * Handles the "Set Countdown Timer" button press. This method cancels any active alarm,
     * resets the countdown state, reads the values from the spinners, normalizes them (converting
     * total seconds to hours/minutes/seconds), and starts the countdown if the total duration
     * is greater than zero. The spinners are updated to reflect the normalized values.
     *
     * <p>If an exception occurs (e.g., invalid spinner values), an error dialog is shown.
     *
     * @see #cancelAnyActiveAlarmAndCleanup()
     * @see #showErrorDialog(String, Exception)
     */
    private void handleSetCountdownButtonPressed(){
        try{
            cancelAnyActiveAlarmAndCleanup();
            countdownIsRunningFlag=false;
            countdownIsPausedFlag=false;
            String stopText="Stop Countdown";
            countdownStopAndResumeButtonComponent.setText(stopText);
            String pauseTooltip="Pause the running countdown";
            countdownStopAndResumeButtonComponent.setToolTipText(pauseTooltip);
            int hoursSpinnerValue=(Integer)countdownHoursSpinnerComponent.getValue();
            int minutesSpinnerValue=(Integer)countdownMinutesSpinnerComponent.getValue();
            int secondsSpinnerValue=(Integer)countdownSecondsSpinnerComponent.getValue();
            long hoursInSeconds=(long)hoursSpinnerValue*3600;
            long minutesInSeconds=(long)minutesSpinnerValue*60;
            long totalSeconds=hoursInSeconds+minutesInSeconds+(long)secondsSpinnerValue;
            long normalizedHours=totalSeconds/3600;
            long normalizedMinutes=(totalSeconds%3600)/60;
            long normalizedSeconds=totalSeconds%60;
            countdownHoursSpinnerComponent.setValue((int)normalizedHours);
            countdownMinutesSpinnerComponent.setValue((int)normalizedMinutes);
            countdownSecondsSpinnerComponent.setValue((int)normalizedSeconds);
            long durationMilliseconds=totalSeconds*1000;
            boolean durationGreaterThanZero=(durationMilliseconds>0);
            if (durationGreaterThanZero){
                countdownTotalDurationInMilliseconds=durationMilliseconds;
                long currentTime=System.currentTimeMillis();
                countdownTimerEndTimeInMilliseconds=currentTime+durationMilliseconds;
                countdownRemainingTimeWhenPausedInMilliseconds=durationMilliseconds;
                boolean durationExceedsIntegerMax=(durationMilliseconds>Integer.MAX_VALUE);
                int maximumProgressValue;
                if (durationExceedsIntegerMax){
                    maximumProgressValue=Integer.MAX_VALUE;
                }
                else{
                    maximumProgressValue=(int)durationMilliseconds;
                }
                countdownProgressBarComponent.setMaximum(maximumProgressValue);
                countdownProgressBarComponent.setValue(0);
                countdownIsRunningFlag=true;
            }
        }
        catch (Exception exceptionObject){
            showErrorDialog("Failed to start countdown", exceptionObject);
        }
    }
    /**
     * Handles the "Stop/Resume" button press for the countdown. If the countdown is running,
     * it pauses it, stores the remaining time, and changes the button text to "Resume Countdown".
     * If the countdown is paused and has remaining time, it resumes the countdown, updates the
     * progress bar, and changes the button text back to "Stop Countdown".
     *
     * <p>Any active alarm is cancelled when pausing.
     *
     * @see #cancelAnyActiveAlarmAndCleanup()
     * @see #countdownIsPausedFlag
     * @see #countdownIsRunningFlag
     * @see #countdownRemainingTimeWhenPausedInMilliseconds
     */
    private void handleStopResumeCountdownButtonPressed(){
        try{
            if (countdownIsRunningFlag){
                cancelAnyActiveAlarmAndCleanup();
                countdownIsRunningFlag=false;
                long currentTime=System.currentTimeMillis();
                long remaining=countdownTimerEndTimeInMilliseconds-currentTime;
                if (remaining<0){
                    countdownRemainingTimeWhenPausedInMilliseconds=0;
                }
                else{
                    countdownRemainingTimeWhenPausedInMilliseconds=remaining;
                }
                countdownIsPausedFlag=true;
                String resumeText="Resume Countdown";
                countdownStopAndResumeButtonComponent.setText(resumeText);
                String resumeTooltip="Resume the paused countdown";
                countdownStopAndResumeButtonComponent.setToolTipText(resumeTooltip);
            }
            else if (countdownIsPausedFlag&&countdownRemainingTimeWhenPausedInMilliseconds>0){
                long currentTime=System.currentTimeMillis();
                countdownTimerEndTimeInMilliseconds=currentTime+countdownRemainingTimeWhenPausedInMilliseconds;
                boolean remainingGreaterThanTotal=(countdownRemainingTimeWhenPausedInMilliseconds>countdownTotalDurationInMilliseconds);
                if (remainingGreaterThanTotal){
                    countdownTotalDurationInMilliseconds=countdownRemainingTimeWhenPausedInMilliseconds;
                }
                long elapsedBeforePause=countdownTotalDurationInMilliseconds-countdownRemainingTimeWhenPausedInMilliseconds;
                countdownProgressBarComponent.setValue((int)elapsedBeforePause);
                countdownIsRunningFlag=true;
                countdownIsPausedFlag=false;
                String stopText="Stop Countdown";
                countdownStopAndResumeButtonComponent.setText(stopText);
                String pauseTooltip="Pause the running countdown";
                countdownStopAndResumeButtonComponent.setToolTipText(pauseTooltip);
            }
        }
        catch (Exception exceptionObject){
            showErrorDialog("Failed to toggle countdown", exceptionObject);
        }
    }
    /**
     * Handles the "Reset Countdown" button press. This cancels any active alarm, stops the
     * countdown, resets all spinners to zero, clears the display to "00:00:00.000", and
     * resets the progress bar to zero.
     *
     * @see #cancelAnyActiveAlarmAndCleanup()
     * @see #countdownTimeDisplayComponent
     * @see #countdownProgressBarComponent
     */
    private void handleResetCountdownButtonPressed(){
        try{
            cancelAnyActiveAlarmAndCleanup();
            countdownIsRunningFlag=false;
            countdownIsPausedFlag=false;
            String stopText="Stop Countdown";
            countdownStopAndResumeButtonComponent.setText(stopText);
            String pauseTooltip="Pause the running countdown";
            countdownStopAndResumeButtonComponent.setToolTipText(pauseTooltip);
            countdownHoursSpinnerComponent.setValue(0);
            countdownMinutesSpinnerComponent.setValue(0);
            countdownSecondsSpinnerComponent.setValue(0);
            String zeroHoursColon="00:";
            String zeroMinutesColon="00:";
            String zeroSeconds="00";
            String zeroMillisecondsDot=".000";
            countdownTimeDisplayComponent.setTime(zeroHoursColon, zeroMinutesColon, zeroSeconds, zeroMillisecondsDot);
            countdownProgressBarComponent.setValue(0);
        }
        catch (Exception exceptionObject){
            showErrorDialog("Failed to reset countdown", exceptionObject);
        }
    }
    /**
     * Builds and adds the stopwatch panel to the main frame. The panel contains a control
     * area with Start/Stop and Reset buttons, and a custom time display. The buttons share
     * the same action listeners defined as inner classes.
     *
     * @see #createStyledTitledPanel(String)
     * @see #createStyledButton(String)
     * @see #handleToggleStopwatchStartStop(JButton)
     * @see #handleResetStopwatchToZero(JButton)
     */
    private void initializeStopwatchPanel(){
        JPanel stopwatchPanel=createStyledTitledPanel("Stopwatch");
        JPanel controlsPanel=new JPanel();
        applyDarkControlPanelStyle(controlsPanel);
        final JButton startStopButton=createStyledButton("Start");
        startStopButton.setToolTipText("Start or stop the stopwatch");
        JButton resetButton=createStyledButton("Reset");
        resetButton.setToolTipText("Reset the stopwatch to zero");
        startStopButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleToggleStopwatchStartStop(startStopButton);
            }
        });
        resetButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEventObject){
                handleResetStopwatchToZero(startStopButton);
            }
        });
        controlsPanel.add(startStopButton);
        controlsPanel.add(resetButton);
        stopwatchPanel.add(controlsPanel, BorderLayout.NORTH);
        stopwatchTimeDisplayComponent=new CustomTimeDisplayComponent();
        stopwatchTimeDisplayComponent.setBorder(DISPLAY_BOX_BORDER);
        stopwatchTimeDisplayComponent.setBackground(WHITE_BACKGROUND_COLOR);
        stopwatchTimeDisplayComponent.setTime("00:", "00:", "00", ".000");
        stopwatchPanel.add(stopwatchTimeDisplayComponent, BorderLayout.CENTER);
        add(stopwatchPanel);
    }
    /**
     * Toggles the stopwatch between running and paused. If the stopwatch is running, it
     * pauses it, updates the accumulated time, and changes the button text to "Start".
     * If it is paused, it resumes from the accumulated time and changes the button text
     * to "Stop".
     *
     * @param startStopButton the Start/Stop button whose text is updated to reflect the
     *                        current state.
     * @see #stopwatchIsRunningFlag
     * @see #stopwatchLastStartTimeInMilliseconds
     * @see #stopwatchAccumulatedTimeBeforePauseInMilliseconds
     */
    private void handleToggleStopwatchStartStop(JButton startStopButton){
        try{
            if (stopwatchIsRunningFlag){
                long currentTime=System.currentTimeMillis();
                long elapsed=currentTime-stopwatchLastStartTimeInMilliseconds;
                stopwatchAccumulatedTimeBeforePauseInMilliseconds=stopwatchAccumulatedTimeBeforePauseInMilliseconds+elapsed;
                stopwatchIsRunningFlag=false;
                String startText="Start";
                startStopButton.setText(startText);
            }
            else{
                stopwatchLastStartTimeInMilliseconds=System.currentTimeMillis();
                stopwatchIsRunningFlag=true;
                String stopText="Stop";
                startStopButton.setText(stopText);
            }
        }
        catch (Exception exceptionObject){
            showErrorDialog("Stopwatch toggle error", exceptionObject);
        }
    }
    /**
     * Resets the stopwatch to zero. This stops the stopwatch, clears the accumulated time,
     * sets the display to "00:00:00.000", and changes the button text to "Start".
     *
     * @param startStopButton the Start/Stop button whose text is set to "Start".
     * @see #stopwatchAccumulatedTimeBeforePauseInMilliseconds
     * @see #stopwatchIsRunningFlag
     */
    private void handleResetStopwatchToZero(JButton startStopButton){
        try{
            stopwatchIsRunningFlag=false;
            stopwatchAccumulatedTimeBeforePauseInMilliseconds=0;
            String zeroHoursColon="00:";
            String zeroMinutesColon="00:";
            String zeroSeconds="00";
            String zeroMillisecondsDot=".000";
            stopwatchTimeDisplayComponent.setTime(zeroHoursColon, zeroMinutesColon, zeroSeconds, zeroMillisecondsDot);
            String startText="Start";
            startStopButton.setText(startText);
        }
        catch (Exception exceptionObject){
            showErrorDialog("Stopwatch reset error", exceptionObject);
        }
    }
    /**
     * Builds and adds the live clock panel to the main frame. The panel contains a custom
     * time display and a label showing the current UTC offset. The display initially shows
     * "00:00:00.000" and is updated by the master timer.
     *
     * @see #createStyledTitledPanel(String)
     * @see #liveClockTimeDisplayComponent
     * @see #timezoneOffsetDisplayLabel
     */
    private void initializeLiveClockPanel(){
        JPanel clockPanel=createStyledTitledPanel("Current Time");
        JPanel boxPanel=new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        boxPanel.setBackground(WHITE_BACKGROUND_COLOR);
        boxPanel.setBorder(DISPLAY_BOX_BORDER);
        liveClockTimeDisplayComponent=new CustomTimeDisplayComponent();
        liveClockTimeDisplayComponent.setTime("00:", "00:", "00", ".000");
        boxPanel.add(liveClockTimeDisplayComponent);
        timezoneOffsetDisplayLabel=new JLabel("(UTC+0)");
        timezoneOffsetDisplayLabel.setFont(generalTextFont);
        boxPanel.add(timezoneOffsetDisplayLabel);
        clockPanel.add(boxPanel, BorderLayout.CENTER);
        add(clockPanel);
    }
    /**
     * Cancels any active alarm sequence and restores the user interface to its normal state.
     * This includes stopping the flash and shake timers, resetting the window title, restoring
     * the original window position, resetting the countdown display background to white,
     * setting the tray icon back to normal, closing the audio line, and interrupting the
     * sound playback thread.
     *
     * <p>The cancellation is triggered by setting the {@link #alarmCancellationRequestedFlag}
     * to {@code true}, which also prevents any new audio from being played.
     *
     * @see #alarmCancellationRequestedFlag
     * @see #alarmFlashTimer
     * @see #alarmShakeTimer
     * @see #alarmTitleResetTimer
     * @see #currentAudioOutputLine
     * @see #alarmSoundPlaybackThread
     */
    private void cancelAnyActiveAlarmAndCleanup(){
        alarmCancellationRequestedFlag=true;
        if (alarmFlashTimer!=null&&alarmFlashTimer.isRunning()){
            alarmFlashTimer.stop();
        }
        if (alarmShakeTimer!=null&&alarmShakeTimer.isRunning()){
            alarmShakeTimer.stop();
        }
        if (alarmTitleResetTimer!=null&&alarmTitleResetTimer.isRunning()){
            alarmTitleResetTimer.stop();
        }
        if (windowOriginalLocationBeforeShaking!=null){
            setLocation(windowOriginalLocationBeforeShaking);
            windowOriginalLocationBeforeShaking=null;
        }
        if (countdownTimeDisplayComponent!=null){
            countdownTimeDisplayComponent.setBackground(WHITE_BACKGROUND_COLOR);
        }
        String originalTitle="All-in-One Timer Tool";
        setTitle(originalTitle);
        if (systemTrayIcon!=null&&normalIconImage!=null){
            systemTrayIcon.setImage(normalIconImage);
        }
        if (currentAudioOutputLine!=null){
            if (currentAudioOutputLine.isOpen()){
                currentAudioOutputLine.close();
            }
            currentAudioOutputLine=null;
        }
        if (alarmSoundPlaybackThread!=null){
            if (alarmSoundPlaybackThread.isAlive()){
                alarmSoundPlaybackThread.interrupt();
            }
            alarmSoundPlaybackThread=null;
        }
        taskbarAttentionFlashCounter=0;
    }
    /**
     * Starts the full alarm sequence, which includes flashing the countdown background and
     * tray icon, shaking the window, playing audible tones, and temporarily changing the
     * window title to "Time Is Up". The sequence runs asynchronously using timers and a
     * separate thread for audio.
     *
     * <p>The alarm can be cancelled by calling {@link #cancelAnyActiveAlarmAndCleanup()}.
     *
     * @see #cancelAnyActiveAlarmAndCleanup()
     * @see #alarmFlashTimer
     * @see #alarmShakeTimer
     * @see #alarmTitleResetTimer
     * @see #alarmSoundPlaybackThread
     */
    private void triggerAlarmSequence(){
        cancelAnyActiveAlarmAndCleanup();
        alarmCancellationRequestedFlag=false;
        alarmFlashIsOnFlag=false;
        windowOriginalLocationBeforeShaking=getLocation();
        alarmShakeTickCounter=0;
        taskbarAttentionFlashCounter=0;
        if (alarmFlashTimer!=null){
            alarmFlashTimer.start();
        }
        if (alarmShakeTimer!=null){
            alarmShakeTimer.start();
        }
        alarmSoundPlaybackThread=new Thread(new Runnable(){
            public void run(){
                executeAlarmSoundSequenceAndCleanup();
            }
        });
        alarmSoundPlaybackThread.start();
        String alarmTitle="Time Is Up";
        setTitle(alarmTitle);
        if (alarmTitleResetTimer!=null){
            alarmTitleResetTimer.restart();
        }
    }
    /**
     * Handles each tick of the alarm flash timer (every 300 ms). It toggles the background
     * color of the countdown display between white and red, toggles the tray icon between
     * normal and red, and requests window focus every other tick to attract attention.
     *
     * @see #alarmFlashIsOnFlag
     * @see #countdownTimeDisplayComponent
     * @see #systemTrayIcon
     * @see #taskbarAttentionFlashCounter
     */
    private void handleAlarmFlashTimerTick(){
        if (countdownTimeDisplayComponent!=null){
            Color backgroundColor;
            if (alarmFlashIsOnFlag){
                backgroundColor=WHITE_BACKGROUND_COLOR;
            }
            else{
                backgroundColor=RED_ALERT_COLOR;
            }
            countdownTimeDisplayComponent.setBackground(backgroundColor);
        }
        if (systemTrayIcon!=null){
            Image iconToShow;
            if (alarmFlashIsOnFlag){
                iconToShow=normalIconImage;
            }
            else{
                iconToShow=redAlarmIconImage;
            }
            systemTrayIcon.setImage(iconToShow);
        }
        boolean shouldRequestFocus=(taskbarAttentionFlashCounter%2)==0;
        if (shouldRequestFocus){
            EventQueue.invokeLater(new Runnable(){
                public void run(){
                    AllInOneTimer.this.toFront();
                    AllInOneTimer.this.requestFocus();
                }
            });
        }
        taskbarAttentionFlashCounter=taskbarAttentionFlashCounter+1;
        alarmFlashIsOnFlag=!alarmFlashIsOnFlag;
    }
    /**
     * Handles each tick of the alarm shake timer (every 50 ms). It moves the window
     * horizontally by ±5 pixels relative to its original position for a total of 20 ticks
     * (1 second). After the 20th tick, the timer stops and the window is returned to its
     * original location.
     *
     * @see #windowOriginalLocationBeforeShaking
     * @see #alarmShakeTickCounter
     */
    private void handleAlarmShakeTimerTick(){
        boolean isEvenTick=(alarmShakeTickCounter%2)==0;
        int deltaX;
        if (isEvenTick){
            deltaX=5;
        }
        else{
            deltaX=-5;
        }
        int newX=windowOriginalLocationBeforeShaking.x+deltaX;
        int newY=windowOriginalLocationBeforeShaking.y;
        setLocation(newX, newY);
        alarmShakeTickCounter=alarmShakeTickCounter+1;
        if (alarmShakeTickCounter>=20){
            alarmShakeTimer.stop();
            setLocation(windowOriginalLocationBeforeShaking);
            windowOriginalLocationBeforeShaking=null;
        }
    }
    /**
     * Handles the expiry of the alarm title reset timer (5 seconds after the alarm starts).
     * It restores the original window title "All-in-One Timer Tool".
     *
     * @see #setTitle(String)
     */
    private void handleAlarmTitleResetTimerExpired(){
        String originalTitle="All-in-One Timer Tool";
        setTitle(originalTitle);
    }
    /**
     * Executes the alarm sound sequence on a background thread, and after the sequence
     * completes (or is interrupted), it stops the flash and shake timers, resets the
     * countdown display background to white, and restores the normal tray icon.
     *
     * <p>Any {@link InterruptedException} is silently swallowed, and other exceptions
     * trigger an audio warning dialog (shown once per session). The cleanup operations
     * are performed on the Event Dispatch Thread via {@link SwingUtilities#invokeLater}.
     *
     * @see #playAlertSoundSequenceOfTonePairs()
     * @see #alarmFlashTimer
     * @see #alarmShakeTimer
     * @see #showAudioWarningDialog(String)
     */
    private void executeAlarmSoundSequenceAndCleanup(){
        try{
            playAlertSoundSequenceOfTonePairs();
        }
        catch (InterruptedException ignoredInterruptedException){
        }
        catch (Exception exceptionObject){
            showAudioWarningDialog("Audio alert failed: "+exceptionObject.getMessage());
        }
        finally{
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    if (alarmFlashTimer!=null&&alarmFlashTimer.isRunning()){
                        alarmFlashTimer.stop();
                    }
                    if (alarmShakeTimer!=null&&alarmShakeTimer.isRunning()){
                        alarmShakeTimer.stop();
                    }
                    if (countdownTimeDisplayComponent!=null){
                        countdownTimeDisplayComponent.setBackground(WHITE_BACKGROUND_COLOR);
                    }
                    if (systemTrayIcon!=null&&normalIconImage!=null){
                        systemTrayIcon.setImage(normalIconImage);
                    }
                    taskbarAttentionFlashCounter=0;
                }
            });
        }
    }
    /**
     * Plays a sequence of 15 tone pairs (420 Hz square wave for 100 ms, then 840 Hz sine
     * wave for 100 ms) with 100 ms silence between pairs. The sequence is interruptible
     * via the {@link #alarmCancellationRequestedFlag} or thread interruption.
     *
     * @throws InterruptedException if the thread is interrupted while sleeping between
     *                              tone pairs.
     * @see #playSingleToneSafe(int, int, float, boolean, float)
     * @see #alarmCancellationRequestedFlag
     */
    private void playAlertSoundSequenceOfTonePairs() throws InterruptedException{
        float audioSampleRate=44100f;
        if (alarmCancellationRequestedFlag){
            return;
        }
        for (int tonePairIndex=0;tonePairIndex<15;tonePairIndex++){
            if (alarmCancellationRequestedFlag||Thread.currentThread().isInterrupted()){
                return;
            }
            playSingleToneSafe(420, 100, audioSampleRate, true, 0.3f);
            if (alarmCancellationRequestedFlag||Thread.currentThread().isInterrupted()){
                return;
            }
            playSingleToneSafe(840, 100, audioSampleRate, false, 0.1f);
            if (alarmCancellationRequestedFlag||Thread.currentThread().isInterrupted()){
                return;
            }
            Thread.sleep(100);
            if (alarmCancellationRequestedFlag||Thread.currentThread().isInterrupted()){
                return;
            }
        }
    }
    /**
     * A safe wrapper around {@link #playSingleToneSynthesized} that catches and silently
     * ignores any exceptions. This prevents individual tone failures from disrupting the
     * entire alarm sequence.
     *
     * @param frequencyHz    the frequency of the tone in Hertz.
     * @param durationMillis the duration of the tone in milliseconds.
     * @param sampleRateHz   the audio sample rate in Hertz (must match the system's
     *                       supported rates; 44.1 kHz is typical).
     * @param useSquareWave  if {@code true}, a square wave is generated; otherwise, a
     *                       sine wave is generated.
     * @param gainAmplitude  the amplitude of the tone (0.0 to 1.0).
     * @see #playSingleToneSynthesized(int, int, float, boolean, float)
     */
    private void playSingleToneSafe(int frequencyHz, int durationMillis, float sampleRateHz, boolean useSquareWave, float gainAmplitude){
        try{
            playSingleToneSynthesized(frequencyHz, durationMillis, sampleRateHz, useSquareWave, gainAmplitude);
        }
        catch (Exception ignoredException){
        }
    }
    /**
     * Synthesizes a single tone of the specified frequency, duration, and waveform type
     * (square or sine) and plays it through the system's audio output. The tone is generated
     * as 8‑bit mono PCM samples and written to a {@link SourceDataLine}. The method handles
     * opening, writing, and closing the audio line.
     *
     * <p>The tone buffer is reused (via {@link #audioToneSynthesisBuffer}) and resized if
     * the requested duration exceeds its current capacity.
     *
     * @param frequencyHz    the frequency of the tone in Hertz.
     * @param durationMillis the duration of the tone in milliseconds.
     * @param sampleRateHz   the audio sample rate in Hertz (e.g., 44100).
     * @param useSquareWave  if {@code true}, a square wave is generated; otherwise, a
     *                       sine wave is generated.
     * @param gainAmplitude  the amplitude of the tone (0.0 to 1.0); this scales the sample
     *                       values.
     * @throws Exception if the audio line cannot be opened or if writing fails.
     * @see AudioFormat
     * @see SourceDataLine
     * @see #audioToneSynthesisBuffer
     * @see #currentAudioOutputLine
     */
    private void playSingleToneSynthesized(int frequencyHz, int durationMillis, float sampleRateHz, boolean useSquareWave, float gainAmplitude) throws Exception{
        int numberOfSamples=(int)(sampleRateHz*durationMillis/1000);
        if (audioToneSynthesisBuffer.length<numberOfSamples){
            audioToneSynthesisBuffer=new byte[numberOfSamples];
        }
        byte[] bufferReference=audioToneSynthesisBuffer;
        for (int sampleIndex=0;sampleIndex<numberOfSamples;sampleIndex++){
            double angleRadians=2*Math.PI*sampleIndex*frequencyHz/sampleRateHz;
            double waveformValue;
            if (useSquareWave){
                waveformValue=Math.signum(Math.sin(angleRadians));
            }
            else{
                waveformValue=Math.sin(angleRadians);
            }
            int byteValue=(int)(waveformValue*gainAmplitude*127);
            bufferReference[sampleIndex]=(byte)byteValue;
        }
        AudioFormat audioFormatObject=new AudioFormat(sampleRateHz, 8, 1, true, false);
        SourceDataLine audioOutputLine=AudioSystem.getSourceDataLine(audioFormatObject);
        currentAudioOutputLine=audioOutputLine;
        try{
            audioOutputLine.open(audioFormatObject);
            audioOutputLine.start();
            audioOutputLine.write(bufferReference, 0, numberOfSamples);
            audioOutputLine.drain();
        }
        finally{
            audioOutputLine.close();
            currentAudioOutputLine=null;
        }
    }
    /**
     * Pads an integer with leading zeros to achieve the specified total length. This is used
     * for formatting numbers that exceed the precomputed range (e.g., hours ≥ 100).
     *
     * @param integerValue   the integer to pad.
     * @param desiredLength  the desired length of the resulting string (must be ≥ 1).
     * @return a string representation of the integer with at least {@code desiredLength}
     *         characters, padded with leading zeros as needed.
     */
    private static String padIntegerWithLeadingZeros(int integerValue, int desiredLength){
        String integerAsString=Integer.toString(integerValue);
        StringBuilder stringBuilderForPadding=new StringBuilder(desiredLength);
        while (true){
            int currentBuilderLength=stringBuilderForPadding.length();
            int stringLength=integerAsString.length();
            int combinedLength=currentBuilderLength+stringLength;
            if (combinedLength<desiredLength){
                stringBuilderForPadding.append('0');
            }
            else{
                break;
            }
        }
        stringBuilderForPadding.append(integerAsString);
        return stringBuilderForPadding.toString();
    }
    /**
     * Creates a styled {@link JSpinner} with the specified range and a fixed preferred size.
     * The spinner uses a {@link SpinnerNumberModel} and applies the shared border and font.
     *
     * @param initialValue the initial value of the spinner.
     * @param minimumValue the minimum allowed value (inclusive).
     * @param maximumValue the maximum allowed value (inclusive).
     * @return a fully styled {@code JSpinner} component.
     * @see #SPINNER_COMPONENT_BORDER
     * @see #generalTextFont
     */
    private JSpinner createStyledSpinner(int initialValue, int minimumValue, int maximumValue){
        SpinnerNumberModel spinnerModel=new SpinnerNumberModel(initialValue, minimumValue, maximumValue, 1);
        JSpinner spinnerComponent=new JSpinner(spinnerModel);
        Dimension preferredSize=new Dimension(50, 24);
        spinnerComponent.setPreferredSize(preferredSize);
        spinnerComponent.setFont(generalTextFont);
        spinnerComponent.setBorder(SPINNER_COMPONENT_BORDER);
        return spinnerComponent;
    }
    /**
     * Creates a styled {@link JButton} with a red background, white text, and hover/press
     * effects managed by the shared {@link MouseAdapter}. The button also uses the hand cursor
     * and the shared border.
     *
     * @param buttonText the text to display on the button.
     * @return a fully styled {@code JButton}.
     * @see #SHARED_BUTTON_HOVER_PRESS_LISTENER
     * @see #BUTTON_COMPONENT_BORDER
     * @see #buttonTextFont
     * @see #RED_BUTTON_BACKGROUND_COLOR
     */
    private JButton createStyledButton(String buttonText){
        JButton buttonComponent=new JButton(buttonText);
        buttonComponent.setFont(buttonTextFont);
        buttonComponent.setBackground(RED_BUTTON_BACKGROUND_COLOR);
        buttonComponent.setForeground(WHITE_BUTTON_TEXT_COLOR);
        buttonComponent.setBorder(BUTTON_COMPONENT_BORDER);
        buttonComponent.setFocusPainted(false);
        buttonComponent.setCursor(HAND_CURSOR_FOR_INTERACTIVE_COMPONENTS);
        buttonComponent.putClientProperty("buttonOriginalBackgroundColor", RED_BUTTON_BACKGROUND_COLOR);
        buttonComponent.putClientProperty("buttonHoverBackgroundColor", RED_BUTTON_BACKGROUND_COLOR.darker());
        buttonComponent.putClientProperty("buttonPressedBackgroundColor", RED_BUTTON_BACKGROUND_COLOR.brighter());
        buttonComponent.addMouseListener(SHARED_BUTTON_HOVER_PRESS_LISTENER);
        return buttonComponent;
    }
    /**
     * Creates a titled panel with a white background and a {@link BorderLayout}. The title
     * is centered at the top, and the panel has an empty border of 5 pixels on all sides.
     *
     * @param titleString the title to display in the border.
     * @return a new {@code JPanel} with the specified titled border and white background.
     * @see BorderFactory#createTitledBorder(Border, String, int, int, Font, Color)
     */
    private JPanel createStyledTitledPanel(String titleString){
        JPanel titledPanel=new JPanel(new BorderLayout());
        Border emptyBorder=BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border titledBorder=BorderFactory.createTitledBorder(emptyBorder, titleString, JLabel.CENTER, JLabel.TOP, buttonTextFont, Color.BLACK);
        titledPanel.setBorder(titledBorder);
        titledPanel.setBackground(WHITE_BACKGROUND_COLOR);
        titledPanel.setOpaque(true);
        return titledPanel;
    }
    /**
     * Applies the dark control panel style to the given panel: sets the background to
     * {@link #DARK_CONTROL_PANEL_COLOR} and adds an empty border of 5 pixels.
     *
     * @param panelToStyle the panel to style; must not be {@code null}.
     */
    private void applyDarkControlPanelStyle(JPanel panelToStyle){
        panelToStyle.setBackground(DARK_CONTROL_PANEL_COLOR);
        panelToStyle.setOpaque(true);
        Border emptyPaddingBorder=BorderFactory.createEmptyBorder(5, 5, 5, 5);
        panelToStyle.setBorder(emptyPaddingBorder);
    }
    /**
     * Shows an error dialog with the given message and the stack trace of the exception.
     * The dialog is modal and blocks the current thread until dismissed.
     *
     * @param errorMessage   a human‑readable description of the error.
     * @param exceptionObject the exception that caused the error; its message and stack
     *                       trace are printed.
     * @see JOptionPane#showMessageDialog(java.awt.Component, Object, String, int)
     */
    private void showErrorDialog(String errorMessage, Exception exceptionObject){
        String fullMessage=errorMessage+": "+exceptionObject.getMessage();
        JOptionPane.showMessageDialog(this, fullMessage, "Error", JOptionPane.ERROR_MESSAGE);
        exceptionObject.printStackTrace();
    }
    /**
     * Shows an audio warning dialog with the given message, but only once per application
     * session to avoid spamming the user. The dialog is displayed on the Event Dispatch
     * Thread via {@link SwingUtilities#invokeLater}.
     *
     * @param warningMessage the warning message to display.
     * @see #audioWarningDialogHasBeenShown
     */
    private void showAudioWarningDialog(String warningMessage){
        if (!audioWarningDialogHasBeenShown){
            audioWarningDialogHasBeenShown=true;
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    JOptionPane.showMessageDialog(AllInOneTimer.this, warningMessage, "Audio Warning", JOptionPane.WARNING_MESSAGE);
                }
            });
        }
    }
    /**
     * A custom lightweight component that paints a time string consisting of four segments:
     * hours (with colon), minutes (with colon), seconds, and milliseconds (with dot).
     * The component is opaque and uses the font set at the time of painting.
     *
     * <p>The time segments are stored as pre‑formatted strings to avoid runtime concatenation.
     * The component overrides {@link #paintComponent(Graphics)} to render the text centered
     * both horizontally and vertically.
     *
     * @see #setTime(String, String, String, String)
     * @see #paintComponent(Graphics)
     */
    static class CustomTimeDisplayComponent extends JComponent{
        private String hoursSegmentWithColon="00:";
        private String minutesSegmentWithColon="00:";
        private String secondsSegment="00";
        private String millisecondsSegmentWithDot=".000";
        /**
         * Constructs a new time display component with default background (white) and
         * the globally defined font for large time labels.
         */
        CustomTimeDisplayComponent(){
            setOpaque(true);
            setBackground(Color.WHITE);
            setFont(largeTimeLabelFont);
        }
        /**
         * Sets the four time segments and triggers a repaint. The segments should be
         * pre‑formatted with colons/dots as needed.
         *
         * @param hoursColonString   the hours part, e.g., "12:" (including colon).
         * @param minutesColonString the minutes part, e.g., "34:" (including colon).
         * @param secondsString      the seconds part, e.g., "56".
         * @param millisecondsDotString the milliseconds part, e.g., ".789".
         */
        void setTime(String hoursColonString, String minutesColonString, String secondsString, String millisecondsDotString){
            this.hoursSegmentWithColon=hoursColonString;
            this.minutesSegmentWithColon=minutesColonString;
            this.secondsSegment=secondsString;
            this.millisecondsSegmentWithDot=millisecondsDotString;
            repaint();
        }
        /**
         * Paints the background and the time text centered within the component's bounds.
         * The background is filled with the current background color, then the text is
         * drawn in black using the current font. The total width of the four segments
         * is computed to achieve centering.
         *
         * @param graphicsContext the {@link Graphics} context used for painting.
         */
        protected void paintComponent(Graphics graphicsContext){
            super.paintComponent(graphicsContext);
            Color currentBackgroundColor=getBackground();
            graphicsContext.setColor(currentBackgroundColor);
            int componentWidth=getWidth();
            int componentHeight=getHeight();
            graphicsContext.fillRect(0, 0, componentWidth, componentHeight);
            graphicsContext.setColor(Color.BLACK);
            Font currentFont=getFont();
            graphicsContext.setFont(currentFont);
            FontMetrics fontMetricsObject=graphicsContext.getFontMetrics();
            int hoursWidth=fontMetricsObject.stringWidth(hoursSegmentWithColon);
            int minutesWidth=fontMetricsObject.stringWidth(minutesSegmentWithColon);
            int secondsWidth=fontMetricsObject.stringWidth(secondsSegment);
            int millisecondsWidth=fontMetricsObject.stringWidth(millisecondsSegmentWithDot);
            int totalTextWidth=hoursWidth+minutesWidth+secondsWidth+millisecondsWidth;
            int xCoordinate=(componentWidth-totalTextWidth)/2;
            int ascent=fontMetricsObject.getAscent();
            int descent=fontMetricsObject.getDescent();
            int yCoordinate=(componentHeight+ascent-descent)/2;
            graphicsContext.drawString(hoursSegmentWithColon, xCoordinate, yCoordinate);
            xCoordinate=xCoordinate+hoursWidth;
            graphicsContext.drawString(minutesSegmentWithColon, xCoordinate, yCoordinate);
            xCoordinate=xCoordinate+minutesWidth;
            graphicsContext.drawString(secondsSegment, xCoordinate, yCoordinate);
            xCoordinate=xCoordinate+secondsWidth;
            graphicsContext.drawString(millisecondsSegmentWithDot, xCoordinate, yCoordinate);
        }
        /**
         * Returns the preferred size of this component, which is the width of the widest
         * possible time string ("00:00:00.000") plus a padding of 20 pixels on each side,
         * and the font height plus 20 pixels vertically.
         *
         * @return a {@link Dimension} representing the preferred size.
         */
        public Dimension getPreferredSize(){
            FontMetrics fontMetricsObject=getFontMetrics(getFont());
            String widestTimeString="00:00:00.000";
            int textWidth=fontMetricsObject.stringWidth(widestTimeString);
            int preferredWidth=textWidth+20;
            int fontHeight=fontMetricsObject.getHeight();
            int preferredHeight=fontHeight+20;
            Dimension preferredSize=new Dimension(preferredWidth, preferredHeight);
            return preferredSize;
        }
    }
    /**
     * The application entry point. It schedules the construction of the main GUI on the
     * Event Dispatch Thread (EDT) using {@link SwingUtilities#invokeLater} to ensure
     * thread safety.
     *
     * @param args command line arguments (not used in this application).
     * @see SwingUtilities#invokeLater(Runnable)
     */
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                new AllInOneTimer();
            }
        });
    }
}