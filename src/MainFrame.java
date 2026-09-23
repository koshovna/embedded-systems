import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    // Физические параметры модели
    private double g = 70.0;
    private double s = 0.0;
    private double omega = 0.0;
    private double phi = 0.0;
    private double T = 0.0;
    private double OC = 0.0;

    private double P_priv = 0.8;
    private double n = 1.0;
    private double P_kotel = 0.5;
    private double k_os = 1.0;
    private final double tau = 0.05;

    // Метрики качества для Лабы №2
    private int ticks = 0;
    private int settlingTicks = 0;
    private boolean settled = false;
    private int waveCount = 0;
    private double prevT = 0.0;
    private boolean movingUp = true;
    private double lastPeakAmp = 0.0;
    private boolean isUnstable = false;

    // UI Элементы
    private JCheckBox chkEnable;
    private JTextField txtG, txtSignal, txtSpeed, txtPos, txtTemp;
    private JTextField txtAccuracy, txtSpeedTicks, txtDamping;
    private JSlider sliderMotorP, sliderGearN, sliderKotelP, sliderFeedbackK;
    private JLabel lblMotorP, lblGearN, lblKotelP, lblFeedbackK;
    private JPanel statusBox; // Зеленый/Красный квадрат
    private JButton btnStart, btnReset;
    private Timer timer;
    private boolean isRunning = false;

    private GraphPanel graphPanel;

    public MainFrame() {
        setTitle("Система автоматического управления котла");
        setSize(1080, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new GridBagLayout());
        topContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 4, 2, 4);

        // 1. Уставка
        JPanel pnlSetpoint = new JPanel();
        pnlSetpoint.setLayout(new BoxLayout(pnlSetpoint, BoxLayout.Y_AXIS));
        pnlSetpoint.setPreferredSize(new Dimension(140, 220));
        chkEnable = new JCheckBox("Вкл", true);
        txtG = new JTextField(String.valueOf((int)g), 6);
        btnStart = new JButton("Пуск");
        btnReset = new JButton("Сброс");

        pnlSetpoint.add(chkEnable);
        pnlSetpoint.add(Box.createVerticalStrut(15));
        pnlSetpoint.add(new JLabel("Уставка температуры %"));
        pnlSetpoint.add(txtG);
        pnlSetpoint.add(Box.createVerticalStrut(10));
        pnlSetpoint.add(btnStart);
        pnlSetpoint.add(Box.createVerticalStrut(5));
        pnlSetpoint.add(btnReset);

        c.gridx = 0; c.gridy = 0;
        topContainer.add(pnlSetpoint, c);

        // 2. Регулятор
        JPanel pnlReg = new JPanel();
        pnlReg.setLayout(new BoxLayout(pnlReg, BoxLayout.Y_AXIS));
        pnlReg.setBorder(BorderFactory.createTitledBorder("Регулятор"));
        pnlReg.setPreferredSize(new Dimension(110, 220));
        txtSignal = new JTextField("0", 6);
        txtSignal.setEditable(false);

        pnlReg.add(Box.createVerticalStrut(75));
        pnlReg.add(new JLabel("сигнал"));
        pnlReg.add(txtSignal);

        c.gridx = 1; c.gridy = 0;
        topContainer.add(pnlReg, c);

        // 3. Мотор
        JPanel pnlMotor = new JPanel();
        pnlMotor.setLayout(new BoxLayout(pnlMotor, BoxLayout.Y_AXIS));
        pnlMotor.setBorder(BorderFactory.createTitledBorder("Мотор управления клапаном"));
        pnlMotor.setPreferredSize(new Dimension(190, 220));
        lblMotorP = new JLabel(String.format("мощность: %.1f", P_priv));
        sliderMotorP = new JSlider(1, 40, (int)(P_priv * 10)); // до 4.0 для вызова неустойчивости
        txtSpeed = new JTextField("0", 6);
        txtSpeed.setEditable(false);

        pnlMotor.add(Box.createVerticalStrut(10));
        pnlMotor.add(lblMotorP);
        pnlMotor.add(sliderMotorP);
        pnlMotor.add(Box.createVerticalStrut(15));
        pnlMotor.add(new JLabel("скорость"));
        pnlMotor.add(txtSpeed);

        c.gridx = 2; c.gridy = 0;
        topContainer.add(pnlMotor, c);

        // 4. Топливный клапан & ОС
        JPanel pnlValveContainer = new JPanel(new BorderLayout());
        JPanel pnlValve = new JPanel();
        pnlValve.setLayout(new BoxLayout(pnlValve, BoxLayout.Y_AXIS));
        pnlValve.setBorder(BorderFactory.createTitledBorder("Топливный клапан"));
        pnlValve.setPreferredSize(new Dimension(180, 130));
        lblGearN = new JLabel(String.format("Передача: %.1f", n));
        sliderGearN = new JSlider(1, 40, (int)(n * 10));
        txtPos = new JTextField("0", 6);
        txtPos.setEditable(false);

        pnlValve.add(lblGearN);
        pnlValve.add(sliderGearN);
        pnlValve.add(Box.createVerticalStrut(5));
        pnlValve.add(new JLabel("Положение"));
        pnlValve.add(txtPos);

        JPanel pnlFeedback = new JPanel();
        pnlFeedback.setLayout(new BoxLayout(pnlFeedback, BoxLayout.Y_AXIS));
        pnlFeedback.setBorder(BorderFactory.createTitledBorder("Обратная связь"));
        lblFeedbackK = new JLabel(String.format("k: %.1f", k_os));
        sliderFeedbackK = new JSlider(1, 20, (int)(k_os * 10));
        pnlFeedback.add(lblFeedbackK);
        pnlFeedback.add(sliderFeedbackK);

        pnlValveContainer.add(pnlValve, BorderLayout.NORTH);
        pnlValveContainer.add(pnlFeedback, BorderLayout.SOUTH);

        c.gridx = 3; c.gridy = 0;
        topContainer.add(pnlValveContainer, c);

        // 5. Котёл с индикатором устойчивости
        JPanel pnlKotel = new JPanel();
        pnlKotel.setLayout(new BoxLayout(pnlKotel, BoxLayout.Y_AXIS));
        pnlKotel.setBorder(BorderFactory.createTitledBorder("Котёл"));
        pnlKotel.setPreferredSize(new Dimension(180, 220));
        lblKotelP = new JLabel(String.format("мощность: %.1f", P_kotel));
        sliderKotelP = new JSlider(1, 30, (int)(P_kotel * 10));
        txtTemp = new JTextField("0", 6);
        txtTemp.setEditable(false);

        statusBox = new JPanel();
        statusBox.setPreferredSize(new Dimension(80, 45));
        statusBox.setMaximumSize(new Dimension(80, 45));
        statusBox.setBackground(new Color(40, 180, 40)); // Зеленый по умолчанию
        statusBox.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        pnlKotel.add(Box.createVerticalStrut(10));
        pnlKotel.add(lblKotelP);
        pnlKotel.add(sliderKotelP);
        pnlKotel.add(Box.createVerticalStrut(10));
        JPanel rowTemp = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rowTemp.add(new JLabel("Температура"));
        rowTemp.add(txtTemp);
        pnlKotel.add(rowTemp);
        pnlKotel.add(Box.createVerticalStrut(10));
        pnlKotel.add(statusBox);

        c.gridx = 4; c.gridy = 0;
        topContainer.add(pnlKotel, c);

        // 6. Качество управления (Точность, Быстродействие, Затухание)
        JPanel pnlQuality = new JPanel();
        pnlQuality.setLayout(new BoxLayout(pnlQuality, BoxLayout.Y_AXIS));
        pnlQuality.setBorder(BorderFactory.createTitledBorder("Качество управления"));
        pnlQuality.setPreferredSize(new Dimension(160, 220));

        txtAccuracy = new JTextField("", 7);
        txtAccuracy.setEditable(false);
        txtSpeedTicks = new JTextField("", 7);
        txtSpeedTicks.setEditable(false);
        txtDamping = new JTextField("", 7);
        txtDamping.setEditable(false);

        pnlQuality.add(new JLabel("Точность"));
        pnlQuality.add(txtAccuracy);
        pnlQuality.add(Box.createVerticalStrut(8));
        pnlQuality.add(new JLabel("Быстродействие"));
        pnlQuality.add(txtSpeedTicks);
        pnlQuality.add(Box.createVerticalStrut(8));
        pnlQuality.add(new JLabel("Затухание"));
        pnlQuality.add(txtDamping);

        c.gridx = 5; c.gridy = 0;
        topContainer.add(pnlQuality, c);

        add(topContainer, BorderLayout.NORTH);

        // Слушатели ползунков
        sliderMotorP.addChangeListener(e -> {
            P_priv = sliderMotorP.getValue() / 10.0;
            lblMotorP.setText(String.format("мощность: %.1f", P_priv));
        });
        sliderGearN.addChangeListener(e -> {
            n = sliderGearN.getValue() / 10.0;
            lblGearN.setText(String.format("Передача: %.1f", n));
        });
        sliderFeedbackK.addChangeListener(e -> {
            k_os = sliderFeedbackK.getValue() / 10.0;
            lblFeedbackK.setText(String.format("k: %.1f", k_os));
        });
        sliderKotelP.addChangeListener(e -> {
            P_kotel = sliderKotelP.getValue() / 10.0;
            lblKotelP.setText(String.format("мощность: %.1f", P_kotel));
        });

        // График
        graphPanel = new GraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        timer = new Timer((int)(tau * 1000), e -> stepSimulation());

        btnStart.addActionListener(e -> {
            if (!isRunning) {
                try {
                    g = Double.parseDouble(txtG.getText());
                } catch (Exception ex) {
                    g = 70.0;
                }
                timer.start();
                btnStart.setText("Стоп");
                isRunning = true;
            } else {
                timer.stop();
                btnStart.setText("Пуск");
                isRunning = false;
            }
        });

        btnReset.addActionListener(e -> resetSimulation());
    }

    private void resetSimulation() {
        timer.stop();
        isRunning = false;
        btnStart.setText("Пуск");
        omega = 0; phi = 0; T = 0; OC = 0; s = 0;
        ticks = 0; settlingTicks = 0; settled = false;
        waveCount = 0; isUnstable = false; lastPeakAmp = 0;
        txtSignal.setText("0"); txtSpeed.setText("0"); txtPos.setText("0"); txtTemp.setText("0");
        txtAccuracy.setText(""); txtSpeedTicks.setText(""); txtDamping.setText("");
        statusBox.setBackground(new Color(40, 180, 40));
        graphPanel.clear();
    }

    private void stepSimulation() {
        if (!chkEnable.isSelected()) return;

        ticks++;

        try {
            g = Double.parseDouble(txtG.getText());
        } catch (Exception ignored) {}

        // 1. ОС
        OC = k_os * T;
        // 2. Регулятор
        s = g - OC;
        // 3. Мотор
        omega = omega + (s - omega) * P_priv * tau;
        // 4. Клапан
        phi = phi + (omega * n * tau);
        if (phi < 0) phi = 0;
        // 5. Котёл
        T = T + (phi - T) * P_kotel * tau;

        // Контроль взрыва модели при вразносе
        if (Math.abs(T) > 500) {
            isUnstable = true;
        }

        // Подсчет волн и устойчивости
        if (movingUp && T < prevT) {
            waveCount++;
            // Считаем амплитуду относительно уровня, к которому стремится система (g / k_os)
            double targetLevel = (k_os > 0) ? (g / k_os) : g;
            double currentAmp = Math.abs(prevT - targetLevel);

            if (waveCount > 2) {
                if (currentAmp > lastPeakAmp * 1.1) {
                    isUnstable = true; // Амплитуда РЕАЛЬНО растет
                }
            }
            lastPeakAmp = currentAmp;
            movingUp = false;
        } else if (!movingUp && T > prevT) {
            movingUp = true;
        }
        prevT = T;

        // Ошибка и быстродействие (коридор 5%)
        double errorAbs = Math.abs(g - T);
        double accuracyPct = (errorAbs / Math.max(1.0, g)) * 100.0;

        if (!settled && errorAbs < (0.05 * g) && ticks > 15) {
            settled = true;
            settlingTicks = ticks;
        } else if (errorAbs >= (0.05 * g)) {
            settled = false;
        }

        // Обновление статуса в блоке Котла
        if (isUnstable) {
            statusBox.setBackground(new Color(220, 40, 40)); // Красный — Вразнос
            txtDamping.setText("Вразнос!");
            txtSpeedTicks.setText("—");
        } else if (ticks > 60 && !settled) {
            statusBox.setBackground(new Color(220, 180, 20)); // Желтый — На грани
            txtDamping.setText("Колебания");
            txtSpeedTicks.setText("—");
        } else {
            statusBox.setBackground(new Color(40, 180, 40)); // Зеленый — Устойчиво
            txtDamping.setText(waveCount > 0 ? waveCount + " волны" : "0 волн");
            txtSpeedTicks.setText(settlingTicks > 0 ? settlingTicks + " тактов" : ticks + " тактов");
        }

        txtSignal.setText(String.format("%.1f", s));
        txtSpeed.setText(String.format("%.1f", omega));
        txtPos.setText(String.format("%.1f", phi));
        txtTemp.setText(String.format("%.2f", T));
        txtAccuracy.setText(String.format("%.2f%%", accuracyPct));

        graphPanel.addPoints(g, T, phi);
    }

    static class GraphPanel extends JPanel {
        private final List<Double> targetHistory = new ArrayList<>();
        private final List<Double> tempHistory = new ArrayList<>();
        private final List<Double> valveHistory = new ArrayList<>();

        public GraphPanel() { setBackground(Color.BLACK); }

        public void clear() {
            targetHistory.clear(); tempHistory.clear(); valveHistory.clear();
            repaint();
        }

        public void addPoints(double target, double temp, double valve) {
            targetHistory.add(target);
            tempHistory.add(temp);
            valveHistory.add(valve);

            if (targetHistory.size() > 950) {
                targetHistory.remove(0);
                tempHistory.remove(0);
                valveHistory.remove(0);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int h = getHeight(), w = getWidth();
            g2.setColor(new Color(40, 40, 40));
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);
            for (int x = 0; x < w; x += 50) g2.drawLine(x, 0, x, h);

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(Color.RED); g2.drawString("— Уставка", 20, 20);
            g2.setColor(Color.GREEN); g2.drawString("— Температура котла", 120, 20);
            g2.setColor(Color.CYAN); g2.drawString("— Клапан", 280, 20);

            if (tempHistory.size() < 2) return;
            double scaleY = h / 130.0;

            for (int i = 0; i < tempHistory.size() - 1; i++) {
                int x1 = i, x2 = i + 1;
                g2.setColor(Color.RED);
                g2.drawLine(x1, h - (int)(targetHistory.get(i) * scaleY), x2, h - (int)(targetHistory.get(i + 1) * scaleY));
                g2.setColor(Color.CYAN);
                g2.drawLine(x1, h - (int)(valveHistory.get(i) * scaleY), x2, h - (int)(valveHistory.get(i + 1) * scaleY));
                g2.setColor(Color.GREEN);
                g2.drawLine(x1, h - (int)(tempHistory.get(i) * scaleY), x2, h - (int)(tempHistory.get(i + 1) * scaleY));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}