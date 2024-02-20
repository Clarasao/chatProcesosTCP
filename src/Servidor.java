import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Servidor extends JFrame {

    private JTextArea mensajesArea;
    private List<PrintWriter> clientes = new ArrayList<>();
    private Set<String> nombresUtilizados = new HashSet<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Servidor servidor = new Servidor();
            servidor.iniciarServidor();
        });
    }

    private void iniciarServidor() {
        mensajesArea = new JTextArea();
        mensajesArea.setEditable(false);
        mensajesArea.append("***BIENVENID@ SERVIDOR***\n");

        add(new JScrollPane(mensajesArea), BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setVisible(true);

        new Thread(this::iniciarServerSocket).start();
    }

    private void iniciarServerSocket() {
        try (ServerSocket serverSocket = new ServerSocket(5555)) {
            System.out.println("Servidor iniciado. Esperando conexiones...");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Nuevo cliente conectado.");

                PrintWriter escritor = new PrintWriter(socketCliente.getOutputStream(), true);

                // Pedir al cliente que ingrese su nombre
                escritor.println("Ingrese su nombre:");
                String nombreCliente = new BufferedReader(new InputStreamReader(socketCliente.getInputStream())).readLine();

                // Verificar si el nombre ya está en uso
                if (nombreEstaEnUso(nombreCliente)) {
                    // Enviar mensaje de error al cliente
                    escritor.println("#NOMBRE_EN_USO#");
                    socketCliente.close();
                    continue;
                }

                // Marcar el nombre como utilizado
                nombresUtilizados.add(nombreCliente);

                broadcastMensaje("Bienvenido, " + nombreCliente + "!");

                // Iniciar un nuevo hilo para manejar al cliente
                new Thread(() -> manejarCliente(socketCliente, escritor, nombreCliente)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean nombreEstaEnUso(String nombre) {
        return nombresUtilizados.contains(nombre);
    }

    private void manejarCliente(Socket socket, PrintWriter escritor, String nombreCliente) {
        try {
            BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String mensaje;
            while ((mensaje = lector.readLine()) != null) {
                if (mensaje.equals("#CLIENTE_ENVIO_MENSAJE#")) {
                    // Mensaje especial para indicar que un cliente envió un mensaje
                    mensaje = lector.readLine();
                    broadcastMensaje(nombreCliente + ": " + mensaje);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (escritor != null) {
                clientes.remove(escritor);
                nombresUtilizados.remove(nombreCliente);
                broadcastMensaje(nombreCliente + " se ha desconectado.");
            }
        }
    }

    private void broadcastMensaje(String mensaje) {
        System.out.println(mensaje);
        SwingUtilities.invokeLater(() -> {
            mensajesArea.append(mensaje + "\n");
            mensajesArea.setCaretPosition(mensajesArea.getDocument().getLength());
        });

        for (PrintWriter cliente : clientes) {
            try {
                cliente.println(mensaje);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
