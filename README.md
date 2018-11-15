#### Netty 文件传输

在之前的项目中介绍了

<a href="https://github.com/haoxiaoyong1014/springboot-netty">springboot整合 netty做心跳检测</a>

<a href="https://github.com/haoxiaoyong1014/netty-time">springboot 整合netty编写时间服务器</a>

这次通过 Netty 传递文件

此项目地址: &#8194;  https://github.com/haoxiaoyong1014/netty-file 

#### 项目依赖
```xml
 <dependency>
   <groupId>io.netty</groupId>
   <artifactId>netty-all</artifactId>
   <version>4.1.21.Final</version>
 </dependency>
```
和之前的两个例子中的依赖是一样的

#### 项目中的重要部分代码

* 客户端
    
    * FileUploadClientHandler
```$xslt

public class FileUploadClientHandler extends ChannelInboundHandlerAdapter {
    private int byteRead;
    private volatile int start = 0;
    private volatile int lastLength = 0;
    public RandomAccessFile randomAccessFile;
    private FileUploadFile fileUploadFile;
    private final static Logger LOGGER = LoggerFactory.getLogger(FileUploadClientHandler.class);
    
    public FileUploadClientHandler(FileUploadFile ef) {
        if (ef.getFile().exists()) {
            if (!ef.getFile().isFile()) {
                System.out.println("Not a file :" + ef.getFile());
                return;
            }
        }
        this.fileUploadFile = ef;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        super.channelInactive(ctx);
        LOGGER.info("客户端结束传递文件channelInactive()");
    }

    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("正在执行channelActive()方法.....");
        try {
            randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(),
                    "r");
            randomAccessFile.seek(fileUploadFile.getStarPos());
            // lastLength = (int) randomAccessFile.length() / 10;
            lastLength = 1024 * 10;
            byte[] bytes = new byte[lastLength];
            if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                fileUploadFile.setEndPos(byteRead);
                fileUploadFile.setBytes(bytes);
                ctx.writeAndFlush(fileUploadFile);   //发送消息到服务端
            } else {
            }
            LOGGER.info("channelActive()文件已经读完 " + byteRead);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }
        LOGGER.info("channelActive()方法执行结束");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof Integer) {
            start = (Integer) msg;
            if (start != -1) {
                randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(), "r");
                randomAccessFile.seek(start); //将文件定位到start
                LOGGER.info("长度：" + (randomAccessFile.length() - start));
                int a = (int) (randomAccessFile.length() - start);
                int b = (int) (randomAccessFile.length() / 1024 * 2);
                if (a < lastLength) {
                    lastLength = a;
                }
                LOGGER.info("文件长度：" + (randomAccessFile.length()) + ",start:" + start + ",a:" + a + ",b:" + b + ",lastLength:" + lastLength);
                byte[] bytes = new byte[lastLength];
                LOGGER.info("bytes的长度是="+bytes.length);
                if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - start) > 0) {
                    LOGGER.info("byteRead = "  + byteRead);
                    fileUploadFile.setEndPos(byteRead);
                    fileUploadFile.setBytes(bytes);
                    try {
                        ctx.writeAndFlush(fileUploadFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    randomAccessFile.close();
                    ctx.close();
                    LOGGER.info("文件已经读完channelRead()--------" + byteRead);
                }
            }
        }
    }

}
```

这里使用了RandomAccessFile

对RandomAccessFile 做一个简单的介绍更有助于理解代码

**RandomAccessFile特点**

> RandomAccessFile是java Io体系中功能最丰富的文件内容访问类。即可以读取文件内容，也可以向文件中写入内容。但是和其他输入/输入流不同的是，程序可以直接跳到文件的任意位置来读写数据。 
  因为RandomAccessFile可以自由访问文件的任意位置，所以如果我们希望只访问文件的部分内容，那就可以使用RandomAccessFile类。 
  与OutputStearm,Writer等输出流不同的是，RandomAccessFile类允许自由定位文件记录指针，所以RandomAccessFile可以不从文件开始的地方进行输出，所以RandomAccessFile可以向已存在的文件后追加内容。则应该使用RandomAccessFile。

