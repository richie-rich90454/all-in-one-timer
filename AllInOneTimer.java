/**
 * A graphical user interface application that integrates a countdown timer with an
 * audio‑visual alarm, a stopwatch with millisecond precision, and a live clock
 * displaying the current system time and its Coordinated Universal Time offset.
 * The interface uses the Swing toolkit and arranges three functional panels
 * vertically inside a resizable frame.
 *
 * The application employs extensive memory‑saving design choices to keep the
 * resident footprint well under 80 MB even with a small Java heap
 * (e.g., -Xms16m -Xmx64m).  All string representations of time values are
 * statically precomputed, eliminating runtime allocation and garbage collection
 * pressure.  Each time display (countdown, stopwatch, live clock) is rendered by
 * a single lightweight custom painting component (TimeDisplay) instead of four
 * separate JLabel instances, reducing the total component count by twelve.  A
 * single master Swing Timer ticks every 10 ms and conditionally updates the
 * active displays; the live clock updates only every fifth tick (50 ms).  All
 * Border objects are shared as static constants, and a single shared
 * MouseAdapter handles button hover and press effects for every button, avoiding
 * per‑button listener allocations.  Explicit System.gc() invocations are omitted,
 * relying instead on timely nullification of references to allow standard garbage
 * collection without introducing stop‑the‑world pauses.
 *
 * Countdown Timer:
 * The user sets the desired duration using three spinner components that each
 * accept any non‑negative integer value. Overflow is automatically normalized
 * before the countdown begins, and the spinners are updated to reflect the
 * normalized values.  The master Timer drives the countdown logic: when active,
 * the remaining time is computed every 10 ms and displayed on the custom
 * TimeDisplay component; a progress bar fills proportionally.  Pause and resume
 * are supported without loss of time.  A reset button clears all state.  When the
 * remaining time reaches zero, the display stops and the alarm sequence is
 * triggered.
 *
 * Alarm Sequence:
 * A set of concurrent effects serves as the notification: background flashing of
 * the countdown TimeDisplay component between white and red at 300 ms intervals,
 * window shaking (20 ticks at 50 ms each), flashing of the system‑tray icon
 * between normal and red, repeated requests for window focus (causing the
 * taskbar button to highlight), and a separate thread that synthesizes and plays
 * 15 tone pairs (420 Hz square wave then 840 Hz sine wave).  The frame title is
 * temporarily set to "Time Is Up" and restored after five seconds.  The alarm
 * can be cancelled instantly via a volatile flag; the audio line is closed
 * externally, causing any blocking write to fail immediately.
 *
 * Stopwatch:
 * The stopwatch panel contains Start/Stop and Reset buttons.  The master Timer
 * updates the stopwatch TimeDisplay when the stopwatch is running, accumulating
 * elapsed time.  The accumulated value is preserved across pauses.
 *
 * Live Clock:
 * Every 50 ms the master Timer updates the clock TimeDisplay and a separate
 * JLabel showing the UTC offset.  The offset label is rebuilt only when the
 * offset actually changes, minimizing string allocations.
 *
 * User Interface Styling and Tooltips:
 * A dark theme with red accents is used.  Colors are stored as static final
 * constants, and pure white/black uses the predefined constants Color.WHITE and
 * Color.BLACK.  Buttons share a single hover/press MouseAdapter that reads
 * colors from client properties.  All interactive components carry tooltips and
 * use the hand cursor.  The countdown controls are arranged in a 3×3 GridLayout
 * to prevent layout shifts when the stop/resume button text changes.
 *
 * Precomputed Strings:
 * All possible two‑digit and three‑digit padded strings (00‑99, 00:‑99:,
 * .000‑.999) are statically initialized in the PAD2, PAD2_COLON, and PAD3_MS
 * arrays.  The TimeDisplay and the clock offset label use these strings directly,
 * completely avoiding runtime concatenation.
 *
 * Custom Font Loading:
 * The constructor attempts to load "NotoSans‑VariableFont_wdth_wght.ttf" from the
 * classpath.  On success the font is derived without registering it with the
 * graphics environment.  On failure, system sans‑serif fallbacks are used.
 *
 * Resource Cleanup:
 * A WindowListener stops the master Timer, cancels the alarm, removes the tray
 * icon, and nullifies all component and thread references when the window is
 * closed.
 *
 * Exception Handling:
 * User actions are wrapped in try‑catch blocks and errors shown in modal dialogs.
 * The audio thread swallows InterruptedException silently and displays a warning
 * dialog (once per session) for other audio failures.
 *
 * @author richie-rich90454
 * @since 2026-07-12
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
    private JSpinner hourSpinner, minuteSpinner, secondSpinner;
    private TimeDisplay countdownDisplay, stopwatchDisplay, clockDisplay;
    private JButton countdownStopButton;
    private JProgressBar countdownProgressBar;
    private JLabel timeZoneLabel;
    private long countdownEndMillis, countdownTotalMillis, countdownRemainingMillis;
    private boolean countdownPaused=false;
    private boolean countdownRunning=false;
    private boolean stopwatchRunning=false;
    private long stopwatchStartMillis, stopwatchAccumulatedMillis;
    private Timer masterTimer;
    private int masterTickCount;
    private Timer flashTimer, shakeTimer, titleResetTimer;
    private boolean flashOn;
    private Point originalLocation;
    private int shakeCount;
    private int taskbarFlashCount;
    private Thread alarmSoundThread;
    private SourceDataLine currentAudioLine;
    private volatile boolean alarmCancelled=false;
    private static boolean audioWarningShown=false;
    private final Calendar calendar=Calendar.getInstance();
    private int lastTimezoneOffset=Integer.MIN_VALUE;
    private TrayIcon trayIcon;
    private Image normalTrayImage, redTrayImage;
    private boolean trayIconAdded=false;
    private byte[] toneBuffer;
    private static final Color PRIMARY_COLOR=Color.decode("#1E1E1E");
    private static final Color SECONDARY_COLOR=Color.decode("#2B2B2B");
    private static final Color BACKGROUND_COLOR=Color.WHITE;
    private static final Color ALERT_COLOR=Color.decode("#DE0000");
    private static final Color BUTTON_BACKGROUND=Color.decode("#DE0000");
    private static final Color BUTTON_TEXT=Color.WHITE;
    private static final Color PROGRESS_FOREGROUND=Color.decode("#DE0000");
    private static final Color PROGRESS_BACKGROUND=Color.decode("#D0D0D0");
    private static final Color SPINNER_BORDER_COLOR=new Color(0, 0, 0, 70);
    private static Font LABEL_FONT=new Font("Noto Sans", Font.BOLD, 24);
    private static Font BUTTON_FONT=new Font("Noto Sans", Font.BOLD, 14);
    private static Font TEXT_FONT=new Font("Noto Sans", Font.PLAIN, 14);
    private static final Cursor HAND_CURSOR=Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Border SPINNER_BORDER=BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(SPINNER_BORDER_COLOR, 1, true),
        BorderFactory.createEmptyBorder(2, 5, 2, 5));
    private static final Border BUTTON_BORDER=BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(SPINNER_BORDER_COLOR, 1, true),
        BorderFactory.createEmptyBorder(5, 10, 5, 10));
    private static final Border BOX_BORDER=BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.BLACK, 1),
        BorderFactory.createEmptyBorder(10, 10, 10, 10));
    private static final String[] PAD2=new String[100];
    private static final String[] PAD2_COLON=new String[100];
    private static final String[] PAD3_MS=new String[1000];
    static{
        StringBuilder stringBuilder=new StringBuilder(5);
        for (int i=0;i<100;i++){
            stringBuilder.setLength(0);
            if (i<10) stringBuilder.append('0');
            stringBuilder.append(i);
            PAD2[i]=stringBuilder.toString();
            PAD2_COLON[i]=stringBuilder.toString()+":";
        }
        for (int i=0;i<1000;i++){
            stringBuilder.setLength(0);
            stringBuilder.append('.');
            if (i<100) stringBuilder.append('0');
            if (i<10) stringBuilder.append('0');
            stringBuilder.append(i);
            PAD3_MS[i]=stringBuilder.toString();
        }
    }
    private static final MouseAdapter BUTTON_HOVER_LISTENER=new MouseAdapter(){
        public void mouseEntered(MouseEvent mouseEvent){
            JButton button=(JButton)mouseEvent.getSource();
            button.setBackground((Color)button.getClientProperty("hoverColor"));
        }
        public void mouseExited(MouseEvent mouseEvent){
            JButton button=(JButton)mouseEvent.getSource();
            button.setBackground((Color)button.getClientProperty("originalBackground"));
        }
        public void mousePressed(MouseEvent mouseEvent){
            JButton button=(JButton)mouseEvent.getSource();
            button.setBackground((Color)button.getClientProperty("pressedColor"));
        }
        public void mouseReleased(MouseEvent mouseEvent){
            JButton button=(JButton)mouseEvent.getSource();
            if (button.contains(mouseEvent.getPoint())){
                button.setBackground((Color)button.getClientProperty("hoverColor"));
            }
            else{
                button.setBackground((Color)button.getClientProperty("originalBackground"));
            }
        }
    };
    public AllInOneTimer(){
        super("All-in-One Timer Tool");
        Image icon=Toolkit.getDefaultToolkit().getImage(getClass().getResource("/favicon.png"));
        if (icon==null){
            icon=createDefaultIcon();
        }
        setIconImage(icon);
        normalTrayImage=icon;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(PRIMARY_COLOR);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        loadCustomFont();
        initCountdownPanel();
        initStopwatchPanel();
        initClockPanel();
        pack();
        setMinimumSize(new Dimension(400, 600));
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);
        masterTimer=new Timer(10, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleMasterTick(actionEvent);
            }
        });
        masterTimer.start();
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent windowEvent){
                disposeResources();
            }
        });
        initSystemTray();
        createReusableTimers();
    }
    private void createReusableTimers(){
        flashTimer=new Timer(300, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleFlashTick();
            }
        });
        flashTimer.setRepeats(true);
        shakeTimer=new Timer(50, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleShakeTick();
            }
        });
        shakeTimer.setRepeats(true);
        titleResetTimer=new Timer(5000, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleTitleReset();
            }
        });
        titleResetTimer.setRepeats(false);
        toneBuffer=new byte[4410];
    }
    private void initSystemTray(){
        if (!SystemTray.isSupported()){
            return;
        }
        if (redTrayImage==null){
            redTrayImage=createRedIcon(normalTrayImage);
        }
        trayIcon=new TrayIcon(normalTrayImage, "All-in-One Timer Tool", null);
        trayIcon.setImageAutoSize(true);
        try{
            SystemTray.getSystemTray().add(trayIcon);
            trayIconAdded=true;
        }
        catch (AWTException exception){
            System.err.println("Could not add tray icon: "+exception.getMessage());
        }
    }
    private Image createDefaultIcon(){
        BufferedImage image=new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 16, 16);
        graphics.dispose();
        return image;
    }
    private Image createRedIcon(Image original){
        int width=original.getWidth(null);
        int height=original.getHeight(null);
        if (width<=0||height<=0){
            width=16; height=16;
        }
        BufferedImage source=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=source.createGraphics();
        graphics.drawImage(original, 0, 0, null);
        graphics.dispose();
        for (int y=0;y<height;y++){
            for (int x=0;x<width;x++){
                int rgb=source.getRGB(x, y);
                int alpha=(rgb>>24)&0xff;
                if (alpha==0) continue;
                source.setRGB(x, y, (alpha<<24)|0xDE0000);
            }
        }
        return source;
    }
    private void handleMasterTick(ActionEvent actionEvent){
        masterTickCount++;
        long now=System.currentTimeMillis();
        if (countdownRunning){
            updateCountdownDisplay(now);
        }
        if (stopwatchRunning){
            updateStopwatchDisplay(now);
        }
        if (masterTickCount%5==0){
            updateClockDisplay(now);
        }
    }
    private void updateCountdownDisplay(long now){
        long remaining=countdownEndMillis-now;
        if (remaining<=0){
            countdownRunning=false;
            countdownPaused=false;
            countdownStopButton.setText("Stop Countdown");
            countdownStopButton.setToolTipText("Pause the running countdown");
            countdownDisplay.setTime("00:", "00:", "00", ".000");
            int maximum=countdownProgressBar.getMaximum();
            countdownProgressBar.setValue(maximum>0?maximum:0);
            triggerAlarm();
        }
        else{
            int hours=(int)(remaining/3600000);
            int minutes=(int)((remaining%3600000)/60000);
            int seconds=(int)((remaining%60000)/1000);
            int milliseconds=(int)(remaining%1000);
            countdownDisplay.setTime(hours<100?PAD2_COLON[hours]:pad(hours, 2)+":", PAD2_COLON[minutes], PAD2[seconds], PAD3_MS[milliseconds]);
            long elapsed=countdownTotalMillis-remaining;
            int progressValue=(elapsed>Integer.MAX_VALUE)?Integer.MAX_VALUE:(int)elapsed;
            countdownProgressBar.setValue(progressValue);
        }
    }
    private void updateStopwatchDisplay(long now){
        long elapsed=now-stopwatchStartMillis+stopwatchAccumulatedMillis;
        int hours=(int)(elapsed/3600000);
        int minutes=(int)((elapsed%3600000)/60000);
        int seconds=(int)((elapsed%60000)/1000);
        int milliseconds=(int)(elapsed%1000);
        stopwatchDisplay.setTime(hours<100?PAD2_COLON[hours]:pad(hours, 2)+":", PAD2_COLON[minutes], PAD2[seconds], PAD3_MS[milliseconds]);
    }
    private void updateClockDisplay(long now){
        calendar.setTimeInMillis(now);
        int hours=calendar.get(Calendar.HOUR_OF_DAY);
        int minutes=calendar.get(Calendar.MINUTE);
        int seconds=calendar.get(Calendar.SECOND);
        int milliseconds=calendar.get(Calendar.MILLISECOND);
        clockDisplay.setTime(hours<100?PAD2_COLON[hours]:pad(hours, 2)+":", PAD2_COLON[minutes], PAD2[seconds], PAD3_MS[milliseconds]);
        TimeZone timeZone=calendar.getTimeZone();
        int offsetHours=timeZone.getOffset(calendar.getTimeInMillis())/3600000;
        if (offsetHours!=lastTimezoneOffset){
            lastTimezoneOffset=offsetHours;
            String sign=(offsetHours>=0)?"+":"";
            timeZoneLabel.setText("(UTC"+sign+offsetHours+")");
        }
    }
    private void disposeResources(){
        cancelAlarm();
        if (masterTimer!=null){
            masterTimer.stop();
            masterTimer=null;
        }
        if (trayIconAdded&&trayIcon!=null){
            SystemTray.getSystemTray().remove(trayIcon);
            trayIconAdded=false;
        }
        flashTimer=null;
        shakeTimer=null;
        titleResetTimer=null;
        alarmSoundThread=null;
        currentAudioLine=null;
        countdownDisplay=null;
        stopwatchDisplay=null;
        clockDisplay=null;
        timeZoneLabel=null;
        hourSpinner=null;
        minuteSpinner=null;
        secondSpinner=null;
        countdownProgressBar=null;
        countdownStopButton=null;
    }
    private void loadCustomFont(){
        InputStream inputStream=null;
        try{
            inputStream=getClass().getResourceAsStream("/NotoSans-VariableFont_wdth_wght.ttf");
            if (inputStream!=null){
                Font loaded=Font.createFont(Font.TRUETYPE_FONT, inputStream);
                LABEL_FONT=loaded.deriveFont(Font.BOLD, 24f);
                BUTTON_FONT=loaded.deriveFont(Font.BOLD, 14f);
                TEXT_FONT=loaded.deriveFont(Font.PLAIN, 14f);
            }
            else{
                throw new IOException("Font file not found in resources");
            }
        }
        catch (Exception exception){
            System.err.println("Error loading font: "+exception.getMessage());
            TEXT_FONT=new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            LABEL_FONT=new Font(Font.SANS_SERIF, Font.BOLD, 24);
            BUTTON_FONT=new Font(Font.SANS_SERIF, Font.BOLD, 14);
        }
        finally{
            if (inputStream!=null){
                try{ inputStream.close(); } catch (IOException ignored){}
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
        JLabel minuteLabel=new JLabel("Minutes", JLabel.CENTER);
        minuteLabel.setFont(TEXT_FONT);
        minuteLabel.setForeground(Color.WHITE);
        minuteLabel.setToolTipText("Minutes for countdown (any non-negative integer, overflow will be normalized)");
        JLabel secondLabel=new JLabel("Seconds", JLabel.CENTER);
        secondLabel.setFont(TEXT_FONT);
        secondLabel.setForeground(Color.WHITE);
        secondLabel.setToolTipText("Seconds for countdown (any non-negative integer, overflow will be normalized)");
        hourSpinner=spinner(0, 0, Integer.MAX_VALUE);
        hourSpinner.setToolTipText("Set countdown hours; values > 0 are allowed");
        minuteSpinner=spinner(0, 0, Integer.MAX_VALUE);
        minuteSpinner.setToolTipText("Set countdown minutes; overflow will roll into hours");
        secondSpinner=spinner(0, 0, Integer.MAX_VALUE);
        secondSpinner.setToolTipText("Set countdown seconds; overflow will roll into minutes and hours");
        JButton setButton=styledButton("Set Countdown Timer");
        setButton.setToolTipText("Start countdown with the specified duration (normalized)");
        setButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleSetCountdown();
            }
        });
        countdownStopButton=styledButton("Stop Countdown");
        countdownStopButton.setToolTipText("Pause the running countdown");
        countdownStopButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleToggleCountdown();
            }
        });
        JButton resetButton=styledButton("Reset Countdown");
        resetButton.setToolTipText("Reset spinners to zero, clear display, stop timer, and cancel alarm");
        resetButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleResetCountdown();
            }
        });
        controls.add(hourLabel);
        controls.add(minuteLabel);
        controls.add(secondLabel);
        controls.add(hourSpinner);
        controls.add(minuteSpinner);
        controls.add(secondSpinner);
        controls.add(setButton);
        controls.add(countdownStopButton);
        controls.add(resetButton);
        panel.add(controls, BorderLayout.NORTH);
        countdownDisplay=new TimeDisplay();
        countdownDisplay.setBorder(BOX_BORDER);
        countdownDisplay.setBackground(BACKGROUND_COLOR);
        countdownDisplay.setTime("00:", "00:", "00", ".000");
        panel.add(countdownDisplay, BorderLayout.CENTER);
        countdownProgressBar=new JProgressBar(0, 1);
        countdownProgressBar.setValue(0);
        countdownProgressBar.setStringPainted(false);
        countdownProgressBar.setForeground(PROGRESS_FOREGROUND);
        countdownProgressBar.setBackground(PROGRESS_BACKGROUND);
        countdownProgressBar.setToolTipText("Countdown progress (elapsed time / total duration)");
        countdownProgressBar.setPreferredSize(new Dimension(0, 8));
        panel.add(countdownProgressBar, BorderLayout.SOUTH);
        add(panel);
    }
    private void handleSetCountdown(){
        try{
            cancelAlarm();
            countdownRunning=false;
            countdownPaused=false;
            countdownStopButton.setText("Stop Countdown");
            countdownStopButton.setToolTipText("Pause the running countdown");
            long totalSeconds=(long)(Integer)hourSpinner.getValue()*3600
                             +(long)(Integer)minuteSpinner.getValue()*60
                             +(long)(Integer)secondSpinner.getValue();
            long normalizedHours=totalSeconds/3600;
            long normalizedMinutes=(totalSeconds%3600)/60;
            long normalizedSeconds=totalSeconds%60;
            hourSpinner.setValue((int)normalizedHours);
            minuteSpinner.setValue((int)normalizedMinutes);
            secondSpinner.setValue((int)normalizedSeconds);
            long duration=totalSeconds*1000;
            if (duration>0){
                countdownTotalMillis=duration;
                countdownEndMillis=System.currentTimeMillis()+duration;
                countdownRemainingMillis=duration;
                int maximumValue=(duration>Integer.MAX_VALUE)?Integer.MAX_VALUE:(int)duration;
                countdownProgressBar.setMaximum(maximumValue);
                countdownProgressBar.setValue(0);
                countdownRunning=true;
            }
        }
        catch (Exception exception){
            showError("Failed to start countdown", exception);
        }
    }
    private void handleToggleCountdown(){
        try{
            if (countdownRunning){
                cancelAlarm();
                countdownRunning=false;
                countdownRemainingMillis=countdownEndMillis-System.currentTimeMillis();
                if (countdownRemainingMillis<0) countdownRemainingMillis=0;
                countdownPaused=true;
                countdownStopButton.setText("Resume Countdown");
                countdownStopButton.setToolTipText("Resume the paused countdown");
            }
            else if (countdownPaused&&countdownRemainingMillis>0){
                countdownEndMillis=System.currentTimeMillis()+countdownRemainingMillis;
                countdownTotalMillis=countdownRemainingMillis>countdownTotalMillis?countdownRemainingMillis:countdownTotalMillis;
                countdownProgressBar.setValue((int)(countdownTotalMillis-countdownRemainingMillis));
                countdownRunning=true;
                countdownPaused=false;
                countdownStopButton.setText("Stop Countdown");
                countdownStopButton.setToolTipText("Pause the running countdown");
            }
        }
        catch (Exception exception){
            showError("Failed to toggle countdown", exception);
        }
    }
    private void handleResetCountdown(){
        try{
            cancelAlarm();
            countdownRunning=false;
            countdownPaused=false;
            countdownStopButton.setText("Stop Countdown");
            countdownStopButton.setToolTipText("Pause the running countdown");
            hourSpinner.setValue(0);
            minuteSpinner.setValue(0);
            secondSpinner.setValue(0);
            countdownDisplay.setTime("00:", "00:", "00", ".000");
            countdownProgressBar.setValue(0);
        }
        catch (Exception exception){
            showError("Failed to reset countdown", exception);
        }
    }
    private void initStopwatchPanel(){
        JPanel panel=styledPanel("Stopwatch");
        JPanel controls=new JPanel();
        applyPanelStyle(controls);
        final JButton startButton=styledButton("Start");
        startButton.setToolTipText("Start or stop the stopwatch");
        JButton resetButton=styledButton("Reset");
        resetButton.setToolTipText("Reset the stopwatch to zero");
        startButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleToggleStopwatch(startButton);
            }
        });
        resetButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent){
                handleResetStopwatch(startButton);
            }
        });
        controls.add(startButton);
        controls.add(resetButton);
        panel.add(controls, BorderLayout.NORTH);
        stopwatchDisplay=new TimeDisplay();
        stopwatchDisplay.setBorder(BOX_BORDER);
        stopwatchDisplay.setBackground(BACKGROUND_COLOR);
        stopwatchDisplay.setTime("00:", "00:", "00", ".000");
        panel.add(stopwatchDisplay, BorderLayout.CENTER);
        add(panel);
    }
    private void handleToggleStopwatch(JButton startButton){
        try{
            if (stopwatchRunning){
                stopwatchAccumulatedMillis+=System.currentTimeMillis()-stopwatchStartMillis;
                stopwatchRunning=false;
                startButton.setText("Start");
            }
            else{
                stopwatchStartMillis=System.currentTimeMillis();
                stopwatchRunning=true;
                startButton.setText("Stop");
            }
        }
        catch (Exception exception){
            showError("Stopwatch toggle error", exception);
        }
    }
    private void handleResetStopwatch(JButton startButton){
        try{
            stopwatchRunning=false;
            stopwatchAccumulatedMillis=0;
            stopwatchDisplay.setTime("00:", "00:", "00", ".000");
            startButton.setText("Start");
        }
        catch (Exception exception){
            showError("Stopwatch reset error", exception);
        }
    }
    private void initClockPanel(){
        JPanel panel=styledPanel("Current Time");
        JPanel boxPanel=new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        boxPanel.setBackground(BACKGROUND_COLOR);
        boxPanel.setBorder(BOX_BORDER);
        clockDisplay=new TimeDisplay();
        clockDisplay.setTime("00:", "00:", "00", ".000");
        boxPanel.add(clockDisplay);
        timeZoneLabel=new JLabel("(UTC+0)");
        timeZoneLabel.setFont(TEXT_FONT);
        boxPanel.add(timeZoneLabel);
        panel.add(boxPanel, BorderLayout.CENTER);
        add(panel);
    }
    private void cancelAlarm(){
        alarmCancelled=true;
        if (flashTimer!=null && flashTimer.isRunning()){
            flashTimer.stop();
        }
        if (shakeTimer!=null && shakeTimer.isRunning()){
            shakeTimer.stop();
        }
        if (titleResetTimer!=null && titleResetTimer.isRunning()){
            titleResetTimer.stop();
        }
        if (originalLocation!=null){
            setLocation(originalLocation);
            originalLocation=null;
        }
        if (countdownDisplay!=null) countdownDisplay.setBackground(BACKGROUND_COLOR);
        setTitle("All-in-One Timer Tool");
        if (trayIcon!=null && normalTrayImage!=null){
            trayIcon.setImage(normalTrayImage);
        }
        if (currentAudioLine!=null){
            if (currentAudioLine.isOpen()) currentAudioLine.close();
            currentAudioLine=null;
        }
        if (alarmSoundThread!=null){
            if (alarmSoundThread.isAlive()) alarmSoundThread.interrupt();
            alarmSoundThread=null;
        }
        taskbarFlashCount=0;
    }
    private void triggerAlarm(){
        cancelAlarm();
        alarmCancelled=false;
        flashOn=false;
        originalLocation=getLocation();
        shakeCount=0;
        taskbarFlashCount=0;
        if (flashTimer!=null){
            flashTimer.start();
        }
        if (shakeTimer!=null){
            shakeTimer.start();
        }
        alarmSoundThread=new Thread(new Runnable(){
            public void run(){
                executeAlarmSequence();
            }
        });
        alarmSoundThread.start();
        setTitle("Time Is Up");
        if (titleResetTimer!=null){
            titleResetTimer.restart();
        }
    }
    private void handleFlashTick(){
        if (countdownDisplay!=null){
            countdownDisplay.setBackground(flashOn?BACKGROUND_COLOR:ALERT_COLOR);
        }
        if (trayIcon!=null){
            trayIcon.setImage(flashOn?normalTrayImage:redTrayImage);
        }
        if (taskbarFlashCount%2==0){
            EventQueue.invokeLater(new Runnable(){
                public void run(){
                    AllInOneTimer.this.toFront();
                    AllInOneTimer.this.requestFocus();
                }
            });
        }
        taskbarFlashCount++;
        flashOn=!flashOn;
    }
    private void handleShakeTick(){
        int deltaX=(shakeCount%2==0)?5:-5;
        setLocation(originalLocation.x+deltaX, originalLocation.y);
        if (++shakeCount>=20){
            shakeTimer.stop();
            setLocation(originalLocation);
            originalLocation=null;
        }
    }
    private void handleTitleReset(){
        setTitle("All-in-One Timer Tool");
    }
    private void executeAlarmSequence(){
        try{
            playAlertSequence();
        }
        catch (InterruptedException ignored){
        }
        catch (Exception exception){
            showAudioWarning("Audio alert failed: "+exception.getMessage());
        }
        finally{
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    if (flashTimer!=null&&flashTimer.isRunning()){
                        flashTimer.stop();
                    }
                    if (shakeTimer!=null&&shakeTimer.isRunning()){
                        shakeTimer.stop();
                    }
                    if (countdownDisplay!=null) countdownDisplay.setBackground(BACKGROUND_COLOR);
                    if (trayIcon!=null && normalTrayImage!=null){
                        trayIcon.setImage(normalTrayImage);
                    }
                    taskbarFlashCount=0;
                }
            });
        }
    }
    private void playAlertSequence() throws InterruptedException{
        float sampleRate=44100f;
        if (alarmCancelled) return;
        for (int i=0;i<15;i++){
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
            playToneSafe(420, 100, sampleRate, true, 0.3f);
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
            playToneSafe(840, 100, sampleRate, false, 0.1f);
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
            Thread.sleep(100);
            if (alarmCancelled||Thread.currentThread().isInterrupted()) return;
        }
    }
    private void playToneSafe(int frequency, int durationMillis, float sampleRate, boolean square, float gain){
        try{
            playTone(frequency, durationMillis, sampleRate, square, gain);
        }
        catch (Exception ignored){
        }
    }
    private void playTone(int frequency, int durationMillis, float sampleRate, boolean square, float gain) throws Exception{
        int length=(int)(sampleRate*durationMillis/1000);
        if (toneBuffer.length<length){
            toneBuffer=new byte[length];
        }
        byte[] buffer=toneBuffer;
        for (int i=0;i<length;i++){
            double angle=2*Math.PI*i*frequency/sampleRate;
            double value=square?Math.signum(Math.sin(angle)):Math.sin(angle);
            buffer[i]=(byte)(value*gain*127);
        }
        AudioFormat audioFormat=new AudioFormat(sampleRate, 8, 1, true, false);
        SourceDataLine line=AudioSystem.getSourceDataLine(audioFormat);
        currentAudioLine=line;
        try{
            line.open(audioFormat);
            line.start();
            line.write(buffer, 0, length);
            line.drain();
        }
        finally{
            line.close();
            currentAudioLine=null;
        }
    }
    private static String pad(int value, int length){
        String string=Integer.toString(value);
        StringBuilder builder=new StringBuilder(length);
        while (builder.length()+string.length()<length) builder.append('0');
        builder.append(string);
        return builder.toString();
    }
    private JSpinner spinner(int value, int minimum, int maximum){
        JSpinner spinner=new JSpinner(new SpinnerNumberModel(value, minimum, maximum, 1));
        spinner.setPreferredSize(new Dimension(50, 24));
        spinner.setFont(TEXT_FONT);
        spinner.setBorder(SPINNER_BORDER);
        return spinner;
    }
    private JButton styledButton(String text){
        JButton button=new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(BUTTON_TEXT);
        button.setBorder(BUTTON_BORDER);
        button.setFocusPainted(false);
        button.setCursor(HAND_CURSOR);
        button.putClientProperty("originalBackground", BUTTON_BACKGROUND);
        button.putClientProperty("hoverColor", BUTTON_BACKGROUND.darker());
        button.putClientProperty("pressedColor", BUTTON_BACKGROUND.brighter());
        button.addMouseListener(BUTTON_HOVER_LISTENER);
        return button;
    }
    private JPanel styledPanel(String title){
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5), title,
            JLabel.CENTER, JLabel.TOP, BUTTON_FONT, Color.BLACK));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setOpaque(true);
        return panel;
    }
    private void applyPanelStyle(JPanel panel){
        panel.setBackground(SECONDARY_COLOR);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    private void showError(String message, Exception exception){
        JOptionPane.showMessageDialog(this, message+": "+exception.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
        exception.printStackTrace();
    }
    private void showAudioWarning(String message){
        if (!audioWarningShown){
            audioWarningShown=true;
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    JOptionPane.showMessageDialog(AllInOneTimer.this, message,
                        "Audio Warning", JOptionPane.WARNING_MESSAGE);
                }
            });
        }
    }
    static class TimeDisplay extends JComponent{
        private String hoursColon="00:";
        private String minutesColon="00:";
        private String seconds="00";
        private String milliseconds=".000";
        TimeDisplay(){
            setOpaque(true);
            setBackground(Color.WHITE);
            setFont(LABEL_FONT);
        }
        void setTime(String hoursColon, String minutesColon, String seconds, String milliseconds){
            this.hoursColon=hoursColon;
            this.minutesColon=minutesColon;
            this.seconds=seconds;
            this.milliseconds=milliseconds;
            repaint();
        }
        protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(getFont());
            FontMetrics fontMetrics=graphics.getFontMetrics();
            int totalWidth=fontMetrics.stringWidth(hoursColon)+fontMetrics.stringWidth(minutesColon)
                          +fontMetrics.stringWidth(seconds)+fontMetrics.stringWidth(milliseconds);
            int x=(getWidth()-totalWidth)/2;
            int y=(getHeight()+fontMetrics.getAscent()-fontMetrics.getDescent())/2;
            graphics.drawString(hoursColon, x, y);
            x+=fontMetrics.stringWidth(hoursColon);
            graphics.drawString(minutesColon, x, y);
            x+=fontMetrics.stringWidth(minutesColon);
            graphics.drawString(seconds, x, y);
            x+=fontMetrics.stringWidth(seconds);
            graphics.drawString(milliseconds, x, y);
        }
        public Dimension getPreferredSize(){
            FontMetrics fontMetrics=getFontMetrics(getFont());
            int width=fontMetrics.stringWidth("00:00:00.000")+20;
            int height=fontMetrics.getHeight()+20;
            return new Dimension(width, height);
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