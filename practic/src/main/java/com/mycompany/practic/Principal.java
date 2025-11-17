package com.mycompany.practic;

import javax.swing.*;
import java.awt.*;

// Clase principal que inicia el programa
public class Principal {

    public static void main(String[] args) {
        // Ejecuta la interfaz gráfica en el hilo correcto de Swing
        SwingUtilities.invokeLater(() -> {
            // Crear ventana principal
            JFrame ventana = new JFrame("Juego Interactivo - Componentes");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Cierra la app al cerrar la ventana
            ventana.setSize(900, 600); // Tamaño de la ventana
            ventana.setLocationRelativeTo(null); // Centrar ventana

            // Panel contenedor con CardLayout para cambiar entre pantallas
            CardLayout layout = new CardLayout();
            JPanel contenedor = new JPanel(layout);

            // Panel del juego
            JuegoPanel juego = new JuegoPanel(layout, contenedor);

            //menu
            JPanel menu = new JPanel(null); // Layout absoluto
            menu.setBackground(new Color(20, 30, 60)); // Fondo azul oscuro

            // Título del juego
            JLabel titulo = new JLabel("mi minijuego", SwingConstants.CENTER);
            titulo.setFont(new Font("SansSerif", Font.BOLD, 36));
            titulo.setForeground(Color.WHITE);
            titulo.setBounds(200, 60, 500, 60); // Posición y tamaño
            menu.add(titulo);

            // Etiqueta para pedir nombre
            JLabel lblNombre = new JLabel("Introduce tu nombre:", SwingConstants.CENTER);
            lblNombre.setForeground(Color.WHITE);
            lblNombre.setBounds(340, 290, 220, 30);
            menu.add(lblNombre);

            // Campo de texto personalizado para nombre del jugador
            CampoTextoPersonalizado campoNombre = new CampoTextoPersonalizado(20);
            campoNombre.setBounds(340, 320, 220, 30);
            menu.add(campoNombre);

            // Botón para iniciar el juego
            BotonPersonalizado btnJugar = new BotonPersonalizado("Jugar");
            btnJugar.setBounds(350, 200, 200, 60);
            btnJugar.addActionListener(e -> {
                juego.setNombreJugador(campoNombre.getText().trim()); // Guardar nombre
                layout.show(contenedor, "juego"); // Cambiar a panel de juego
                juego.iniciar(); // Iniciar lógica del juego
                juego.requestFocusInWindow(); // Que el panel reciba teclado
            });
            menu.add(btnJugar);

            // Agregar paneles al contenedor
            contenedor.add(menu, "menu");
            contenedor.add(juego, "juego");

            // Agregar contenedor a la ventana y mostrar
            ventana.add(contenedor);
            ventana.setVisible(true);
            layout.show(contenedor, "menu"); // Mostrar menú al inicio
        });
    }
}