> RandomAccessFile类包含了一个记录指针，用以标识当前读写处的位置，当程序新创建一个RandomAccessFile对象时，该对象的文件记录指针位于文件头（也就是0处）,
当读/写了n个字节后，文件记录指针将会向后移动n个字节。除此之外，RandomAccessFile可以自由的移动记录指针，即可以向前移动，也可以向后移动。
RandomAccessFile包含了以下两个方法来操作文件的记录指针.
```$xslt
long getFilePointer(); 返回文件记录指针的当前位置
void seek(long pos); 将文件记录指针定位到pos位置
```

> RandomAccessFile即可以读文件，也可以写，所以它即包含了完全类似于InputStream的3个read()方法，其用法和InputStream的3个read()方法完全一样；
也包含了完全类似于OutputStream的3个write()方法，其用法和OutputStream的3个Writer()方法完全一样。
除此之外，RandomAccessFile还包含了一系类的readXXX()和writeXXX()方法来完成输入和输出。

- > RandomAccessFile有两个构造器，其实这两个构造器基本相同，只是指定文件的形式不同而已，一个使用String参数来指定文件名，一个使用File参数来指定文件本身。
除此之外，创建RandomAccessFile对象还需要指定一个mode参数。该参数指定RandomAccessFile的访问模式，有以下4个值：

* “r” 以只读方式来打开指定文件夹。如果试图对该RandomAccessFile执行写入方法，都将抛出IOException异常。
* “rw” 以读，写方式打开指定文件。如果该文件尚不存在，则试图创建该文件。
* “rws” 以读，写方式打开指定文件。相对于”rw” 模式，还要求对文件内容或元数据的每个更新都同步写入到底层设备。
* “rwd” 以读，写方式打开指定文件。相对于”rw” 模式，还要求对文件内容每个更新都同步写入到底层设备

**下面对上面的FileUploadClientHandler类中的代码进行说明:**

`channelActive()`方法中有这么一段代码:
```$xslt
ctx.writeAndFlush(fileUploadFile);
```
这段代码的意思是向服务端发送消息,消息内容就是`FileUploadFile`对象, FileUploadFile对象中包含 :文件,文件名,开始位置,文件字节数组,结尾位置

**服务端代码:**

* FileUploadServerHandler
```$xslt
public class FileUploadServerHandler extends ChannelInboundHandlerAdapter {
	private int byteRead;
    private volatile int start = 0;
    private String file_dir = "/tmp";
    private final static Logger LOGGER = LoggerFactory.getLogger(FileUploadServerHandler.class);
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	// TODO Auto-generated method stub
    	super.channelActive(ctx);
        LOGGER.info("服务端：channelActive()");
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	// TODO Auto-generated method stub
    	super.channelInactive(ctx);
        LOGGER.info("服务端：channelInactive()");
    	ctx.flush();
    	ctx.close();
    }
    
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.info("收到客户端发来的文件,正在处理....");
        if (msg instanceof FileUploadFile) {
            FileUploadFile ef = (FileUploadFile) msg;
            byte[] bytes = ef.getBytes();
            byteRead = ef.getEndPos();
            String md5 = ef.getFile_md5();//文件名
            String path = file_dir + File.separator + md5;
            File file = new File(path);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");//r: 只读模式 rw:读写模式
            randomAccessFile.seek(start);//移动文件记录指针的位置,
            randomAccessFile.write(bytes);//调用了seek（start）方法，是指把文件的记录指针定位到start字节的位置。也就是说程序将从start字节开始写数据
            start = start + byteRead;
            if (byteRead > 0) {
                ctx.writeAndFlush(start);//向客户端发送消息
                randomAccessFile.close();
                if(byteRead!=1024 * 10){
                	Thread.sleep(1000);
                	channelInactive(ctx);
                }
            } else {
                ctx.close();
            }
            LOGGER.info("处理完毕,文件路径:"+path+","+byteRead);
        }
    }
}
```

`channelRead()`方法即是接收客户端的代码,客户端也有这个方法,客户端`channelRead()`方法主要是负责读文件,服务端主要是写文件.

**注意:**
在服务端`FileUploadServerHandler`这个类中我们要将`file_dir`的路径改为自己电脑上的路径,mac: /tmp; windows: F:


我们可以先运行服务端的`ServerFileTest`测试类,然后运行客户端的 `ClientFileTest`测试类进行

具体代码就不贴上去了,可以在这里进行下载这个案例
<a href="https://github.com/haoxiaoyong1014/netty-file">netty-file</a>
&ensp;&ensp;如果对你有帮助还请给个Star哦



