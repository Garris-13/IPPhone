package controller;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioMessageReceiver {
    private ServerSocket serverSocket;
    private boolean isReceiving = false;
    private String saveDirectory = "received_messages";

    public void startReceiving(int port) {
        if (isReceiving) {
            return;
        }

        new Thread(() -> {
            try {
                createSaveDirectory();
                serverSocket = new ServerSocket(port);
                isReceiving = true;

                System.out.println("音频消息接收器启动，端口: " + port);

                while (isReceiving) {
                    Socket clientSocket = serverSocket.accept();
                    handleIncomingMessage(clientSocket);
                }

            } catch (IOException e) {
                System.out.println("接收器错误: " + e.getMessage());
            }
        }).start();
    }

    public void stopReceiving() {
        isReceiving = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createSaveDirectory() {
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void handleIncomingMessage(Socket clientSocket) {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                InputStream inputStream = clientSocket.getInputStream();

                String messageType = in.readLine();
                if ("AUDIO_MESSAGE".equals(messageType)) {
                    receiveAudioFile(in, inputStream);
                }

                clientSocket.close();
            } catch (IOException e) {
                System.out.println("处理消息失败: " + e.getMessage());
            }
        }).start();
    }

    private void receiveAudioFile(BufferedReader in, InputStream inputStream) {
        try {
            String fileName = in.readLine();
            long fileSize = Long.parseLong(in.readLine());

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String savedName = "received_" + timestamp + ".wav";
            File outputFile = new File(saveDirectory, savedName);

            FileOutputStream fileOut = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead;

            while (totalRead < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            fileOut.close();

            // 在GUI中显示通知
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "收到新的音频消息:\n" + outputFile.getName(),
                        "新消息",
                        JOptionPane.INFORMATION_MESSAGE);
            });

            System.out.println("收到音频消息: " + outputFile.getName());

        } catch (Exception e) {
            System.out.println("接收音频文件失败: " + e.getMessage());
        }
    }
}