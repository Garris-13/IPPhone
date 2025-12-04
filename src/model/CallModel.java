package model;

public class CallModel {
    private String remoteIP;
    private int tcpPort = 8081;
    private int udpPort = 9091;
    private boolean isCalling = false;
    private boolean isMuted = false;
    private boolean remoteDisconnected = false; // 新增：对方是否已断开连接

    // Getters and Setters
    public String getRemoteIP() { return remoteIP; }
    public void setRemoteIP(String remoteIP) { this.remoteIP = remoteIP; }

    public int getTcpPort() { return tcpPort; }
    public void setTcpPort(int tcpPort) { this.tcpPort = tcpPort; }

    public int getUdpPort() { return udpPort; }
    public void setUdpPort(int udpPort) { this.udpPort = udpPort; }

    public boolean isCalling() { return isCalling; }
    public void setCalling(boolean calling) { isCalling = calling; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }

    // 新增：对方断开连接状态
    public boolean isRemoteDisconnected() { return remoteDisconnected; }
    public void setRemoteDisconnected(boolean remoteDisconnected) {
        this.remoteDisconnected = remoteDisconnected;
    }

    // 重置所有状态
    public void resetAll() {
        isCalling = false;
        isMuted = false;
        remoteDisconnected = false;
    }
}