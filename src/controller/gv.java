package controller;

import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class gv {
    public static void main(String[] args) throws IOException, InterruptedException, UnsupportedAudioFileException, LineUnavailableException {

        ServerSocket ss = new ServerSocket(8182);
        while (true) {

            Socket client = ss.accept();
            InputStream is = client.getInputStream();
            //BufferedReader in = new BufferedReader(new InputStreamReader(is));
            DataInputStream dis= new DataInputStream(is);
            // ★★★ 1. 接收文件名与大小
            String fileName = dis.readLine();
            String sizeStr = dis.readLine();

            long size = Long.parseLong(sizeStr);
            System.out.println("收到文件" + fileName);
            System.out.println("收到大小" + sizeStr);
            byte[] buffer = new byte[4096];
            long total = 0;
            int len;

            while (total < size && (len = dis.read(buffer,0,(int)Math.min(buffer.length,size-total))) != -1) {
                System.out.println(len);
                total += len;
            }
            System.out.println("实际大小 " + total);
        }
        /*
        gv gvplay = new gv();
        gvplay.play("d://Ring05.wav");
        Thread.sleep(20000);
*/
    }
    public void play(String filePath)
    {
        try
        {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        }catch(Exception e)
        {
            System.out.println("EEror");
        }
    }

}