import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    // Физические параметры модели
    private double g = 70.0;        // Уставка (%)
    private double s = 0.0;         // Сигнал регулятора
    private double omega = 0.0;     // Скорость привода
    private double phi = 0.0;       // Положение клапана
    private double T = 0.0;         // Температура котла
    private double OC = 0.0;        // Сигнал обратной связи

    // Коэффициенты
    private double P_priv = 0.8;    // Мощность привода
    private double n = 1.0;         // Передача
    private double P_kotel = 0.5;   // Мощность котла
    private double k_os = 1.0;      // Коэффициент ОС
    private final double tau = 0.05;// Шаг времени (секунды)

    // Переменные расчета качества
    private double simTime = 0.0;
    private double settlingTime = 0.0;
    private boolean isSettled = false;
    private double firstPeak = 0.0;
    private double secondPeak = 0.0;
    private double damping = 0.0;
    private double prevT = 0.0;
    private boolean movingUp = true;
    private int peakCount = 0;

    // UI Элементы
    private JCheckBox chkEnable;
    private JTextField txtG, txtSignal, txtSpeed, txtPos, txtTemp;
    private JTextField txtError, txtSettlingTime, txtDamping;
    private JSlider sliderMotorP, sliderGearN, sliderKotelP, sliderFeedbackK;
    private JLabel lblMotorP, lblGearN, lblKotelP, lblFeedbackK;
    private JButton btnStart;
    private Timer timer;
    private boolean isRunning = false;

    private GraphPanel graphPanel;

    public MainFrame() {
        setTitle("Система автоматического управления котла");
        setSize(1050, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Верхняя панель
        JPanel topContainer = new JPanel(new GridBagLayout());
        topContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 4, 2, 4);

        // 1. Блок Уставка / Вкл
        JPanel pnlSetpoint = new JPanel();
        pnlSetpoint.setLayout(new BoxLayout(pnlSetpoint, BoxLayout.Y_AXIS));
        pnlSetpoint.setPreferredSize(new Dimension(140, 210));
        chkEnable = new JCheckBox("Вкл", true);
        txtG = new JTextField(String.valueOf((int)g), 6);
        btnStart = new JButton("Пуск");
        btnStart.setPreferredSize(new Dimension(100, 30));

        pnlSetpoint.add(chkEnable);
        pnlSetpoint.add(Box.createVerticalStrut(25));
        pnlSetpoint.add(new JLabel("Уставка температуры %"));
        pnlSetpoint.add(txtG);
        pnlSetpoint.add(Box.createVerticalStrut(10));
        pnlSetpoint.add(btnStart);

        c.gridx = 0; c.gridy = 0;
        topContainer.add(pnlSetpoint, c);

        // 2. Блок "Регулятор"
        JPanel pnlReg = new JPanel();
        pnlReg.setLayout(new BoxLayout(pnlReg, BoxLayout.Y_AXIS));
        pnlReg.setBorder(BorderFactory.createTitledBorder("Регулятор"));
        pnlReg.setPreferredSize(new Dimension(110, 210));
        txtSignal = new JTextField("0", 6);
        txtSignal.setEditable(false);

        pnlReg.add(Box.createVerticalStrut(75));
        pnlReg.add(new JLabel("сигнал"));
        pnlReg.add(txtSignal);

        c.gridx = 1; c.gridy = 0;
        topContainer.add(pnlReg, c);

        // 3. Блок "Мотор управления клапаном"
        JPanel pnlMotor = new JPanel();
        pnlMotor.setLayout(new BoxLayout(pnlMotor, BoxLayout.Y_AXIS));
        pnlMotor.setBorder(BorderFactory.createTitledBorder("Мотор управления клапаном"));
        pnlMotor.setPreferredSize(new Dimension(190, 210));
        lblMotorP = new JLabel(String.format("мощность: %.1f", P_priv));
        sliderMotorP = new JSlider(1, 20, (int)(P_priv * 10));
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

        // 4. Блок "Топливный клапан" + "Обратная связь"
        JPanel pnlValveContainer = new JPanel(new BorderLayout());
        JPanel pnlValve = new JPanel();
        pnlValve.setLayout(new BoxLayout(pnlValve, BoxLayout.Y_AXIS));
        pnlValve.setBorder(BorderFactory.createTitledBorder("Топливный клапан"));
        pnlValve.setPreferredSize(new Dimension(180, 130));
        lblGearN = new JLabel(String.format("Передача: %.1f", n));
        sliderGearN = new JSlider(1, 20, (int)(n * 10));
        txtPos = new JTextField("0", 6);
        txtPos.setEditable(false);

        pnlValve.add(lblGearN);
        pnlValve.add(sliderGearN);
        pnlValve.add(Box.createVerticalStrut(5));
        pnlValve.add(new JLabel("Положение"));
        pnlValve.add(txtPos);

        // Подблок Обратная связь
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

        // 5. Блок "Котёл"
        JPanel pnlKotel = new JPanel();
        pnlKotel.setLayout(new BoxLayout(pnlKotel, BoxLayout.Y_AXIS));
        pnlKotel.setBorder(BorderFactory.createTitledBorder("Котёл"));
        pnlKotel.setPreferredSize(new Dimension(180, 210));
        lblKotelP = new JLabel(String.format("мощность: %.1f", P_kotel));
        sliderKotelP = new JSlider(1, 20, (int)(P_kotel * 10));
        txtTemp = new JTextField("0", 6);
        txtTemp.setEditable(false);

        pnlKotel.add(Box.createVerticalStrut(10));
        pnlKotel.add(lblKotelP);
        pnlKotel.add(sliderKotelP);
        pnlKotel.add(Box.createVerticalStrut(15));
        JPanel rowTemp = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rowTemp.add(new JLabel("Температура"));
        rowTemp.add(txtTemp);
        pnlKotel.add(rowTemp);

        c.gridx = 4; c.gridy = 0;
        topContainer.add(pnlKotel, c);

        // 6. Блок "Качество управления"
        JPanel pnlQuality = new JPanel();
        pnlQuality.setLayout(new BoxLayout(pnlQuality, BoxLayout.Y_AXIS));
        pnlQuality.setBorder(BorderFactory.createTitledBorder("Качество управления"));
        pnlQuality.setPreferredSize(new Dimension(150, 210));

        txtError = new JTextField("", 6);
        txtError.setEditable(false);
        txtSettlingTime = new JTextField("", 6);
        txtSettlingTime.setEditable(false);
        txtDamping = new JTextField("", 6);
        txtDamping.setEditable(false);

        pnlQuality.add(new JLabel("Ошибка"));
        pnlQuality.add(txtError);
        pnlQuality.add(Box.createVerticalStrut(6));
        pnlQuality.add(new JLabel("Быстродействие"));
        pnlQuality.add(txtSettlingTime);
        pnlQuality.add(Box.createVerticalStrut(6));
        pnlQuality.add(new JLabel("Затухание"));
        pnlQuality.add(txtDamping);

        c.gridx = 5; c.gridy = 0;
        topContainer.add(pnlQuality, c);

        add(topContainer, BorderLayout.NORTH);

        // Слушатели ползунков (обновление цифр при перетаскивании)
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

        // Черный график
        graphPanel = new GraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        // Таймер
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
    }

    private void stepSimulation() {
        simTime += tau;

        try {
            g = Double.parseDouble(txtG.getText());
        } catch (Exception ignored) {}

        // 1. Датчик ОС (если галочка Вкл снята — обратная связь обрывается, OC = 0)
        if (chkEnable.isSelected()) {
            OC = k_os * T;
        } else {
            OC = 0.0;
        }

        // 2. Регулятор: s = g - OC
        s = g - OC;

        // 3. Мотор: w = w0 + (s - w0) * P * tau
        omega = omega + (s - omega) * P_priv * tau;

        // 4. Клапан: phi = phi0 + w * n * tau
        phi = phi + (omega * n * tau);
        if (phi < 0) phi = 0;

        // 5. Котёл: T = T0 + (phi - T0) * P * tau
        T = T + (phi - T) * P_kotel * tau;

        // Расчет показателей качества
        double currentError = Math.abs(g - T);

        if (!isSettled && currentError < (0.05 * g) && simTime > 1.0) {
            settlingTime = simTime;
            isSettled = true;
        } else if (currentError >= (0.05 * g)) {
            isSettled = false;
        }

        if (movingUp && T < prevT) {
            peakCount++;
            if (peakCount == 1) {
                firstPeak = prevT - g;
            } else if (peakCount == 2) {
                secondPeak = prevT - g;
                if (firstPeak > 0) {
                    damping = Math.max(0, 1.0 - (secondPeak / firstPeak));
                }
            }
            movingUp = false;
        } else if (!movingUp && T > prevT) {
            movingUp = true;
        }
        prevT = T;

        // Обновление полей на экране
        txtSignal.setText(String.format("%.1f", s));
        txtSpeed.setText(String.format("%.1f", omega));
        txtPos.setText(String.format("%.1f", phi));
        txtTemp.setText(String.format("%.1f", T));

        txtError.setText(String.format("%.2f", currentError));
        txtSettlingTime.setText(settlingTime > 0 ? String.format("%.1f с", settlingTime) : "—");
        txtDamping.setText(damping > 0 ? String.format("%.2f", damping) : "1.00");

        graphPanel.addPoints(g, T, phi);
    }

    static class GraphPanel extends JPanel {
        private final List<Double> targetHistory = new ArrayList<>();
        private final List<Double> tempHistory = new ArrayList<>();
        private final List<Double> valveHistory = new ArrayList<>();

        public GraphPanel() {
            setBackground(Color.BLACK);
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

            int h = getHeight();
            int w = getWidth();

            // Сетка
            g2.setColor(new Color(40, 40, 40));
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);
            for (int x = 0; x < w; x += 50) g2.drawLine(x, 0, x, h);

            // ПОДПИСИ ЛИНИЙ (ЛЕГЕНДА)
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(Color.RED);
            g2.drawString("— Уставка температуры", 20, 25);

            g2.setColor(Color.GREEN);
            g2.drawString("— Температура котла", 280, 25);

            g2.setColor(Color.CYAN);
            g2.drawString("— Положение клапана", 530, 25);

            if (tempHistory.size() < 2) return;

            double scaleY = h / 130.0;

            for (int i = 0; i < tempHistory.size() - 1; i++) {
                int x1 = i;
                int x2 = i + 1;

                // Уставка
                g2.setColor(Color.RED);
                g2.drawLine(x1, h - (int)(targetHistory.get(i) * scaleY), x2, h - (int)(targetHistory.get(i + 1) * scaleY));

                // Клапан
                g2.setColor(Color.CYAN);
                g2.drawLine(x1, h - (int)(valveHistory.get(i) * scaleY), x2, h - (int)(valveHistory.get(i + 1) * scaleY));

                // Температура
                g2.setColor(Color.GREEN);
                g2.drawLine(x1, h - (int)(tempHistory.get(i) * scaleY), x2, h - (int)(tempHistory.get(i + 1) * scaleY));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}