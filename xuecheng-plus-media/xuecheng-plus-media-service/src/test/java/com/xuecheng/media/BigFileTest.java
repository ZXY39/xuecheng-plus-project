package com.xuecheng.media;


import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BigFileTest {
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("D:\\test.mp4");
        String filePath ="D:\\zzzzz\\";

        int chunkSize =1024*1024*5;
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        RandomAccessFile r = new RandomAccessFile(sourceFile, "r");

        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File tFile = new File(filePath + i);
            RandomAccessFile trw = new RandomAccessFile(tFile, "rw");
            int len =-1;
            while ((len =r.read(bytes))!=-1){
                trw.write(bytes,0,len);
                if(tFile.length()>=chunkSize){
                 break;
                }
            }
            trw.close();
        }
        r.close();
    }

    @Test
    public void testMerge() throws IOException {
        File chunkFolder = new File("D:\\zzzzz\\");
        File sourceFile = new File("D:\\test.mp4");
        File mergeFile = new File("D:\\test_merged.mp4");

        File[] files = chunkFolder.listFiles();
        List<File> filesList = Arrays.asList(files);
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });

        RandomAccessFile trw = new RandomAccessFile(mergeFile, "rw");
         byte[] bytes =new byte[1024];
        for (File file : filesList) {
            RandomAccessFile r = new RandomAccessFile(file, "r");
            int len=-1;
            while((len= r.read(bytes))!=-1){
               trw.write(bytes,0,len);
            }
            r.close();
        }
        trw.close();
        FileInputStream fileInputStream = new FileInputStream(mergeFile);
        FileInputStream fileInputStream1 = new FileInputStream(sourceFile);
        String s = DigestUtils.md5Hex(fileInputStream);
        String s1 = DigestUtils.md5Hex(fileInputStream1);
        System.out.println(s1.equals(s)?"合并成功":"合并失败");
    }













    @Test
    public void testChunk1() throws IOException {
        //源文件
        File sourceFile = new File("D:\\test.mp4");

        //分块文件存储路径
        File chunkFolderPath = new File("D:\\bigfile_test\\");
        if(!chunkFolderPath.exists()){
            chunkFolderPath.mkdirs();
        }

        //分块的大小
        int chunkSize = 1024 * 1024 * 1;

        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);

        //思路，使用流对象读取源文件，向分块文件写数据，达到分块大小不再写
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r");
        //缓冲区
        byte[] b = new byte[1024];
        for (long i = 0; i < chunkNum; i++) {

            File file = new File("D:\\bigfile_test\\" + i);
            //如果分块文件存在，则删除
            if(file.exists()){
                file.delete();
            }
            boolean newFile = file.createNewFile();
            if(newFile){
                //向分块文件写数据流对象
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
                int len=-1;
                while ((len=raf_read.read(b))!=-1){
                    //向文件中写数据
                    raf_write.write(b,0,len);
                    //达到分块大小不再写了
                    if(file.length()>=chunkSize){
                        break;
                    }
                }
                raf_write.close();

            }

        }
        raf_read.close();

    }
}
