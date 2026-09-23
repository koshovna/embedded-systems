import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    // --- Переменные моделирования ---
    private double g = 70.0;       // Уставка температуры
    private double s = 0.0;        // Сигнал рассогласования
    private double omega = 0.0;    // Скорость привода
    private double phi = 0.0;      // Положение клапана
    private double T = 20.0;       // Начальная температура котла
    private double OC = 0.0;       // Сигнал обратной связи

    // Коэффициенты
    private double P_priv = 0.8;   // Мощность привода
    private double n = 1.0;        // Передаточное число
    private double P_kotel = 0.5;  // Мощность котла
    private double k_os = 1.0;     // Коэффициент датчика ОС
    private final double tau = 0.05; // Шаг времени (dt в секундах)

    // Элементы управления
    private JTextField txtG, txtOmega, txtPhi, txtT, txtError;
    private JSlider sliderPpriv, sliderN, sliderPkotel, sliderKos;
    private JButton btnStart;
    private Timer timer;
    private boolean isRunning = false;

    // Панель для графика
    private GraphPanel graphPanel;

    public MainFrame() {
        setTitle("Система автоматического управления котла (САУ с ОС)");
        setSize(850, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Верхняя панель с параметрами (как на рисунке 5)
        JPanel controlsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Колонка 1: Уставка и Старт
        JPanel col1 = new JPanel(new GridLayout(3, 1, 5, 5));
        col1.setBorder(BorderFactory.createTitledBorder("Регулятор"));
        txtG = new JTextField(String.valueOf(g));
        btnStart = new JButton("Пуск");
        col1.add(new JLabel("Уставка t (°C):"));
        col1.add(txtG);
        col1.add(btnStart);
        controlsPanel.add(col1);

        // Колонка 2: Мотор (Привод)
        JPanel col2 = new JPanel(new GridLayout(4, 1, 5, 5));
        col2.setBorder(BorderFactory.createTitledBorder("Мотор привода"));
        sliderPpriv = new JSlider(1, 20, (int)(P_priv * 10));
        txtOmega = new JTextField("0.0");
        txtOmega.setEditable(false);
        col2.add(new JLabel("Мощность (P):"));
        col2.add(sliderPpriv);
        col2.add(new JLabel("Скорость (ω):"));
        col2.add(txtOmega);
        controlsPanel.add(col2);

        // Колонка 3: Клапан
        JPanel col3 = new JPanel(new GridLayout(4, 1, 5, 5));
        col3.setBorder(BorderFactory.createTitledBorder("Топливный клапан"));
        sliderN = new JSlider(1, 20, (int)(n * 10));
        txtPhi = new JTextField("0.0");
        txtPhi.setEditable(false);
        col3.add(new JLabel("Передача (n):"));
        col3.add(sliderN);
        col3.add(new JLabel("Положение (φ):"));
        col3.add(txtPhi);
        controlsPanel.add(col3);

        // Колонка 4: Котёл и Качество
        JPanel col4 = new JPanel(new GridLayout(4, 1, 5, 5));
        col4.setBorder(BorderFactory.createTitledBorder("Котёл & Качество"));
        sliderPkotel = new JSlider(1, 20, (int)(P_kotel * 10));
        txtT = new JTextField(String.valueOf(T));
        txtT.setEditable(false);
        txtError = new JTextField("0.0");
        txtError.setEditable(false);
        col4.add(new JLabel("Температура (T):"));
        col4.add(txtT);
        col4.add(new JLabel("Ошибка (s):"));
        col4.add(txtError);
        controlsPanel.add(col4);

        add(controlsPanel, BorderLayout.NORTH);

        // 2. График (черная панель внизу)
        graphPanel = new GraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        // 3. Таймер физического моделирования (шаг 50 мс)
        timer = new Timer((int)(tau * 1000), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stepSimulation();
            }
        });

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

    // --- Шаг дискретного моделирования системы ---
    private void stepSimulation() {
        // Считываем ползунки
        P_priv = sliderPpriv.getValue() / 10.0;
        n = sliderN.getValue() / 10.0;
        P_kotel = sliderPkotel.getValue() / 10.0;

        // 1. Датчик ОС: OC = k * T
        OC = k_os * T;

        // 2. Регулятор: s = g - OC
        s = g - OC;

        // 3. Привод: w = w0 + (s - w0) * P * tau
        omega = omega + (s - omega) * P_priv * tau;

        // 4. Клапан: phi = phi0 + w * n * tau
        phi = phi + (omega * n * tau);
        // Физическое ограничение хода клапана (не может быть меньше 0)
        if (phi < 0) phi = 0;

        // 5. Котёл: T = T0 + (phi - T0) * P * tau
        T = T + (phi - T) * P_kotel * tau;

        // Обновляем текстовые поля
        txtOmega.setText(String.format("%.2f", omega));
        txtPhi.setText(String.format("%.2f", phi));
        txtT.setText(String.format("%.2f", T));
        txtError.setText(String.format("%.2f", Math.abs(s)));

        // Добавляем точки на график
        graphPanel.addPoints(g, T, phi);
    }

    // --- Класс отрисовки графика ---
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

            // Ограничение истории по ширине окна
            if (targetHistory.size() > 700) {
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

            // Отрисовка сетки
            g2.setColor(new Color(40, 40, 40));
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);
            for (int x = 0; x < w; x += 50) g2.drawLine(x, 0, x, h);

            // Легенда
            g2.setColor(Color.RED);
            g2.drawString("— Уставка (g)", 20, 20);
            g2.setColor(Color.GREEN);
            g2.drawString("— Температура котла (T)", 130, 20);
            g2.setColor(Color.CYAN);
            g2.drawString("— Положение клапана (φ)", 310, 20);

            if (tempHistory.size() < 2) return;

            // Масштабирование (100 градусов = высота панели)
            double scaleY = h / 120.0;

            for (int i = 0; i < tempHistory.size() - 1; i++) {
                int x1 = i;
                int x2 = i + 1;

                // Уставка (Красный)
                g2.setColor(Color.RED);
                g2.drawLine(x1, h - (int)(targetHistory.get(i) * scaleY), x2, h - (int)(targetHistory.get(i + 1) * scaleY));

                // Температура (Зеленый)
                g2.setColor(Color.GREEN);
                g2.drawLine(x1, h - (int)(tempHistory.get(i) * scaleY), x2, h - (int)(tempHistory.get(i + 1) * scaleY));

                // Клапан (Голубой)
                g2.setColor(Color.CYAN);
                g2.drawLine(x1, h - (int)(valveHistory.get(i) * scaleY), x2, h - (int)(valveHistory.get(i + 1) * scaleY));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}