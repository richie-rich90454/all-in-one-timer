import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
public class AllInOneTimer extends JFrame{
    private JSpinner hourSpinner, minSpinner, secSpinner;
    private JLabel cdHour, cdMin, cdSec, cdMs;
    private Timer countdownTimer;
    private long countdownEndMs;
    private JLabel swHour, swMin, swSec, swMs;
    private Timer stopwatchTimer;
    private long swStartMs;
    private long swAccumulatedMs;
    private boolean swRunning=false;
    private JLabel timeHour, timeMin, timeSec, timeMs, timeZone;
    private Timer clockTimer;
    private JPanel countdownBox;
    private Timer flashTimer;
    private boolean flashOn;
    private Point origLocation;
    private Timer shakeTimer;
    private int shakeCount;
    private static final Color PRIMARY_COLOR=Color.decode("#32A5E4");
    private static final Color SECONDARY_COLOR=Color.decode("#1C94E9");
    private static final Color BACKGROUND_COLOR=Color.WHITE;
    private static final Color ALERT_COLOR=Color.decode("#DE0000");
    private static final Color BUTTON_BACKGROUND=Color.decode("#DE0000");
    private static final Color BUTTON_TEXT=Color.WHITE;
    private static final Font LABEL_FONT=new Font("Noto Sans", Font.BOLD, 24);
    private static final Font BUTTON_FONT=new Font("Noto Sans", Font.BOLD, 14);
    private static final Font TEXT_FONT=new Font("Noto Sans", Font.PLAIN, 14);
    private static final Cursor HAND_CURSOR=new Cursor(Cursor.HAND_CURSOR);
    public AllInOneTimer(){
        super("All-in-one Timer Tool");
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
    }
    private void loadCustomFont(){
        try{
            InputStream is=getClass().getResourceAsStream("/NotoSans-VariableFont_wdth_wght.ttf");
            Font font=Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            setFont(TEXT_FONT);
        }
        catch (Exception e){

        }
    }
    private void initCountdownPanel(){
        JPanel panel=styledPanel("Countdown");
        JPanel controls=new JPanel();
        applyPanelStyle(controls);
        JLabel hourLabel=new JLabel("Hours");
        hourLabel.setFont(TEXT_FONT);
        hourLabel.setForeground(Color.decode("#FFFFFF"));
        JLabel minLabel=new JLabel("Minutes");
        minLabel.setFont(TEXT_FONT);
        minLabel.setForeground(Color.decode("#FFFFFF"));
        JLabel secLabel=new JLabel("Seconds");
        secLabel.setFont(TEXT_FONT);
        secLabel.setForeground(Color.decode("#FFFFFF"));
        hourSpinner=spinner(0, 0, Integer.MAX_VALUE);
        minSpinner=spinner(0, 0, 59);
        secSpinner=spinner(0, 0, 59);
        JButton setBtn=styledButton("Set Countdown");
        setBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int h=(Integer) hourSpinner.getValue();
                int m=(Integer) minSpinner.getValue();
                int s=(Integer) secSpinner.getValue();
                long duration=((long) h*3600+m*60+s)*1000;
                countdownEndMs=System.currentTimeMillis()+duration;
                countdownTimer.start();
            }
        });
        controls.add(hourLabel);
        controls.add(hourSpinner);
        controls.add(minLabel);
        controls.add(minSpinner);
        controls.add(secLabel);
        controls.add(secSpinner);
        controls.add(setBtn);
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
        countdownTimer=new Timer(10, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                long rem=countdownEndMs-System.currentTimeMillis();
                if (rem<=0){
                    countdownTimer.stop();
                    cdHour.setText("00:");
                    cdMin.setText("00:");
                    cdSec.setText("00");
                    cdMs.setText(".000");
                    triggerAlarm();
                }
                else{
                    int h=(int) (rem/3600000);
                    int m=(int) ((rem%3600000)/60000);
                    int s=(int) ((rem%60000)/1000);
                    int ms=(int) (rem%1000);
                    cdHour.setText(pad(h, 2)+":");
                    cdMin.setText(pad(m, 2)+":");
                    cdSec.setText(pad(s, 2));
                    cdMs.setText("."+pad(ms, 3));
                }
            }
        });
        add(panel);
    }
    private void initStopwatchPanel(){
        JPanel panel=styledPanel("Stopwatch");
        JPanel controls=new JPanel();
        applyPanelStyle(controls);
        JButton startBtn=styledButton("START");
        JButton resetBtn=styledButton("RESET");
        startBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if (swRunning){
                    stopwatchTimer.stop();
                    swAccumulatedMs+=System.currentTimeMillis()-swStartMs;
                    startBtn.setText("START");
                }
                else{
                    swStartMs=System.currentTimeMillis();
                    stopwatchTimer.start();
                    startBtn.setText("STOP");
                }
                swRunning=!swRunning;
            }
        });
        resetBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                stopwatchTimer.stop();
                swAccumulatedMs=0;
                swHour.setText("00:");
                swMin.setText("00:");
                swSec.setText("00");
                swMs.setText(".000");
                startBtn.setText("START");
                swRunning=false;
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
                long elapsed=System.currentTimeMillis()-swStartMs+swAccumulatedMs;
                int h=(int) (elapsed/3600000);
                int m=(int) ((elapsed%3600000)/60000);
                int s=(int) ((elapsed%60000)/1000);
                int ms=(int) (elapsed%1000);
                swHour.setText(pad(h, 2)+":");
                swMin.setText(pad(m, 2)+":");
                swSec.setText(pad(s, 2));
                swMs.setText("."+pad(ms, 3));
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
                Calendar cal=Calendar.getInstance();
                int h=cal.get(Calendar.HOUR_OF_DAY);
                int m=cal.get(Calendar.MINUTE);
                int s=cal.get(Calendar.SECOND);
                int ms=cal.get(Calendar.MILLISECOND);
                timeHour.setText(pad(h, 2)+":");
                timeMin.setText(pad(m, 2)+":");
                timeSec.setText(pad(s, 2));
                timeMs.setText("."+pad(ms, 3));
                TimeZone tz=cal.getTimeZone();
                int off=tz.getRawOffset()/3600000;
                String sg=(off>=0)?"+":"";
                timeZone.setText("(UTC"+sg+off+")");
            }
        });
        clockTimer.start();
    }
    private void triggerAlarm(){
        flashOn=false;
        flashTimer=new Timer(500, new ActionListener(){
            private int cnt=0;
            public void actionPerformed(ActionEvent e){
                countdownBox.setBackground(flashOn?BACKGROUND_COLOR:ALERT_COLOR);
                flashOn=!flashOn;
                if (++cnt>=10){
                    flashTimer.stop();
                }
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
                }
            }
        });
        shakeTimer.start();
        new Thread(new Runnable(){
            public void run(){
                try{
                    playAlertSequence();
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();
        setTitle("TIME IS UP");
        new Timer(5000, new ActionListener(){
            public void actionPerformed(ActionEvent e){
                setTitle("All-in-one Timer Tool");
            }
        }).start();
    }
    private void playAlertSequence() throws Exception{
        float sr=44100f;
        for (int i=0;i<15;i++){
            playTone(420, 100, sr, true, 0.3f);
            playTone(840, 100, sr, false, 0.1f);
            Thread.sleep(100);
        }
    }
    private void playTone(int freq, int ms, float sr, boolean square, float gain) throws Exception{
        int len=(int) (sr*ms/1000);
        byte[] buf=new byte[len];
        for (int i=0;i<len;i++){
            double angle=2*Math.PI*i*freq/sr;
            double val=square?Math.signum(Math.sin(angle)):Math.sin(angle);
            buf[i]=(byte) (val*gain*127);
        }
        AudioFormat af=new AudioFormat(sr, 8, 1, true, false);
        SourceDataLine line=AudioSystem.getSourceDataLine(af);
        line.open(af);
        line.start();
        line.write(buf, 0, len);
        line.drain();
        line.close();
    }
    private String pad(int v, int l){
        String s=""+v;
        while (s.length()<l){
            s="0"+s;
        }
        return s;
    }
    private JSpinner spinner(int val, int min, int max){
        JSpinner sp=new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setPreferredSize(new Dimension(50, 24));
        sp.setFont(TEXT_FONT);
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
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 70)), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(HAND_CURSOR);
        btn.addMouseListener(new MouseAdapter(){
            private Color originalBg=BUTTON_BACKGROUND;
            private Color hoverColor=BUTTON_BACKGROUND.darker();
            private Color pressedColor=BUTTON_BACKGROUND.brighter();
            public void mouseEntered(MouseEvent e){
                animateBackground(btn, originalBg, hoverColor, 200);
            }
            public void mouseExited(MouseEvent e){
                animateBackground(btn, btn.getBackground(), originalBg, 200);
            }
            public void mousePressed(MouseEvent e){
                animateBackground(btn, btn.getBackground(), pressedColor, 100);
            }
            public void mouseReleased(MouseEvent e){
                animateBackground(btn, btn.getBackground(), hoverColor, 100);
            }
        });
        return btn;
    }
    private void animateBackground(final JComponent component, final Color from, final Color to, final int duration){
        new Thread(new Runnable(){
            public void run(){
                try{
                    final long startTime=System.currentTimeMillis();
                    while (System.currentTimeMillis()-startTime<duration){
                        float progress=(float) (System.currentTimeMillis()-startTime)/duration;
                        if (progress>1){
                            progress=1;
                        }
                        final int red=(int) (from.getRed()+(to.getRed()-from.getRed())*progress);
                        final int green=(int) (from.getGreen()+(to.getGreen()-from.getGreen())*progress);
                        final int blue=(int) (from.getBlue()+(to.getBlue()-from.getBlue())*progress);
                        final Color intermediate=new Color(red, green, blue);
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run(){
                                component.setBackground(intermediate);
                            }
                        });
                        Thread.sleep(10);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private JPanel styledPanel(String title){
        JPanel p=new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), title, JLabel.CENTER, JLabel.TOP, BUTTON_FONT, Color.decode("#000000")));
        p.setBackground(BACKGROUND_COLOR);
        p.setOpaque(true);
        return p;
    }
    private void applyPanelStyle(JPanel p){
        p.setBackground(SECONDARY_COLOR);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    private JPanel styledBox(){
        JPanel p=new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#000000"), 1), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        p.setBackground(BACKGROUND_COLOR);
        p.setOpaque(true);
        return p;
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                new AllInOneTimer();
            }
        });
    }
}