package controller;

import model.AudioModel;
import model.NetworkModel;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

/**
 * 最终修复版 AudioController：
 * - 支持音频消息发送（TCP 8182）
 * - 支持实时通话（UDP）
 * - 修复 isMuted Getter/Setter
 * - 修复连接错误、线程退出、资源关闭等问题
 */
public class AudioController {

    private final AudioModel audioModel;
    private final NetworkModel networkModel;

    // ============================
    // 实时音频相关
    // ============================
    private volatile boolean isStreaming = false;
    private volatile boolean isMuted = false;
    private Thread sendThread;
    private Thread recvThread;

    // ============================
    // 音频消息录制相关
    // ============================
    private boolean isRecording = false;
    private TargetDataLine recordingLine;
    private AudioInputStream recordingStream;
    private File currentRecordingFile;
    private boolean isMicrophoneAvailable = false;

    public AudioController(AudioModel audioModel, NetworkModel networkModel) {
        this.audioModel = audioModel;
        this.networkModel = networkModel;
    }

    // ==========================================================
    // Getter / Setter（修复 private isMuted 报错问题）
    // ==========================================================
    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    // ==========================================================
    // 实时音频：启动 UDP 音频线程
    // ==========================================================
    public void startAudioStreaming(String remoteIP, int udpPort) {
        if (isStreaming) return;

        isStreaming = true;

        sendThread = new Thread(() -> sendAudio(remoteIP, udpPort), "AudioSendThread");
        recvThread = new Thread(this::receiveAudio, "AudioRecvThread");

        sendThread.start();
        recvThread.start();
    }

    // 停止实时音频
    public void stopAudio() {
        isStreaming = false;

        if (sendThread != null) sendThread.interrupt();
        if (recvThread != null) recvThread.interrupt();
    }


