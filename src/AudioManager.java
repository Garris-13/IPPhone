import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioManager {
    private static final int AUDIO_BUFFER_SIZE = 1024;
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;

    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private boolean isRecording;
    private boolean isPlaying;
    private DatagramSocket audioSocket;
    private int audioPort;

    public AudioManager(int audioPort) {
        this.audioPort = audioPort;
        this.audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, true, true);
        initializeAudioDevices();
    }

    private boolean initializeAudioDevices() {
        try {
            // 初始化麦克风
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(micInfo)) {
                System.err.println("麦克风不支持指定的音频格式");
                return false;
            }
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);

            // 初始化扬声器
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.err.println("扬声器不支持指定的音频格式");
                return false;
            }
            speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);

            // 初始化音频socket
            audioSocket = new DatagramSocket(audioPort);

            System.out.println("音频设备初始化成功，端口: " + audioPort);
            return true;

        } catch (LineUnavailableException e) {
            System.err.println("音频设备初始化失败 - LineUnavailable: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("音频Socket初始化失败: " + e.getMessage());
            return false;
        }
    }

    public void startRecording(String remoteIP, int remoteAudioPort) {
        if (isRecording) {
            System.out.println("已经在录音中");
            return;
        }

        try {
            microphone.open(audioFormat);
            microphone.start();
            isRecording = true;

            Thread recordingThread = new Thread(new RecordingTask(remoteIP, remoteAudioPort));
            recordingThread.setDaemon(true);
            recordingThread.start();

        } catch (LineUnavailableException e) {
            System.err.println("无法打开麦克风: " + e.getMessage());
        }
    }

    private class RecordingTask implements Runnable {
        private String remoteIP;
        private int remoteAudioPort;

        public RecordingTask(String remoteIP, int remoteAudioPort) {
            this.remoteIP = remoteIP;
            this.remoteAudioPort = remoteAudioPort;
        }

        @Override
        public void run() {
            System.out.println("开始发送音频到 " + remoteIP + ":" + remoteAudioPort);
            byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

            try {
                InetAddress remoteAddress = InetAddress.getByName(remoteIP);

                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        DatagramPacket packet = new DatagramPacket(
                                buffer, bytesRead, remoteAddress, remoteAudioPort
                        );
                        audioSocket.send(packet);
                    }

                    // 短暂休眠以减少CPU使用
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                if (isRecording) {
                    System.err.println("音频发送错误: " + e.getMessage());
                }
            } finally {
                if (microphone != null && microphone.isOpen()) {
                    microphone.stop();
                    microphone.close();
                }
            }
        }
    }

    public void startPlaying() {
        if (isPlaying) {
            System.out.println("已经在播放中");
            return;
        }

        try {
            speakers.open(audioFormat);
            speakers.start();
            isPlaying = true;

            Thread playingThread = new Thread(new PlayingTask());
            playingThread.setDaemon(true);
            playingThread.start();

        } catch (LineUnavailableException e) {
            System.err.println("无法打开扬声器: " + e.getMessage());
        }
    }

    private class PlayingTask implements Runnable {
        @Override
        public void run() {
            System.out.println("开始接收音频在端口 " + audioPort);
            byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

            while (isPlaying) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    audioSocket.receive(packet);

                    if (isPlaying && speakers != null && speakers.isOpen()) {
                        speakers.write(packet.getData(), 0, packet.getLength());
                    }
                } catch (IOException e) {
                    if (isPlaying) {
                        System.err.println("音频接收错误: " + e.getMessage());
                    }
                }
            }

            // 清理扬声器资源
            if (speakers != null && speakers.isOpen()) {
                speakers.stop();
                speakers.close();
            }
        }
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            System.out.println("停止录音");
        }
    }

    public void stopPlaying() {
        if (isPlaying) {
            isPlaying = false;
            System.out.println("停止播放");
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void close() {
        stopRecording();
        stopPlaying();
        if (audioSocket != null && !audioSocket.isClosed()) {
            audioSocket.close();
        }
        System.out.println("音频管理器已关闭");
    }
}