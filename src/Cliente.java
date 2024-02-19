import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente extends JFrame {

    private JTextArea mensajesArea;
    private JTextField mensajeField;
    private PrintWriter escritor;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente());
    }

    public Cliente() {
        super("Cliente de Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);

        mensajesArea = new JTextArea();
        mensajesArea.setEditable(false);

        mensajeField = new JTextField();

        JButton enviarButton = new JButton("Enviar");
        enviarButton.addActionListener(e -> enviarMensaje());

        JButton salirButton = new JButton("Salir");
        salirButton.addActionListener(e -> desconectarCliente());

        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new FlowLayout());
        panelBotones.add(enviarButton);
        panelBotones.add(salirButton);

        add(new JScrollPane(mensajesArea), BorderLayout.CENTER);
        add(mensajeField, BorderLayout.NORTH);
        add(panelBotones, BorderLayout.SOUTH);

        setVisible(true);

        conectarAlServidor();
    }

    private void conectarAlServidor() {
        try {
            Socket socket = new Socket("localhost", 5555);
            BufferedReader lectorServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            escritor = new PrintWriter(socket.getOutputStream(), true);

            // Pedir al usuario que ingrese su nombre
            String nombreCliente = JOptionPane.showInputDialog("Ingrese su nombre:");
            escritor.println(nombreCliente);

            // Iniciar un nuevo hilo para recibir mensajes del servidor
            new Thread(() -> recibirMensajesServidor(lectorServidor)).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recibirMensajesServidor(BufferedReader lectorServidor) {
        try {
            String mensaje;
            while ((mensaje = lectorServidor.readLine()) != null) {
                appendMensaje(mensaje);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarMensaje() {
        String mensaje = mensajeField.getText();
        if (!mensaje.isEmpty()) {
            escritor.println("#CLIENTE_ENVIO_MENSAJE#");
            escritor.println(mensaje);
            mensajeField.setText("");
        }
    }

    private void desconectarCliente() {
        escritor.println("#CLIENTE_DESCONECTAR#");
        System.exit(0);
    }

    private void appendMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> mensajesArea.append(mensaje + "\n"));
    }
}
