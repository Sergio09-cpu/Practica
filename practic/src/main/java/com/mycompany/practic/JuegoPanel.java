package com.mycompany.practic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

// Panel principal del juego
public class JuegoPanel extends JPanel implements KeyListener {

    private final CardLayout layout; // Para cambiar entre menú y juego
    private final JPanel contenedor;

    private final Jugador jugador; // El jugador
    private final java.util.List<Enemigo> enemigos = new ArrayList<>(); // Lista de enemigos

    private final Timer timerJuego; // Timer para lógica del juego
    private final Timer timerUI; // Timer para efectos UI (banner parpadeante)

    private boolean corriendo = false; // Indica si el juego está activo
    private JLabel lblEstado; // Etiqueta para mostrar mensajes
    private BannerLabel banner; // Banner parpadeante

    private String nombre = "Jugador"; // Nombre del jugador

    private final Random rnd = new Random(); // Para posiciones aleatorias de enemigos

    // Constructor del panel
    public JuegoPanel(CardLayout layout, JPanel contenedor) {
        this.layout = layout;
        this.contenedor = contenedor;

        setLayout(null); // Layout absoluto
        setBackground(new Color(10, 40, 60)); // Color de fondo
        setFocusable(true); // Permitir recibir teclado
        addKeyListener(this); // Escuchar teclas

        // Crear jugador
        jugador = new Jugador(50, 50);
        jugador.setBounds(100, 200, 50, 50); // Posición inicial
        add(jugador);

        // Etiqueta de estado
        lblEstado = new JLabel("Objetivo: Toca los objetos", SwingConstants.LEFT);
        lblEstado.setForeground(Color.WHITE);
        lblEstado.setBounds(20, 20, 600, 30);
        add(lblEstado);

        // Botón para volver al menú
        BotonPersonalizado btnVolver = new BotonPersonalizado("Volver");
        btnVolver.setBounds(750, 20, 100, 40);
        btnVolver.addActionListener(e -> {
            parar(); // Detener juego
            layout.show(contenedor, "menu"); // Volver al menú
        });
        add(btnVolver);

        // Banner parpadeante
        banner = new BannerLabel("gana tocando los objetos");
        banner.setBounds(200, 520, 500, 30);
        add(banner);

        // Timer para actualizar juego
        timerJuego = new Timer(30, e -> actualizar());
        // Timer para animaciones UI
        timerUI = new Timer(600, e -> { banner.tick(); repaint(); });
        timerUI.start();
    }

    // Guardar nombre del jugador
    public void setNombreJugador(String nombre) {
        if (!nombre.isBlank()) this.nombre = nombre;
    }

    // Iniciar juego
    public void iniciar() {
        enemigos.clear();
        for (Component c : getComponents())
            if (c instanceof Enemigo) remove(c); // Quitar enemigos viejos

        jugador.setLocation(100, 200); // Posición inicial del jugador
        corriendo = true;

        lblEstado.setText("Hola " + nombre + "! Objetivo: Toca los 2 objetos");

        // Crear dos enemigos
        crearEnemigo();
        crearEnemigo();

        timerJuego.start(); // Arrancar timer de juego
        requestFocusInWindow(); // Recibir teclado
    }

    // Detener juego
    public void parar() {
        corriendo = false;
        timerJuego.stop();
    }

    // Actualizar lógica de juego
    private void actualizar() {
        if (!corriendo) return;

        Rectangle rj = jugador.getBounds();
        Iterator<Enemigo> it = enemigos.iterator();

        while (it.hasNext()) {
            Enemigo en = it.next();

            // Detectar colisión
            if (!en.isImpactado() && rj.intersects(en.getBounds())) {
                en.marcarImpacto(); // Cambiar color y animar
            }
            if (en.isListoParaEliminar()) {
                remove(en); // Quitar enemigo del panel
                it.remove(); // Quitar enemigo de la lista
            }
        }

        // Si no hay enemigos, gana el jugador
        if (enemigos.isEmpty()) {
            corriendo = false;
            timerJuego.stop();
            lblEstado.setText("ganaste" + nombre );
        }

        repaint(); // Redibujar panel
    }

