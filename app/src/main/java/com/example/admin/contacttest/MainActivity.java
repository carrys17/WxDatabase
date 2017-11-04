package com.example.admin.contacttest;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;


import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//           本地数据库的加密规则：
//          1.获取手机IMEI码
//          2.获取当前登录微信账号的uin(存储在sp里面)
//          3.拼接IMEI和uin
//          4.将拼接完的字符串进行md5加密
//          5.截取加完密的字符串的前七位（字母必须为小写）
public class MainActivity extends AppCompatActivity {

    private static final String WECHAT_PATH = "/data/data/com.tencent.mm/";
    private static final String SP_UIN_PATH = WECHAT_PATH +"shared_prefs/auth_info_key_prefs.xml";

    private String phoneIMEI ;  // 手机IMEI码
    private String currentUin; // 当前登录微信账号的uin码
    private String password;   // 加密密码


    private static final String DB_DIR_PATH = WECHAT_PATH + "MicroMsg";  // 数据库的文件夹目录
    private static final String DB_FILE_NAME = "EnMicroMsg.db";           // 数据库名

    //   List<File> fileList = new ArrayList<>();  // 找到的数据库
    private static final String COPY_DB_NAME = "wx_data.db";

    String mCurrentApkPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 没有root的要先赋予权限
        execCMD("chmod -R 777 " + WECHAT_PATH);

        // 1、获取手机IMEI码
        initPhoneIMEI();

        // 2、获取当前登录微信账号的uin(存储在sp里面)/data/data/com.tencent.mm/shared_prefs/auth_info_key_prefs.xml 里面的name为_auth_uin的值
        initCurrentUin();

        // 3、拼接、加密并获取到密码
        initDbPassword(phoneIMEI,currentUin);

        // 4、获取EnMicroMsg.db的详细路径
        // 在文件夹MicroMsg中递归查找数据库文件EnMicroMsg.db(如果有多个微信号登陆过的话就有多个)
//        File fileDir = new File(DB_DIR_PATH);
//        findFile(fileDir,DB_FILE_NAME);
        // 如果在手机上登陆多个微信的话，在MicroMsg中的多个问价夹下有EnMicroMsg.db，而我们需要找出的是
        // 当前微信号所对应的文件夹，其实这个文件夹的命名就是 MD5("mm"+auth_info_key_prefs.xml中解析出微信的uin码)


        // 所以直接在计算出数据库的详细路径就可以了
        String db_path = DB_DIR_PATH+"/"+encrypt("mm"+currentUin)+"/"+DB_FILE_NAME;
        Log.i("xyz","db_path = "+db_path);


        // 5、将微信数据库复制到本地应用中（直接连接会导致微信客户端退出并出现异常）
        mCurrentApkPath = "/data/data/"+getApplication().getPackageName();
        String copyFilePath = mCurrentApkPath+"/"+COPY_DB_NAME;

        copyFile(db_path,copyFilePath);
        Log.i("xyz","copyFilePath = "+copyFilePath);

        // 6、连接数据库搜索想要的信息（通讯录）

        // 7、插入数据
        File dbFile = new File(db_path);
        linkAndOpenDataBase(dbFile);

    }


    // 有个问题，就是复制把数据库复制到本地之后，对其进行操作，数据库就打不开了，密码错误，但是代码中却可以打开；
    //  直接对微信数据库操作，会使微信推出登陆，今天试了下，微信数据库插入完也打不开了 - -！

    private void linkAndOpenDataBase(File dbFile) {
        //  Context context = getApplicationContext();
        // 这个SQLiteDatabase记得导入的是sqlcipher的类
        SQLiteDatabase.loadLibs(this);
        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteDatabase sqLiteDatabase) {

            }

            @Override
            public void postKey(SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库  
            }
        };

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, password, null, hook);

        String phoneNum = "13430013863";
        String moblie = "+86" + phoneNum;
        String md5 = encrypt(moblie);
        String table = "addr_upload2";

        Cursor cursor = db.rawQuery("select * from "+table, null);
        int sum = cursor.getCount();
        long uploadTime = System.currentTimeMillis() / 1000;

        Random random = new Random();
        String idLarge = md5.substring(0,8).toUpperCase();
        int id = (int) Long.parseLong(idLarge,16);

        sum++;
        String peopleid = "" + sum;
        int reserved = random.nextInt(9999);
        String reserved1 = reserved + "";


        Log.i("xyz","before sum:"+cursor.getCount());


