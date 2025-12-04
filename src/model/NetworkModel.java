//package model;
//
//import java.net.Socket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.net.SocketException;
//import java.util.Enumeration;
//
//public class NetworkModel {
//    private Socket tcpSocket;
//    private DatagramSocket udpSocket;
//    private String localIP;
//
//    public NetworkModel() {
//        this.localIP = getLocalIPAddress();
//    }
//
//    // 获取本地IP地址（优先获取非回环地址）
//    private String getLocalIPAddress() {
//        try {
//            // 先尝试获取本机IP（非回环地址）
//            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//            while (interfaces.hasMoreElements()) {
//                NetworkInterface iface = interfaces.nextElement();
//                // 跳过回环接口和未启用的接口
//                if (iface.isLoopback() || !iface.isUp()) continue;
//
//                Enumeration<InetAddress> addresses = iface.getInetAddresses();
//                while (addresses.hasMoreElements()) {
//                    InetAddress addr = addresses.nextElement();
//                    // 只返回IPv4地址
//                    if (addr.getHostAddress().contains(":")) continue;
//                    return addr.getHostAddress();
//                }
//            }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//        // 如果获取失败，返回回环地址
//        return "127.0.0.1";
//    }
//
//    // Getters and Setters
//    public Socket getTcpSocket() { return tcpSocket; }
//    public void setTcpSocket(Socket tcpSocket) { this.tcpSocket = tcpSocket; }
//
//    public DatagramSocket getUdpSocket() { return udpSocket; }
//    public void setUdpSocket(DatagramSocket udpSocket) { this.udpSocket = udpSocket; }
//
//    public String getLocalIP() { return localIP; }
//    public void setLocalIP(String localIP) { this.localIP = localIP; }
//}
package model;

import java.net.Socket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkModel {
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private ServerSocket serverSocket;
    private String localIP;
    private boolean isServerRunning = false;

    public NetworkModel() {
        this.localIP = getLocalIPAddress();
    }

    // 获取本地IP地址（优先获取非回环地址）
    private String getLocalIPAddress() {
        try {
            System.out.println("=== 网络接口检查 ===");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            // 优先选择无线网络接口
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                // 优先选择无线网络
                if (iface.getName().toLowerCase().contains("wireless")) {
                    System.out.println("选择无线网络接口: " + iface.getName());
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.getHostAddress().contains(":")) { // IPv4
                            String ip = addr.getHostAddress();
                            System.out.println("选择IP: " + ip);
                            return ip;
                        }
                    }
                }
            }

            // 如果没有无线网络，选择任何可用的IPv4
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.getHostAddress().contains(":")) { // IPv4
                        String ip = addr.getHostAddress();
                        System.out.println("选择IP: " + ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }


    // Getters and Setters
    public Socket getTcpSocket() { return tcpSocket; }
    public void setTcpSocket(Socket tcpSocket) { this.tcpSocket = tcpSocket; }

    public DatagramSocket getUdpSocket() { return udpSocket; }
    public void setUdpSocket(DatagramSocket udpSocket) { this.udpSocket = udpSocket; }

    public ServerSocket getServerSocket() { return serverSocket; }
    public void setServerSocket(ServerSocket serverSocket) { this.serverSocket = serverSocket; }

    public String getLocalIP() { return localIP; }
    public void setLocalIP(String localIP) { this.localIP = localIP; }

    public boolean isServerRunning() { return isServerRunning; }
    public void setServerRunning(boolean serverRunning) { isServerRunning = serverRunning; }
}