import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class PhoneServer {
    private static final int DIAL_PORT = 6060;
    private static final int MESSAGE_PORT = 6062;
    private ExecutorService threadPool;
    private CallManager callManager;

    public PhoneServer() {
        this.threadPool = Executors.newFixedThreadPool(10);
        this.callManager = new CallManager();
    }

    public void start() {
        System.out.println("启动电话服务器...");

        // 启动拨号服务器
        new Thread(this::startDialingServer).start();

        // 启动消息服务器
        new Thread(this::startMessageServer).start();

        System.out.println("电话服务器已启动在端口 " + DIAL_PORT + " 和 " + MESSAGE_PORT);
    }

    private void startDialingServer() {
        try (ServerSocket serverSocket = new ServerSocket(DIAL_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new DialingHandler(clientSocket, callManager));
            }
        } catch (IOException e) {
            System.err.println("拨号服务器错误: " + e.getMessage());
        }
    }

    private void startMessageServer() {
        try (ServerSocket serverSocket = new ServerSocket(MESSAGE_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new MessageHandler(clientSocket, callManager));
            }
        } catch (IOException e) {
            System.err.println("消息服务器错误: " + e.getMessage());
        }
    }

    public void stop() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("电话服务器已停止");
    }

    public static void main(String[] args) {
        PhoneServer server = new PhoneServer();
        server.start();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}