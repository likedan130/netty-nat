package core.ssl.factory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Project Name: nat-core 
 * Package Name: core.factory
 * ClassName: SslContextFactory 
 * Function: TODO ADD FUNCTION.  
 * date: 2017/4/24 16:24
 * @author wneck130@gmail.com
 * @since JDK 1.8 
 */
public class SslContextFactory {

    private static final String PROTOCOL = "TLSv1.2";
    private static final SSLContext SERVER_CONTEXT;

    static{
        SSLContext serverContext = null;
        // get keystore locations and passwords
        String keyStorePassword = "hadlinks";
        String sep = File.separator;
        try{
            KeyStore ks = KeyStore.getInstance("JKS");
            //加载jks文件，路径：工程目录/cert/server.jks
            ks.load(new FileInputStream(System.getProperty("user.dir")+sep+"cert"+sep+"server.jks"), keyStorePassword.toCharArray());
            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyStorePassword.toCharArray());
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e){
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }
        SERVER_CONTEXT = serverContext;
    }

    public static SSLContext getServerContext(){
        return SERVER_CONTEXT;
    }
}
