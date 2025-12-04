import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IPPhoneServer {
    private ServerSocket tcpServer;
    private int tcpPort = 8081;
    private String receivedFilesDir = "received_audio_messages";

    public IPPhoneServer() {
        createReceivedDirectory();
        startServer();
    }

    private void createReceivedDirectory() {
        File dir = new File(receivedFilesDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("创建接收目录: " + dir.getAbsolutePath());
            } else {
                System.out.println("无法创建接收目录，使用当前目录");
                receivedFilesDir = ".";
            }
        }
    }

    private void startServer() {
        try {
            tcpServer = new ServerSocket(tcpPort);
            System.out.println("IP Phone 服务器启动，监听端口: " + tcpPort);
            System.out.println("接收目录: " + new File(receivedFilesDir).getAbsolutePath());
            System.out.println("等待客户端连接...");

            while (true) {
                Socket clientSocket = tcpServer.accept();
                System.out.println("客户端连接: " + clientSocket.getInetAddress().getHostAddress());

                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String message = in.readLine();
                System.out.println("收到消息类型: " + message);

                if ("DIAL_REQUEST".equals(message)) {
                    handleDialRequest(in, out);
                } else if ("AUDIO_MESSAGE".equals(message)) {
                    handleAudioMessage(in, clientSocket.getInputStream());
                } else if ("CALL_END".equals(message)) {
                    System.out.println("通话结束");
                } else {
                    System.out.println("未知消息类型: " + message);
                }

            } catch (IOException e) {
                System.out.println("处理客户端请求时出错: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("客户端连接已关闭");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleDialRequest(BufferedReader in, PrintWriter out) {
            try {
                System.out.println("收到拨号请求");
                // 这里可以添加用户确认逻辑
                // 例如：弹出确认对话框或等待用户输入

                // 自动接受通话
                out.println("DIAL_ACCEPT");
                System.out.println("已接受通话请求");

                // 等待通话结束或其他消息
                String nextMessage = in.readLine();
                if ("CALL_END".equals(nextMessage)) {
                    System.out.println("通话结束");
                }

            } catch (IOException e) {
                System.out.println("处理拨号请求时出错: " + e.getMessage());
            }
        }

        private void handleAudioMessage(BufferedReader in, InputStream inputStream) {
            FileOutputStream fileOut = null;
            try {
                String fileName = in.readLine();
                String fileSizeStr = in.readLine();

                if (fileName == null || fileSizeStr == null) {
                    System.out.println("无效的音频消息格式");
                    return;
                }

                long fileSize = Long.parseLong(fileSizeStr);

                // 生成带时间戳的文件名
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String savedFileName = "audio_message_" + timestamp + ".wav";
                File outputFile = new File(receivedFilesDir, savedFileName);

                System.out.println("接收音频消息: " + fileName);
                System.out.println("文件大小: " + fileSize + " 字节");
                System.out.println("保存为: " + outputFile.getName());

                fileOut = new FileOutputStream(outputFile);
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;

                // 显示接收进度
                System.out.print("接收进度: 0%");

                while (totalRead < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    // 显示进度
                    int progress = (int) ((totalRead * 100) / fileSize);
                    System.out.print("\r接收进度: " + progress + "%");
                }

                System.out.println("\n文件接收完成: " + outputFile.getName());
                System.out.println("实际接收: " + totalRead + " 字节");

                // 检查文件是否完整
                if (totalRead == fileSize) {
                    System.out.println("✓ 文件接收完整");
                } else {
                    System.out.println("⚠ 文件接收不完整，期望: " + fileSize + "，实际: " + totalRead);
                }

            } catch (IOException e) {
                System.out.println("\n接收音频消息失败: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("无效的文件大小格式");
            } finally {
                if (fileOut != null) {
                    try {
                        fileOut.close();
                    } catch (IOException e) {
                        System.out.println("关闭文件输出流失败: " + e.getMessage());
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== IP Phone 服务器 ===");
        System.out.println("正在启动服务器...");
        new IPPhoneServer();
    }
}