//        Log.i("xyz","insert into "+table+"(id,md5,peopleid,uploadtime,realname,realnamepyinitial," +
//                "realnamequanpin,type,moblie,status,reserved1,reserved3,reserved4,lvbuf,showhead)" +
//                "values(" + id + ",'" + md5 + "','" + peopleid + "'," + uploadTime + ",'" + phoneNum + "','" + phoneNum + "','"
//                + phoneNum + "',0,'" + moblie + "',65535,'" + reserved1 + "',0,0,'{',123)");
//         插入一次之后就不能再运行了，因为id是唯一的
        db.execSQL("insert into "+table+"(id,md5,peopleid,uploadtime,realname,realnamepyinitial," +
                "realnamequanpin,type,moblie,status,reserved1,reserved3,reserved4,lvbuf,showhead)" +
                "values(" + id + ",'" + md5 + "','" + peopleid + "'," + uploadTime + ",'" + phoneNum + "','" + phoneNum + "','"
                + phoneNum + "',0,'" + moblie + "',65535,'" + reserved1 + "',0,0,'{',123)");

        Cursor cursor1 = db.rawQuery("select * from "+table,null);
        Log.i("xyz","now sum :"+cursor1.getCount());

        Cursor cursor2 = db.rawQuery("select * from "+table+" where peopleid = "+cursor1.getCount(),null);
        while (cursor2.moveToNext()){
            String number = cursor2.getString(cursor2.getColumnIndex("moblie"));
            Log.i("xyz","查到的数据最新为: "+number);
        }

        //  查询所有联系人（verifyFlag!=0:公众号(8), 微信团队（56）等类型，群里面非好友的类型为4，群为2，好友为3，自己为1） 
        // 【rcontact】联系人表，【message】聊天消息表
     //   Cursor cursor = db.rawQuery("select * from rcontact where verifyFlag = 0 and type = 3 and nickname !=''",null);
//         Cursor cursor = db.rawQuery("select * from rcontact",null);
//
//        List<User> users = new ArrayList<>();
//        while (cursor.moveToNext()){
//            User user = new User();
//            user.setUserName(cursor.getString(cursor.getColumnIndex("username")));
//            user.setAlias(cursor.getString(cursor.getColumnIndex("alias")));
//            user.setNickName(cursor.getString(cursor.getColumnIndex("nickname")));
//            users.add(user);
//        }
//        Log.i("xyz","users = "+users.toString());
        cursor.close();
        db.close();
    }

    private void copyFile(String oldPath, String newPath) {
        File file = new File(oldPath);
        if (file.exists()){
            try {
                FileInputStream fis = new FileInputStream(oldPath);
                FileOutputStream fos = new FileOutputStream(newPath);
                byte [] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer))!= -1){
                    fos.write(buffer,0,len);
                }
                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

//    private void findFile(File file, String dbFileName) {
//        if (file.isDirectory()){
//            File[] files = file.listFiles();
//            if (files != null){
//                for (File fileChild : files){
//                    findFile(fileChild,dbFileName);
//                }
//            }
//        }else {
//            if (dbFileName.equals(file.getName())){
//                fileList.add(file);
//            }
//        }
//    }


    private void initDbPassword(String phoneIMEI, String currentUin) {

        if (TextUtils.isEmpty(phoneIMEI)||TextUtils.isEmpty(currentUin)){
            Log.i("xyz","IMEI 和 Uin不能为空");
            return;
        }
        String content = phoneIMEI + currentUin;
        password = encrypt(content).substring(0,7).toLowerCase();
        Log.i("xyz","password = "+password);

    }

    // MD5 加密
    private String encrypt(String content) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(content.getBytes("UTF-8"));
            byte[] encryption = digest.digest();  // 加密
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < encryption.length; i++) {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1){
                    sb.append("0").append(Integer.toHexString(0xff & encryption[i]));
                }else {
                    sb.append(Integer.toHexString(0xff & encryption[i]));
                }
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initCurrentUin() {

        File file = new File(SP_UIN_PATH);
        try {
            FileInputStream fis = new FileInputStream(file);
            // 利用dom4j里面的类
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(fis);
            Element root = document.getRootElement();
            List<Element> list = root.elements();

            for (Element element : list){
                if ("_auth_uin".equals(element.attributeValue("name"))){
                    currentUin = element.attributeValue("value");
                    Log.i("xyz","currentUin = "+currentUin);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void initPhoneIMEI() {
        // 记得在manifest中添加权限
//        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        phoneIMEI = manager.getDeviceId();

        // 由于模拟器中的应用变量已经设置为该值，所以直接用这个值，如果没有设置的话就用上面的获取方式
        phoneIMEI = "864725414149606";
        Log.i("xyz","phoneIMEI = "+phoneIMEI);
    }

    private void execCMD(String paramString) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            Object object = process.getOutputStream();
            DataOutputStream dos = new DataOutputStream((OutputStream) object);
            String s = String.valueOf(paramString);
            object = s +"\n";
            dos.writeBytes((String) object);
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            process.waitFor();
            object = process.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
