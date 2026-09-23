import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    // Переменные модели
    private double g = 70.0;
    private double s = 0.0;
    private double omega = 0.0;
    private double phi = 0.0;
    private double T = 20.0;
    private double OC = 0.0;

    private double P_priv = 0.8;
    private double n = 1.0;
    private double P_kotel = 0.5;
    private double k_os = 1.0;
    private final double tau = 0.05;

    // Элементы UI
    private JTextField txtG, txtOmega, txtPhi, txtT, txtError;
    private JSlider sliderPpriv, sliderN, sliderPkotel;
    private JLabel lblPpriv, lblN, lblPkotel;
    private JButton btnStart;
    private Timer timer;
    private boolean isRunning = false;

    private GraphPanel graphPanel;

    public MainFrame() {
        setTitle("Система автоматического управления котла (САУ с ОС)");
        setSize(900, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel controlsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Уставка
        JPanel col1 = new JPanel(new GridLayout(3, 1, 5, 5));
        col1.setBorder(BorderFactory.createTitledBorder("Регулятор"));
        txtG = new JTextField(String.valueOf(g));
        btnStart = new JButton("Пуск");
        col1.add(new JLabel("Уставка t (°C):"));
        col1.add(txtG);
        col1.add(btnStart);
        controlsPanel.add(col1);

        // 2. Мотор
        JPanel col2 = new JPanel(new GridLayout(4, 1, 5, 5));
        col2.setBorder(BorderFactory.createTitledBorder("Мотор привода"));
        lblPpriv = new JLabel(String.format("Мощность (P): %.1f", P_priv));
        sliderPpriv = new JSlider(1, 20, (int)(P_priv * 10));
        txtOmega = new JTextField("0.0");
        txtOmega.setEditable(false);
        col2.add(lblPpriv);
        col2.add(sliderPpriv);
        col2.add(new JLabel("Скорость (ω):"));
        col2.add(txtOmega);
        controlsPanel.add(col2);

        // 3. Клапан
        JPanel col3 = new JPanel(new GridLayout(4, 1, 5, 5));
        col3.setBorder(BorderFactory.createTitledBorder("Топливный клапан"));
        lblN = new JLabel(String.format("Передача (n): %.1f", n));
        sliderN = new JSlider(1, 20, (int)(n * 10));
        txtPhi = new JTextField("0.0");
        txtPhi.setEditable(false);
        col3.add(lblN);
        col3.add(sliderN);
        col3.add(new JLabel("Положение (φ):"));
        col3.add(txtPhi);
        controlsPanel.add(col3);

        // 4. Котёл & Качество
        JPanel col4 = new JPanel(new GridLayout(4, 1, 5, 5));
        col4.setBorder(BorderFactory.createTitledBorder("Котёл & Качество"));
        lblPkotel = new JLabel(String.format("Мощность котла: %.1f", P_kotel));
        sliderPkotel = new JSlider(1, 20, (int)(P_kotel * 10));
        txtT = new JTextField(String.valueOf(T));
        txtT.setEditable(false);
        txtError = new JTextField("0.0");
        txtError.setEditable(false);
        col4.add(lblPkotel);
        col4.add(sliderPkotel);
        col4.add(new JLabel("Температура (T):"));
        col4.add(txtT);
        controlsPanel.add(col4);

        add(controlsPanel, BorderLayout.NORTH);

        // Слушатели ползунков (обновляют надписи с цифрами при движении)
        sliderPpriv.addChangeListener(e -> {
            P_priv = sliderPpriv.getValue() / 10.0;
            lblPpriv.setText(String.format("Мощность (P): %.1f", P_priv));
        });
        sliderN.addChangeListener(e -> {
            n = sliderN.getValue() / 10.0;
            lblN.setText(String.format("Передача (n): %.1f", n));
        });
        sliderPkotel.addChangeListener(e -> {
            P_kotel = sliderPkotel.getValue() / 10.0;
            lblPkotel.setText(String.format("Мощность котла: %.1f", P_kotel));
        });

        // График
        graphPanel = new GraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        // Таймер
        timer = new Timer((int)(tau * 1000), e -> stepSimulation());

        btnStart.addActionListener(e -> {
            if (!isRunning) {
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
        // Читаем уставку прямо на ходу
        try {
            g = Double.parseDouble(txtG.getText());
        } catch (Exception ignored) {}

        // Математика САУ
        OC = k_os * T;
        s = g - OC;
        omega = omega + (s - omega) * P_priv * tau;
        phi = phi + (omega * n * tau);
        if (phi < 0) phi = 0; // кран не может закрыться сильнее нуля
        T = T + (phi - T) * P_kotel * tau;

        // Обновление цифр в полях
        txtOmega.setText(String.format("%.2f", omega));
        txtPhi.setText(String.format("%.2f", phi));
        txtT.setText(String.format("%.2f", T));
        txtError.setText(String.format("%.2f", Math.abs(s)));

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

            if (targetHistory.size() > 800) {
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

            // Легенда
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(Color.RED);
            g2.drawString("— Уставка (g)", 20, 20);
            g2.setColor(Color.GREEN);
            g2.drawString("— Температура котла (T)", 140, 20);
            g2.setColor(Color.CYAN);
            g2.drawString("— Положение клапана (φ)", 330, 20);

            if (tempHistory.size() < 2) return;

            double scaleY = h / 130.0;

            for (int i = 0; i < tempHistory.size() - 1; i++) {
                int x1 = i;
                int x2 = i + 1;

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