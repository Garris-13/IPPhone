package controller;

import java.io.*;
import java.net.Socket;

public class send {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        FileInputStream fis = null;
        File f = new File("a.wav");

            int msgPort = 8012; // 8081 + 101 = 8182

            socket = new Socket("192.168.5.79", 8182);
            socket.setSoTimeout(30000);

            OutputStream rawOut = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(rawOut));

            // 发送头信息
            writer.println("A.wav");
            writer.println(f.length());
            writer.flush();

            // 发送二进制文件内容
            fis = new FileInputStream(f);

            byte[] buf = new byte[4096];
            int len;

            while ((len = fis.read(buf)) != -1) {
                rawOut.write(buf, 0, len);
                System.out.println(len);
            }

            rawOut.flush();

            System.out.println("音频消息发送完成");
            socket.close();
            rawOut.close();
            writer.close();
        }
        }

