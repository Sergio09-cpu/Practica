import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;


public class BotonPersonalizado {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame().setVisible(true));
    }
}

class GameFrame extends JFrame implements KeyListener {
    CardLayout cards = new CardLayout();
    JPanel root = new JPanel(cards);

    MenuPanel menuPanel;
    GamePanel gamePanel;
    ScorePanel scorePanel;

    JMenuBar menuBar;

    public GameFrame() {
        super("Componentes - Juego interactivo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Menú personalizado con item personalizado
        menuBar = new JMenuBar();
        JMenu archivo = new JMenu("Archivo");
        CustomMenuItem miSalir = new CustomMenuItem("Salir");
        miSalir.addActionListener(e -> System.exit(0));
        CustomMenuItem miCambColor = new CustomMenuItem("Cambiar color fondo");
        miCambColor.addActionListener(e -> {
            if (gamePanel != null) gamePanel.cycleBackground();
        });
        archivo.add(miCambColor);
        archivo.addSeparator();
        archivo.add(miSalir);
        menuBar.add(archivo);
        setJMenuBar(menuBar);

        // Panels
        menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel(this);
        scorePanel = new ScorePanel(this);

        root.add(menuPanel, "menu");
        root.add(gamePanel, "game");
        root.add(scorePanel, "score");

        add(root, BorderLayout.CENTER);

        addKeyListener(this);
        setFocusable(true);

        cards.show(root, "menu");
    }

    public void showScreen(String name) {
        if ("game".equals(name)) {
            gamePanel.startGame();
            gamePanel.requestFocusInWindow();
        } else if ("menu".equals(name)) {
            gamePanel.stopGame();
        } else if ("score".equals(name)) {
            gamePanel.stopGame();
            scorePanel.updateScore(gamePanel.getScore());
        }
        cards.show(root, name);
    }

    // KeyListener simple passthrough to the active panel
    public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focused == null || focused instanceof JPanel) {
            if (cards != null) {
                for (Component c : root.getComponents()) {
                    if (c.isVisible() && c instanceof KeyListener) {
                        ((KeyListener) c).keyPressed(e);
                    }
                }
            }
        }
    }
    public void keyReleased(KeyEvent e) {}
}

// -------------------- Menu Panel --------------------
class MenuPanel extends JPanel {
    public MenuPanel(GameFrame frame) {
        setLayout(null); // coordenadas absolutas según el PDF

        setBackground(new Color(30, 40, 60));

        CustomButton btnStart = new CustomButton("Jugar");
        btnStart.setBounds(350, 180, 200, 60);
        btnStart.addActionListener(e -> frame.showScreen("game"));
        add(btnStart);

        CustomButton btnSettings = new CustomButton("Opciones");
        btnSettings.setBounds(350, 260, 200, 60);
        btnSettings.addActionListener(e -> {
            // Abrir diálogo de opciones
            OptionsDialog dlg = new OptionsDialog((JFrame) SwingUtilities.getWindowAncestor(this));
            dlg.setVisible(true);
        });
        add(btnSettings);

        // Personalizado JTextField interactivo
        CustomTextField tfName = new CustomTextField(20);
        tfName.setBounds(320, 340, 260, 36);
        add(tfName);

        // Lista con renderer personalizado
        String[] avatars = {"Rojo", "Verde", "Azul", "Naranja"};
        JList<String> lst = new JList<>(avatars);
        lst.setCellRenderer(new ColorListRenderer());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lst.setBounds(650, 180, 180, 120);
        add(lst);

        // Etiqueta instructiva
        JLabel title = new JLabel("MI MINI JUEGO", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setBounds(200, 60, 500, 60);
        add(title);

        // Pequeño banner animado (timer independiente)
        BannerLabel banner = new BannerLabel("¡Presiona Jugar para comenzar!");
        banner.setBounds(150, 420, 600, 40);
        add(banner);
    }
}

// -------------------- Game Panel --------------------
class GamePanel extends JPanel implements KeyListener {
    GameFrame parent;
    Player player;
    java.util.List<Enemy> enemies = new ArrayList<>();

    // Temporizadores
    Timer gameTimer;      // bucle de juego (colisiones, física)
    Timer enemySpawner;   // crea enemigos periódicamente
    Timer uiBlinker;      // anima UI

    int score = 0;
    boolean running = false;

    Color[] bgColors = {new Color(8,24,40), new Color(12,60,80), new Color(30,12,40)};
    int bgIndex = 0;

    Random rnd = new Random();

