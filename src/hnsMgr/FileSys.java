package hnsMgr;

import java.io.*;

public class FileSys {
    static File f;
    static BufferedWriter bw;
    static BufferedReader br;
    public static void saveLine(String path, String line){
        f = new File(path);
        try {
            bw = new BufferedWriter(new FileWriter(f));
            bw.write(line+"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String readLine(String path){
        f = new File(path);
        try {
            br = new BufferedReader(new FileReader(f));
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void closeAll(){
        try {
            br.close();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
