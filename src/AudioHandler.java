import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioHandler {
    private static final int AUDIO_BUFFER_SIZE = 1024;
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;

    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private boolean isRecording;
    private boolean isPlaying;

    public AudioHandler() {
        initializeAudioFormat();
    }

    private void initializeAudioFormat() {
        audioFormat = new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                true, // signed
                true  // bigEndian
        );
    }

    public boolean initializeAudioDevices() {
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

            return true;

        } catch (LineUnavailableException e) {
            System.err.println("音频设备初始化失败: " + e.getMessage());
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

            new Thread(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    InetAddress remoteAddress = InetAddress.getByName(remoteIP);
                    byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

                    System.out.println("开始发送音频到 " + remoteIP + ":" + remoteAudioPort);

                    while (isRecording) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            DatagramPacket packet = new DatagramPacket(
                                    buffer, bytesRead, remoteAddress, remoteAudioPort
                            );
                            socket.send(packet);
                        }

                        // 短暂休眠以减少CPU使用
                        Thread.sleep(10);
                    }

                } catch (IOException | InterruptedException e) {
                    System.err.println("音频发送错误: " + e.getMessage());
                } finally {
                    stopRecording();
                }
            }).start();

        } catch (LineUnavailableException e) {
            System.err.println("无法打开麦克风: " + e.getMessage());
        }
    }

    public void startPlaying(int localAudioPort) {
        if (isPlaying) {
            System.out.println("已经在播放中");
            return;
        }

        try {
            speakers.open(audioFormat);
            speakers.start();
            isPlaying = true;

            new Thread(() -> {
                try (DatagramSocket socket = new DatagramSocket(localAudioPort)) {
                    byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

                    System.out.println("开始接收音频在端口 " + localAudioPort);

                    while (isPlaying) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        if (isPlaying) {
                            speakers.write(packet.getData(), 0, packet.getLength());
                        }
                    }

                } catch (IOException e) {
                    System.err.println("音频接收错误: " + e.getMessage());
                } finally {
                    stopPlaying();
                }
            }).start();

        } catch (LineUnavailableException e) {
            System.err.println("无法打开扬声器: " + e.getMessage());
        }
    }

    public void stopRecording() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        System.out.println("停止录音");
    }

    public void stopPlaying() {
        isPlaying = false;
        if (speakers != null) {
            speakers.stop();
            speakers.close();
        }
        System.out.println("停止播放");
    }

    public void setVolume(float volume) {
        if (speakers != null && speakers.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) speakers.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
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
    }
}