    public GamePanel(GameFrame parent) {
        this.parent = parent;
        setLayout(null);
        setFocusable(true);

        // Player como JLabel personalizado
        player = new Player(40, 40);
        player.setBounds(100, 200, 40, 40);
        add(player);

        // Barra superior con marcador y botón pausa
        JLabel scoreLabel = new JLabel("Score: 0");
        scoreLabel.setBounds(10, 10, 120, 30);
        scoreLabel.setForeground(Color.WHITE);
        add(scoreLabel);

        CustomButton btnPause = new CustomButton("Pausa");
        btnPause.setBounds(760, 10, 110, 30);
        btnPause.addActionListener(e -> togglePause());
        add(btnPause);

        // Dialogo flotante de ayuda (JDialog personalizado)
        CustomDialog help = new CustomDialog((JFrame) SwingUtilities.getWindowAncestor(this), false);
        JButton openHelp = new CustomButton("Ayuda");
        openHelp.setBounds(640, 10, 110, 30);
        openHelp.addActionListener(e -> help.setVisible(true));
        add(openHelp);

        // Temporizador principal: 25ms ~= 40FPS
        gameTimer = new Timer(25, e -> {
            if (!running) return;
            // Mover enemigos
            for (Enemy en : enemies) en.move();
            // Detectar colisiones
            Rectangle pr = player.getBounds();
            Iterator<Enemy> it = enemies.iterator();
            while (it.hasNext()) {
                Enemy en = it.next();
                if (pr.intersects(en.getBounds())) {
                    // Colisión detectada
                    en.markHit();
                    score += 10;
                }
                // eliminar si sale de pantalla
                if (en.getX() > getWidth() + 50) it.remove();
            }
            scoreLabel.setText("Score: " + score);
            // repintar
            repaint();
        });

        // Enemy spawner cada 900-1500ms (temporizador 1)
        enemySpawner = new Timer(900, e -> {
            if (!running) return;
            spawnEnemy();
        });

        // UI blinker (temporizador 2)
        uiBlinker = new Timer(600, e -> {
            // busca banner u otros elementos animables
            for (Component c : getComponents()) if (c instanceof Animable) ((Animable) c).tick();
            repaint();
        });
        uiBlinker.start();

        addKeyListener(this);

        // Mouse listener para mover player con click
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                player.moveTo(e.getX() - player.getWidth() / 2, e.getY() - player.getHeight() / 2);
            }
        });
    }

    public void startGame() {
        running = true;
        score = 0;
        enemies.clear();
        player.setLocation(100, 200);
        gameTimer.start();
        enemySpawner.start();
        requestFocusInWindow();
    }

    public void stopGame() {
        running = false;
        gameTimer.stop();
        enemySpawner.stop();
    }

    public void togglePause() {
        running = !running;
    }

    public int getScore() { return score; }

    public void spawnEnemy() {
        int h = 30 + rnd.nextInt(40);
        Enemy e = new Enemy(h, h);
        int y = 60 + rnd.nextInt(Math.max(1, getHeight() - 120));
        e.setLocation(-50, y);
        e.setSpeed(2 + rnd.nextInt(4));
        add(e);
        enemies.add(e);
    }

    public void cycleBackground() {
        bgIndex = (bgIndex + 1) % bgColors.length;
        repaint();
    }

    // Pintado con gradiente atractivo
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();
        Color c1 = bgColors[bgIndex];
        Color c2 = c1.brighter().brighter();
        GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Dibujar texto instructivo si no corriendo
        if (!running) {
            g2.setColor(new Color(255, 255, 255, 80));
            g2.setFont(new Font("SansSerif", Font.BOLD, 26));
            g2.drawString("Pausa - pulsa 'J' para volver a jugar o 'S' para salir", 120, getHeight() - 30);
        }

        // Dibujar colisiones: si un enemigo ha sido marcado, dibuja un pulso
        for (Enemy en : enemies) en.drawOverlay(g2);
    }

    // KeyListener para control con teclado
    public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT) player.nudge(-10, 0);
        if (code == KeyEvent.VK_RIGHT) player.nudge(10, 0);
        if (code == KeyEvent.VK_UP) player.nudge(0, -10);
        if (code == KeyEvent.VK_DOWN) player.nudge(0, 10);
        if (code == KeyEvent.VK_P) togglePause();
        if (code == KeyEvent.VK_J) startGame();
        if (code == KeyEvent.VK_S) parent.showScreen("score");
    }
    public void keyReleased(KeyEvent e) {}
}

// -------------------- Score Panel --------------------
class ScorePanel extends JPanel {
    GameFrame parent;
    JLabel lblScore;

    public ScorePanel(GameFrame parent) {
        this.parent = parent;
        setLayout(null);
        setBackground(new Color(18, 28, 48));

        JLabel title = new JLabel("Puntuación final", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setBounds(200, 40, 500, 50);
        add(title);

        lblScore = new JLabel("0", SwingConstants.CENTER);
        lblScore.setFont(new Font("Monospaced", Font.BOLD, 48));
        lblScore.setForeground(Color.ORANGE);
        lblScore.setBounds(350, 140, 200, 80);
        add(lblScore);

        CustomButton btnMenu = new CustomButton("Volver al menú");
        btnMenu.setBounds(360, 260, 180, 50);
        btnMenu.addActionListener(e -> parent.showScreen("menu"));
        add(btnMenu);
    }

    public void updateScore(int s) { lblScore.setText(String.valueOf(s)); }
}

// -------------------- Componentes personalizados --------------------
class CustomButton extends JButton implements MouseListener {
    boolean hovered = false;
    public CustomButton(String txt) {
        super(txt);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFont(new Font("SansSerif", Font.BOLD, 16));
        setForeground(Color.WHITE);
        addMouseListener(this);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        if (hovered) {
            g2.setPaint(new GradientPaint(0, 0, new Color(255,100,100), w, h, new Color(180,20,20)));
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(40,130,200), w, h, new Color(10,70,120)));
        }
        g2.fillRoundRect(0, 0, w, h, 18, 18);
        g2.setColor(new Color(255,255,255,30));
        g2.fillRoundRect(3, 3, w-6, h-6, 14, 14);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String s = getText();
        int sw = fm.stringWidth(s);
        int sh = fm.getAscent();
        g2.drawString(s, (w-sw)/2, (h+sh)/2-4);
        g2.dispose();
    }

    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
}

