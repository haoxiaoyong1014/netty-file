package com.haoxy.netty.file;

import com.haoxy.netty.server.file.work.FileUploadServer;
import org.junit.Test;

/**
 * Created by haoxy on 2018/11/15.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */
public class ServerFileTest {

    private static final int FILE_PORT = 9991;

    @Test
    public void testServerFile() {
        try {
            new FileUploadServer().bind(FILE_PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
