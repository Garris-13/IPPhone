package model;

import javax.sound.sampled.AudioFormat;

public class AudioModel {
    private AudioFormat audioFormat;
    private boolean isAudioEnabled = false;
    private int tcpPort = 8081;  // 添加TCP端口

    public AudioModel() {
        initializeAudioFormat();
    }

    private void initializeAudioFormat() {
        audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);
    }

    // Getters and Setters
    public AudioFormat getAudioFormat() { return audioFormat; }
    public void setAudioFormat(AudioFormat audioFormat) { this.audioFormat = audioFormat; }

    public boolean isAudioEnabled() { return isAudioEnabled; }
    public void setAudioEnabled(boolean audioEnabled) { isAudioEnabled = audioEnabled; }

    public int getTcpPort() { return tcpPort; }
    public void setTcpPort(int tcpPort) { this.tcpPort = tcpPort; }
    public int getAudioMessagePort() {
        return tcpPort + 1;  // 8082
    }

}