    // 实时音频发送（UDP）
    private void sendAudio(String remoteIP, int udpPort) {
        TargetDataLine line = null;

        try {
            AudioFormat format = audioModel.getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);

            line.open(format);
            line.start();

            InetAddress remoteAddr = InetAddress.getByName(remoteIP);
            byte[] buffer = new byte[1024];

            while (isStreaming && !Thread.currentThread().isInterrupted()) {

                if (!isMuted) {
                    int len = line.read(buffer, 0, buffer.length);
                    DatagramPacket packet = new DatagramPacket(buffer, len, remoteAddr, udpPort);

                    DatagramSocket udp = networkModel.getUdpSocket();
                    if (udp != null && !udp.isClosed()) {
                        udp.send(packet);
                    }
                } else {
                    Thread.sleep(10);
                }
            }

        } catch (Exception e) {
            System.err.println("音频发送错误: " + e.getMessage());
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

    // ==========================================================
    // 实时音频接收（UDP）
    // ==========================================================
//    private void receiveAudio() {
//        SourceDataLine line = null;
//
//        try {
//            AudioFormat format = audioModel.getAudioFormat();
//            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
//            line = (SourceDataLine) AudioSystem.getLine(info);
//
//            line.open(format);
//            line.start();
//
//            byte[] buf = new byte[1024];
//
//            while (isStreaming && !Thread.currentThread().isInterrupted()) {
//                DatagramSocket udp = networkModel.getUdpSocket();
//
//                if (udp == null || udp.isClosed()) {
//                    Thread.sleep(10);
//                    continue;
//                }
//
//                DatagramPacket packet = new DatagramPacket(buf, buf.length);
//                udp.receive(packet);
//
//                line.write(packet.getData(), 0, packet.getLength());
//            }
//
//        } catch (Exception e) {
//            System.err.println("音频接收错误: " + e.getMessage());
//        } finally {
//            if (line != null) {
//                line.stop();
//                line.close();
//            }
//        }
//    }

    // ==========================================================
    // 音频消息：开始录音
    // ==========================================================
//    public boolean startRecording() {
//        if (isRecording) return false;
//
//        try {
//            isRecording = true;
//
//            AudioFormat format = audioModel.getAudioFormat();
//            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
//
//            recordingLine = (TargetDataLine) AudioSystem.getLine(info);
//            recordingLine.open(format);
//            recordingLine.start();
//
//            // 临时文件
//            currentRecordingFile = File.createTempFile("audio_message_", ".wav");
//
//            recordingStream = new AudioInputStream(recordingLine);
//
//            // 异步写入文件
//            new Thread(() -> {
//                try {
//                    AudioSystem.write(recordingStream, AudioFileFormat.Type.WAVE, currentRecordingFile);
//                } catch (Exception ignored) {}
//            }, "AudioRecordingWriter").start();
//
//            return true;
//
//        } catch (Exception e) {
//            System.err.println("录音失败: " + e.getMessage());
//            isRecording = false;
//            return false;
//        }
//    }
    public boolean startRecording() {
        if (isRecording) {
            System.err.println("已经在录音中");
            return false;
        }

        // 先检查麦克风是否可用
        if (!isMicrophoneAvailable()) {
            System.err.println("麦克风不可用，无法开始录音");
            return false;
        }

        try {
            isRecording = true;

            AudioFormat format = audioModel.getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            recordingLine = (TargetDataLine) AudioSystem.getLine(info);
            recordingLine.open(format);
            recordingLine.start();

            System.out.println("麦克风已连接，开始录音...");

            // 临时文件
            currentRecordingFile = File.createTempFile("audio_message_", ".wav");

            recordingStream = new AudioInputStream(recordingLine);

            // 异步写入文件
            new Thread(() -> {
                try {
                    AudioSystem.write(recordingStream, AudioFileFormat.Type.WAVE, currentRecordingFile);
                    System.out.println("录音文件已保存: " + currentRecordingFile.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("保存录音文件失败: " + e.getMessage());
                }
            }, "AudioRecordingWriter").start();

            return true;

        } catch (Exception e) {
            System.err.println("录音失败: " + e.getMessage());
            isRecording = false;
            return false;
        }
    }

    // ==========================================================
    // 停止录音
    // ==========================================================
    public File stopRecording() {
        if (!isRecording || recordingLine == null) return null;

        isRecording = false;

        try {
            recordingLine.stop();
            recordingLine.close();
            recordingLine = null;

            Thread.sleep(100);

            if (currentRecordingFile.length() > 0) {
                return currentRecordingFile;
            }

        } catch (Exception e) {
            System.err.println("停止录音失败: " + e.getMessage());
        }

        return null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public File getRecordedFile() {
        return currentRecordingFile;
    }


    // 发送音频文件消息（TCP，端口 = tcpPort + 101，即 8182）
    public void sendAudioMessage(String remoteIP, File audioFile) {
        new Thread(() -> {

            Socket socket = null;
            FileInputStream fis = null;

            try {
                int msgPort = audioModel.getTcpPort() + 101; // 8081 + 101 = 8182

                socket = new Socket(remoteIP, msgPort);
                socket.setSoTimeout(30000);

                OutputStream rawOut = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(rawOut, "UTF-8"), true);

                // 发送头信息
                writer.println(audioFile.getName());
                writer.println(audioFile.length());
                writer.flush();

                // 发送二进制文件内容
                fis = new FileInputStream(audioFile);

                byte[] buf = new byte[4096];
                int len;

                while ((len = fis.read(buf)) != -1) {
                    rawOut.write(buf, 0, len);
                    rawOut.flush();
                    Thread.sleep((10));
                }

                rawOut.flush();

                System.out.println("音频消息发送完成");

            } catch (Exception e) {
                System.err.println("发送音频消息失败: " + e.getMessage());
            } finally {
                try { if (fis != null) fis.close(); } catch (Exception ignored) {}
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            }

        }, "AudioMessageSender").start();
    }
    public boolean isMicrophoneAvailable() {
        if (isMicrophoneAvailable)      //增加变量判断
            return true;
        try {
            AudioFormat format = audioModel.getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            // 检查系统是否有可用的麦克风
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("麦克风不支持当前音频格式");
                return false;
            }

            // 尝试打开麦克风线路
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.close();
            isMicrophoneAvailable = true;
            return true;

        } catch (Exception e) {
            System.err.println("麦克风检测失败: " + e.getMessage());
            isMicrophoneAvailable = false;
            return false;
        }
    }

    /**
     * 获取可用的麦克风列表
     */
    public void listAvailableMicrophones() {
        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            System.out.println("=== 可用的音频设备 ===");

            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] sourceLines = mixer.getSourceLineInfo(); // 扬声器
                Line.Info[] targetLines = mixer.getTargetLineInfo(); // 麦克风

                System.out.println("设备: " + mixerInfo.getName());
                System.out.println("  描述: " + mixerInfo.getDescription());

                // 显示麦克风信息
                if (targetLines.length > 0) {
                    System.out.println("  麦克风: " + targetLines.length + " 个输入线路");
                    for (Line.Info lineInfo : targetLines) {
                        System.out.println("    - " + lineInfo);
                    }
                }

                // 显示扬声器信息
                if (sourceLines.length > 0) {
                    System.out.println("  扬声器: " + sourceLines.length + " 个输出线路");
                }

                System.out.println("------------------------");
            }
        } catch (Exception e) {
            System.err.println("获取音频设备列表失败: " + e.getMessage());
        }
    }
    private volatile boolean isAudioDetected = false;
    private volatile long firstAudioDetectionTime = 0;
    private volatile String audioDetectedIP = "";
    private AudioDetectionListener audioDetectionListener;

    // 音频检测监听器接口
    public interface AudioDetectionListener {
        void onAudioDetected(String ip, long timestamp);
    }

    // 设置音频检测监听器
    public void setAudioDetectionListener(AudioDetectionListener listener) {
        this.audioDetectionListener = listener;
    }

    // 获取音频检测信息
    public AudioDetectionInfo getAudioDetectionInfo() {
        return new AudioDetectionInfo(isAudioDetected, firstAudioDetectionTime, audioDetectedIP);
    }

    // 音频检测信息类
    public static class AudioDetectionInfo {
        private final boolean detected;
        private final long firstDetectionTime;
        private final String detectedIP;

        public AudioDetectionInfo(boolean detected, long firstDetectionTime, String detectedIP) {
            this.detected = detected;
            this.firstDetectionTime = firstDetectionTime;
            this.detectedIP = detectedIP;
        }

        public boolean isDetected() { return detected; }
        public long getFirstDetectionTime() { return firstDetectionTime; }
        public String getDetectedIP() { return detectedIP; }
    }


    // 实时音频接收方法，添加音频检测
    private void receiveAudio() {
        SourceDataLine line = null;

        try {
            AudioFormat format = audioModel.getAudioFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);

            line.open(format);
            line.start();

            byte[] buf = new byte[1024];
            DatagramSocket udp = new DatagramSocket(9091);
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (isStreaming && !Thread.currentThread().isInterrupted()) {
                //DatagramSocket udp = networkModel.getUdpSocket();

                if (udp == null || udp.isClosed()) {
                    Thread.sleep(10);
                    continue;
                }

                //DatagramPacket packet = new DatagramPacket(buf, buf.length);
                udp.receive(packet);

                // 检测音频数据
                String remoteIP = packet.getAddress().getHostAddress();
                detectAudioInPacket(packet.getData(), packet.getLength(), remoteIP);

                line.write(packet.getData(), 0, packet.getLength());
            }
            udp.close();

        } catch (Exception e) {
            System.err.println("音频接收错误: " + e.getMessage());
        } finally {
            if (line != null) {
                line.stop();
                line.close();

            }

        }
    }

    // 音频检测逻辑
    private void detectAudioInPacket(byte[] audioData, int length, String sourceIP) {
        if (length == 0) return;

        // 计算音频数据的平均音量
        double audioLevel = calculateAudioLevel(audioData, length);

        // 设置音量阈值（根据实际情况调整）
        double threshold = 10.0;

        if (audioLevel > threshold) {
            // 检测到有效音频
            if (!isAudioDetected) {
                // 第一次检测到音频
                isAudioDetected = true;
                firstAudioDetectionTime = System.currentTimeMillis();
                audioDetectedIP = sourceIP;

                System.out.println("首次检测到音频来自: " + sourceIP +
                        ", 时间: " + new java.util.Date(firstAudioDetectionTime) +
                        ", 音量: " + audioLevel);

                // 通知监听器
                if (audioDetectionListener != null) {
                    audioDetectionListener.onAudioDetected(sourceIP, firstAudioDetectionTime);
                }
            } else {
                // 后续检测，可以在这里添加持续检测逻辑
                // 例如：更新当前说话者等
            }
        }
    }

    // 计算音频电平
    private double calculateAudioLevel(byte[] audioData, int length) {
        long sum = 0;
        int sampleCount = length / 2; // 16-bit samples

        for (int i = 0; i < length; i += 2) {
            // 将两个byte组合成16-bit sample
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF);
            sum += Math.abs(sample);
        }

        return sampleCount > 0 ? (double) sum / sampleCount : 0;
    }


    // 重置音频检测状态
    public void resetAudioDetection() {
        isAudioDetected = false;
        firstAudioDetectionTime = 0;
        audioDetectedIP = "";
    }

    // ==========================================================
    // 获取当前音频状态
    // ==========================================================
    public String getAudioStatusText() {
        if (!isAudioDetected) {
            return "等待音频输入...";
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
        String timeStr = sdf.format(new java.util.Date(firstAudioDetectionTime));

        return "检测到声音: " + audioDetectedIP + " (" + timeStr + ")";
    }


}