class CustomTextField extends JTextField implements DocumentListener, FocusListener {
    public CustomTextField(int cols) {
        super(cols);
        getDocument().addDocumentListener(this);
        addFocusListener(this);
        setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    public void insertUpdate(DocumentEvent e) { updateBg(); }
    public void removeUpdate(DocumentEvent e) { updateBg(); }
    public void changedUpdate(DocumentEvent e) { updateBg(); }

    private void updateBg() {
        int len = getText().length();
        // menor texto -> fondo claro, mayor texto -> fondo oscuro
        int v = Math.min(200, 255 - len * 4);
        setBackground(new Color(v, v, 255));
    }

    public void focusGained(FocusEvent e) { setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2)); }
    public void focusLost(FocusEvent e) { setBorder(null); }
}

class BannerLabel extends JLabel implements Animable {
    int phase = 0;
    public BannerLabel(String text) {
        super(text, SwingConstants.CENTER);
        setFont(new Font("SansSerif", Font.PLAIN, 18));
        setForeground(Color.WHITE);
    }
    public void tick() { phase = (phase + 1) % 4; setVisible(phase != 0); }
}

interface Animable { void tick(); }

// Custom JDialog
class CustomDialog extends JDialog {
    public CustomDialog(JFrame owner, boolean modal) {
        super(owner, "Dialogo de ayuda", modal);
        setSize(360, 200);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        JLabel txt = new JLabel("Usa las flechas o usa el ratón. Evita enemigos.", SwingConstants.CENTER);
        add(txt, BorderLayout.CENTER);
        CustomButton close = new CustomButton("Cerrar");
        close.addActionListener(e -> setVisible(false));
        JPanel p = new JPanel(); p.add(close);
        add(p, BorderLayout.SOUTH);
    }
}

class OptionsDialog extends JDialog {
    public OptionsDialog(JFrame owner) {
        super(owner, "Opciones", false);
        setSize(420, 240);
        setLocationRelativeTo(owner);
        setLayout(null);

        JLabel lbl = new JLabel("Velocidad de enemigos:");
        lbl.setBounds(20, 20, 200, 25);
        add(lbl);

        JSlider sld = new JSlider(1, 10, 4);
        sld.setBounds(20, 50, 360, 40);
        add(sld);

        CustomButton ok = new CustomButton("Aceptar");
        ok.setBounds(240, 160, 120, 36);
        ok.addActionListener(e -> setVisible(false));
        add(ok);
    }
}

class CustomMenuItem extends JMenuItem {
    public CustomMenuItem(String t) {
        super(t);
        setFont(new Font("SansSerif", Font.PLAIN, 13));
    }
}

// -------------------- Sprites y enemigos --------------------
class Player extends JLabel {
    Color color = new Color(80, 200, 160);
    public Player(int w, int h) {
        setSize(w, h);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, getWidth(), getHeight());
        g2.setColor(color.darker());
        g2.drawOval(0, 0, getWidth()-1, getHeight()-1);
    }

    public void nudge(int dx, int dy) {
        setLocation(Math.max(0, getX() + dx), Math.max(50, getY() + dy));
    }

    public void moveTo(int x, int y) { setLocation(x, y); }
}

class Enemy extends JLabel {
    private Color color = new Color(220, 80, 80);
    private int speed = 3;
    private boolean hit = false;
    private int pulse = 0;

    public Enemy(int w, int h) { setSize(w, h); }

    public void setSpeed(int s) { speed = s; }

    public void move() { setLocation(getX() + speed, getY()); }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(color.darker());
        g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
    }

    public void markHit() { hit = true; pulse = 8; }

    public void drawOverlay(Graphics2D g2) {
        if (!hit) return;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        g2.setColor(new Color(255, 255, 0, 120));
        g2.fillOval(x - pulse, y - pulse, w + pulse*2, h + pulse*2);
        pulse++;
        if (pulse > 40) hit = false;
    }
}

// -------------------- JList renderer personalizado --------------------
class ColorListRenderer extends JLabel implements ListCellRenderer<String> {
    public ColorListRenderer() { setOpaque(true); setBorder(BorderFactory.createEmptyBorder(4,4,4,4)); }
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(value);
        Color c = switch (index % 4) {
            case 0 -> Color.RED;
            case 1 -> Color.GREEN.darker();
            case 2 -> Color.BLUE;
            default -> Color.ORANGE;
        };
        setBackground(isSelected ? c.brighter() : list.getBackground());
        setForeground(isSelected ? Color.WHITE : c.darker());
        return this;
    }
}