    // Crear enemigo en posición aleatoria
    private void crearEnemigo() {
        Enemigo en = new Enemigo(50, 50);
        en.setLocation(180 + rnd.nextInt(500), 100 + rnd.nextInt(300));
        add(en);
        enemigos.add(en);
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    // Movimiento jugador con flechas
    @Override
    public void keyPressed(KeyEvent e) {
        if (!corriendo) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> jugador.mover(0, -10);
            case KeyEvent.VK_DOWN -> jugador.mover(0, 10);
            case KeyEvent.VK_LEFT -> jugador.mover(-10, 0);
            case KeyEvent.VK_RIGHT -> jugador.mover(10, 0);
        }
    }
}

// componentes personalizados

// Jugador: cuadrado amarillo que se mueve
class Jugador extends JComponent {
    private Color color = new Color(255, 230, 40);
    private final Timer anim; // Timer para efecto visual

    public Jugador(int w, int h) {
        setSize(w, h);
        setOpaque(false);

        // Timer para pulso visual
        anim = new Timer(200, e -> {
            color = color.brighter().darker(); // Pulso leve
            repaint();
        });
        anim.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(color);
        g2.fillRect(0, 0, w, h); // Dibujar cuadrado

        g2.setColor(Color.BLACK);
        g2.drawRect(0, 0, w - 1, h - 1); // Bordes

        g2.dispose();
    }

    // Mover jugador dentro de límites del panel
    public void mover(int dx, int dy) {
        if (getParent() == null) return;
        int nx = Math.max(0, Math.min(getParent().getWidth() - getWidth(), getX() + dx));
        int ny = Math.max(50, Math.min(getParent().getHeight() - getHeight() - 30, getY() + dy));
        setLocation(nx, ny);
    }

    public void setColor(Color c) {
        this.color = c;
        repaint();
    }
}

// Enemigo: cambia de color al impactar
class Enemigo extends JComponent {

    private boolean impacto = false;
    private int radio = 0;
    private Color colorActual = new Color(255, 80, 80);
    private final Random rnd = new Random();

    public Enemigo(int w, int h) {
        setSize(w, h);
        setOpaque(false);
    }

    public boolean isImpactado() { return impacto; }
    public boolean isListoParaEliminar() { return !impacto && radio > 40; }

    // Marcar impacto
    public void marcarImpacto() {
        impacto = true;
        radio = 8;

        colorActual = new Color(
                50 + rnd.nextInt(205),
                50 + rnd.nextInt(205),
                50 + rnd.nextInt(205)
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        if (!impacto) {
            g2.setColor(colorActual);
            g2.fillOval(0, 0, getWidth(), getHeight());
        } else {
            g2.setColor(colorActual);
            g2.fillOval(0, 0, getWidth(), getHeight());

            // Efecto circular de impacto
            g2.setColor(new Color(255, 255, 0, 120));
            g2.fillOval(-radio, -radio, getWidth() + radio * 2, getHeight() + radio * 2);

            radio += 3;
            if (radio > 40) impacto = false;
        }

        g2.dispose();
    }
}

// Botón con gradiente y efecto hover
class BotonPersonalizado extends JButton implements MouseListener {
    private boolean encima = false;

    public BotonPersonalizado(String texto) {
        super(texto);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 14));
        addMouseListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        // Gradiente según hover
        g2.setPaint(encima
                ? new GradientPaint(0,0,new Color(255,120,120),w,h,new Color(180,20,20))
                : new GradientPaint(0,0,new Color(40,130,200),w,h,new Color(10,70,120)));

        g2.fillRoundRect(0,0,w,h,16,16); // Botón redondeado

        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), (w - fm.stringWidth(getText()))/2, (h + fm.getAscent())/2 - 2);

        g2.dispose();
    }

    public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
    public void mouseExited(MouseEvent e) { encima = false; repaint(); }
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
}

// Campo de texto con borde personalizado
class CampoTextoPersonalizado extends JTextField {
    public CampoTextoPersonalizado(int cols) {
        super(cols);
        setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
    }
}

// Banner que parpadea
class BannerLabel extends JLabel {
    private int fase = 0;
    public BannerLabel(String txt) {
        super(txt, SwingConstants.CENTER);
        setForeground(Color.WHITE);
    }
    public void tick() {
        fase = (fase + 1) % 4;
        setForeground(new Color(255,255,255,(fase==0?60:255))); // Transparencia variable
    }
}