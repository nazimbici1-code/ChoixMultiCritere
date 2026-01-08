package Sad;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;

public class Serveur {

    private static final DecimalFormat DF = new DecimalFormat("#.####");

    public static void main(String[] args) {
        int port = 3000;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Serveur ELECTRE I lancé sur le port " + port);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client connecté : " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket)).start();
            }

        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
            ) {
                double[][] data = (double[][]) ois.readObject();
                double[] poids = (double[]) ois.readObject();

                double[][] concordance = calculConcordance(data, poids);
                double[][] discordance = calculDiscordance(data);

                oos.writeObject(concordance);
                oos.writeObject(discordance);
                oos.flush();

            } catch (Exception ex) {
                System.err.println("Erreur client : " + ex.getMessage());
                ex.printStackTrace();

            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static double[][] calculConcordance(double[][] data, double[] poids) {
        int n = data.length;
        int m = data[0].length;
        double[][] concordance = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;

                double somme = 0.0;

                for (int k = 0; k < m; k++) {
                    if (data[i][k] >= data[j][k]) {
                        somme += poids[k];
                    }
                }

                concordance[i][j] = Double.parseDouble(DF.format(somme).replace(',', '.'));
            }
        }

        return concordance;
    }

    public static double[][] calculDiscordance(double[][] data) {
        int n = data.length;
        int m = data[0].length;

        double[][] discordance = new double[n][n];

        double globalMaxDiff = 0.0;

        for (int a = 0; a < n; a++)
            for (int b = 0; b < n; b++)
                for (int k = 0; k < m; k++)
                    globalMaxDiff = Math.max(globalMaxDiff, Math.abs(data[a][k] - data[b][k]));

        if (globalMaxDiff == 0) globalMaxDiff = 1.0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;

                double maxDiff = 0.0;

                for (int k = 0; k < m; k++) {
                    if (data[j][k] > data[i][k]) {
                        double diff = Math.abs(data[i][k] - data[j][k]);
                        maxDiff = Math.max(maxDiff, diff);
                    }
                }

                discordance[i][j] = Double.parseDouble(
                        DF.format(maxDiff / globalMaxDiff).replace(',', '.')
                );
            }
        }

        return discordance;
    }
}


