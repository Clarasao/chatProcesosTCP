import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Servidor extends JFrame {

    private JTextArea areaMensajes;
    private List<PrintWriter> clientesConectados = new ArrayList<>();
    private List<String> nombresUtilizados = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Servidor servidor = new Servidor();
            servidor.iniciarServidor();
        });
    }

    private void iniciarServidor() {
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.append("***BIENVENIDO AL SERVIDOR***\n");

        add(new JScrollPane(areaMensajes), BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setVisible(true);

        new Thread(this::iniciarConexionServidor).start();
    }

    private void iniciarConexionServidor() {
        try (ServerSocket serverSocket = new ServerSocket(5555)) {
            System.out.println("El servidor ha sido iniciado. Esperando conexiones...");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Nuevo cliente conectado.");

                PrintWriter escritor = new PrintWriter(socketCliente.getOutputStream(), true);

                escritor.println("Por favor, ingrese su nombre:");
                String nombreCliente = new BufferedReader(new InputStreamReader(socketCliente.getInputStream())).readLine();

                if (nombreEstaEnUso(nombreCliente)) {
                    escritor.println("#NOMBRE_EN_USO#");
                    socketCliente.close();
                    continue;
                }

                nombresUtilizados.add(nombreCliente);
                broadcastMensaje(obtenerFechaHoraActual() + " - " + nombreCliente + " se ha unido al chat.");

                enviarListaClientesConectados();

                new Thread(() -> manejarCliente(socketCliente, escritor, nombreCliente)).start();

                clientesConectados.add(escritor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarListaClientesConectados() {
        for (PrintWriter cliente : clientesConectados) {
            cliente.println("#LISTA_CLIENTES#");
            for (String nombre : nombresUtilizados) {
                cliente.println(nombre);
            }
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
                    mensaje = lector.readLine();
                    broadcastMensaje(obtenerFechaHoraActual() + " - " + nombreCliente + ": " + mensaje);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (escritor != null) {
                clientesConectados.remove(escritor);
                nombresUtilizados.remove(nombreCliente);
                broadcastMensaje(obtenerFechaHoraActual() + " - " + nombreCliente + " se ha desconectado.");
                enviarListaClientesConectados();
            }
        }
    }

    private void broadcastMensaje(String mensaje) {
        System.out.println(mensaje);
        SwingUtilities.invokeLater(() -> {
            areaMensajes.append(mensaje + "\n");
            areaMensajes.setCaretPosition(areaMensajes.getDocument().getLength());
        });

        for (PrintWriter cliente : clientesConectados) {
            try {
                cliente.println(mensaje);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String obtenerFechaHoraActual() {
        SimpleDateFormat formatoFechaHora = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return formatoFechaHora.format(new Date());
    }
}