package corp.malignor.com;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileNotesRetroUpdate {
    private static String jmxFileResource = "EmailFromParent-JAR-sanitized.jmx";
    private static int waitTime;
    private static int sendCount;
    private static String targetEnv;
    private static String sslCert;
    private static String sslCertPwd;
    private static String[] dataFile;
    private static String ibmClientId;
    private static final String key = "allthi54n0t35";
    private static final Key aesKey = new SecretKeySpec(obfuscate(),"AES");
    private static boolean verbose = false;
    private static boolean diagnosis = false;
    private static byte[] obfuscate(){
        // takes the AES key and encodes it, so the password itself isn't so easy
        byte[] encodedBytes = Base64.getEncoder().encode(key.getBytes());
        byte[] keyBytes = "0123456789ABCDEF".getBytes();
        int begin=0;
        int end=encodedBytes.length-1;
        while (end > begin && begin < keyBytes.length) {
            keyBytes[begin]=encodedBytes[end];
            end--;
            begin++;
        }
        return keyBytes;
    }
    private static String encryptArgs(){
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            if (diagnosis) log("String of SSLCP="+sslCertPwd);
            if (diagnosis) log(" Bytes of SSLCP="+sslCertPwd.getBytes().toString());
            String encryptedPwd = new String(Base64.getEncoder().encode(cipher.doFinal(sslCertPwd.getBytes())));
            String encryptedCid = new String(Base64.getEncoder().encode(cipher.doFinal(ibmClientId.getBytes())));
            System.out.println("sslCertPassword="+encryptedPwd+System.lineSeparator()+"clientID="+encryptedCid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "tadaaa";
    }
    private static String decryptThis(String hashy){
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            String decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(hashy)));
            if (diagnosis) System.out.println("DECRYPTED="+decrypted+" (len="+decrypted.length()+")");
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String runLegit(){
        return "this doesn't work yet";
    }

    private static String runByForce() throws IOException {
        String userDir = System.getProperty("user.dir");
        String jmeterBin = "."+File.separator+"jmeterHome"+File.separator+"bin"+File.separator+"jmeter";
        String jmxFile = "."+File.separator+jmxFileResource;
        String outJtl = "."+File.separator+"NotesAddedExec-"+condensedTs.format(new Date()) +".jtl";
        String jmSysPropsFile = "."+File.separator+"jmeterHome"+File.separator+"bin"+File.separator+"system.properties";
        String entLimit = sendCount>0 ? " -JsendCount="+sendCount : "";
        String execCommon = jmeterBin + " -n -t " + jmxFile + " -l " + outJtl + " -JnotesPauseTime=" + waitTime + " -JenvParam=" + targetEnv + entLimit + " -JibmClientId=" + decryptThis(ibmClientId);

        //=== EDIT PROPERTIES FILE WITH CERTIFICATE===
        File sysProps = new File(jmSysPropsFile);
        String [] certProperty = {"javax.net.ssl.keyStore=","javax.net.ssl.keyStorePassword=","javax.net.ssl.keyStoreType="};
//        String [] certValue = {sslCert,decryptThis(sslCertPwd),"pkcs12"};
//        String [] certValue = {sslCert,decryptThis(sslCertPwd),"JKS"};
        String [] certValue = {sslCert,decryptThis(sslCertPwd),sslCert.toLowerCase().endsWith("jks") ? "JKS" : "pkcs12"};
        for (int cpi = 0; cpi<certProperty.length; cpi++) {
            String certProp = certProperty[cpi];
            BufferedReader file = new BufferedReader(new FileReader(sysProps));
            StringBuffer inputBuffer = new StringBuffer();
            String line;

            while ((line = file.readLine()) != null) {
                // TODO: Figure out the password with its darn $# characters
                if (line.startsWith(certProp)) {
                    String lineC = "#"+certProp; // comment it out
                    if (certProp.compareTo(certProperty[0])==0){
                        line = lineC+System.lineSeparator()+certProp+sslCert;
                    } else if (certProp.compareTo(certProperty[1])==0){
                        line = lineC+System.lineSeparator()+certProp+decryptThis(sslCertPwd);
                    } else if (certProp.compareTo(certProperty[2])==0){
                        //line = lineC+System.lineSeparator()+certProp+"pkcs12";
                        //line = lineC+System.lineSeparator()+certProp+"JKS";
                        line = lineC+System.lineSeparator()+certProp+(sslCert.toLowerCase().endsWith("jks") ? "JKS" : "pkcs12");
                    }
                }
                inputBuffer.append(line+"\r\n");
            }
            inputBuffer.append(certProp+certValue[cpi]+"\r\n");
            file.close();

            // write the new string with the replaced line OVER the same file
            FileOutputStream fileOut = new FileOutputStream(sysProps);
            fileOut.write(inputBuffer.toString().getBytes());
            fileOut.close();
        }

        if (verbose) {
            log("append complete. Final file:");
            BufferedReader fileBR = new BufferedReader(new FileReader(sysProps));
            String line;
            while ((line = fileBR.readLine()) != null) {
                log(line);
            }
            fileBR.close();
        }

        for (String csv : dataFile){
            String execWithCsv = " -JdataFileCsv="+csv;
            log("COMMAND: "+execCommon+execWithCsv);
            runCmd(execCommon+execWithCsv);
        }
        return "complete!";
    }
    private static void runCmd(String commandLine) {
        String windowsCmdPrefix = "cmd /c call ";
        String cmdPrefix = "";
        String osName = System.getProperty("os.name");
        log("OS="+osName);
        if (osName.toLowerCase().contains("windows")) {
            cmdPrefix = windowsCmdPrefix;
        }
        try {
            if (osName.toLowerCase().contains("linux")){
                Runtime.getRuntime().exec("chmod -R 777 ./jmeterHome");
            }
            Process process = Runtime.getRuntime().exec(cmdPrefix+commandLine);
            logOutput(process.getInputStream(), "");
            logOutput(process.getErrorStream(), "Error: ");
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private static void logOutput(InputStream inputStream, String prefix) {
        new Thread(() -> {
            Scanner scanner = new Scanner(inputStream, "UTF-8");
            while (scanner.hasNextLine()) {
                log(prefix + scanner.nextLine());
            }
            scanner.close();
        }).start();
    }
    private static SimpleDateFormat formatTs = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss:SSS");
    private static SimpleDateFormat condensedTs = new SimpleDateFormat("yyMMdd-hhmmss");
    private static synchronized void log(String message) {
        System.out.println(formatTs.format(new Date()) + ": " + message);
    }
    public static void main(String[] args) throws IOException {
        boolean bruteForce = true; // TRUE = installing jmeter, running, and removing jmeter.
        // verify and apply command line args
        if (args.length < 7) {
            System.err.println("Usage: java -jar FileNotesRetroUpdate.jar <environment> <sslCertificate> <certificatePassword> <X-ibmClientId> <timeBetweenMsg> <sendCount> <csvFiles>");
            System.exit(1);
        }

        // Extract command-line arguments
        targetEnv = args[0];
        sslCert = args[1];
        sslCertPwd = args[2];
        ibmClientId = args[3];
        waitTime = Integer.parseInt(args[4]);
        sendCount = Integer.parseInt(args[5]);
        dataFile = args[6].split(",");

        // using the secret value [hashItOut] for the sslCert will instead hash the password and client ID
        if (sslCert.equalsIgnoreCase("hashItOut")) {
            encryptArgs();
            System.exit(0);
        }
        // if cert is appended by zzz then we're doing diagnosis.
        if (sslCert.substring(sslCert.length() - 3).equalsIgnoreCase("zzz")) {
            verbose = true;
            diagnosis = true;
            sslCert = args[1].substring(0,args[1].length() - 3);
        }

        // Print out the arguments (for verification)
        log("Environment: " + targetEnv);
        log("SSL Certificate: " + sslCert);
        log("Certificate Password (encrypted): " + sslCertPwd);
        log("X-IBM Client ID (encrypted): " + ibmClientId);
        log("Time between messages: " + waitTime+"ms");
        log("Maximum entities to notify: " + sendCount+" entities");
        log("CSV Files:");
        for (String csvFile : dataFile) {
            log("- " + csvFile);
        }

        // Copy all the required files from the resource folder. The copies are in the same directory as the JAR file.
        String dirList [] = {"jmeterHome","jmeterHome/bin","jmeterHome/lib","jmeterHome/lib/ext","jmeterHome/lib/junit"};
        String fileList [];

        // this is so I can collapse that huge assignment
        if (bruteForce) {
            String [] fileListBF = {jmxFileResource,"jmeterHome/bin/ApacheJMeter.jar"
                    ,"jmeterHome/bin/ApacheJMeterTemporaryRootCA.crt","jmeterHome/bin/BeanShellAssertion.bshrc","jmeterHome/bin/BeanShellFunction.bshrc"
                    ,"jmeterHome/bin/BeanShellListeners.bshrc","jmeterHome/bin/BeanShellSampler.bshrc","jmeterHome/bin/create-rmi-keystore.bat"
                    ,"jmeterHome/bin/create-rmi-keystore.sh","jmeterHome/bin/hc.parameters","jmeterHome/bin/heapdump.cmd","jmeterHome/bin/heapdump.sh"
                    ,"jmeterHome/bin/jaas.conf","jmeterHome/bin/jmeter","jmeterHome/bin/jmeter-Backup.properties","jmeterHome/bin/jmeter-n-r.cmd"
                    ,"jmeterHome/bin/jmeter-n.cmd","jmeterHome/bin/jmeter-server","jmeterHome/bin/jmeter-server.bat","jmeterHome/bin/jmeter-t.cmd"
                    ,"jmeterHome/bin/jmeter.bat","jmeterHome/bin/jmeter.properties","jmeterHome/bin/jmeter.sh","jmeterHome/bin/jmeterw.cmd"
                    ,"jmeterHome/bin/jmeter_html_report.log","jmeterHome/bin/krb5.conf","jmeterHome/bin/log4j2.xml","jmeterHome/bin/merge-results.properties"
                    ,"jmeterHome/bin/mirror-server","jmeterHome/bin/mirror-server.cmd","jmeterHome/bin/mirror-server.sh","jmeterHome/bin/PluginsManagerCMD.bat"
                    ,"jmeterHome/bin/PluginsManagerCMD.sh","jmeterHome/bin/proxyserver.jks","jmeterHome/bin/reportgenerator.properties"
                    ,"jmeterHome/bin/rmi_keystore.jks","jmeterHome/bin/saveservice.properties","jmeterHome/bin/shutdown.cmd","jmeterHome/bin/shutdown.sh"
                    ,"jmeterHome/bin/stoptest.cmd","jmeterHome/bin/stoptest.sh","jmeterHome/bin/system.properties","jmeterHome/bin/system.properties-original"
                    ,"jmeterHome/bin/threaddump.cmd","jmeterHome/bin/threaddump.sh","jmeterHome/bin/upgrade.properties","jmeterHome/bin/user.properties"
                    ,"jmeterHome/bin/user.properties-original","jmeterHome/bin/utility.groovy","jmeterHome/lib/accessors-smart-2.4.8.jar"
                    ,"jmeterHome/lib/annotations-23.0.0.jar","jmeterHome/lib/apiguardian-api-1.1.2.jar","jmeterHome/lib/asm-9.3.jar"
                    ,"jmeterHome/lib/base-portable-jvm-2.2.1.jar","jmeterHome/lib/batik-anim-1.14.jar","jmeterHome/lib/batik-awt-util-1.14.jar"
                    ,"jmeterHome/lib/batik-bridge-1.14.jar","jmeterHome/lib/batik-codec-1.14.jar","jmeterHome/lib/batik-constants-1.14.jar"
                    ,"jmeterHome/lib/batik-css-1.14.jar","jmeterHome/lib/batik-dom-1.14.jar","jmeterHome/lib/batik-ext-1.14.jar"
                    ,"jmeterHome/lib/batik-gvt-1.14.jar","jmeterHome/lib/batik-i18n-1.14.jar","jmeterHome/lib/batik-parser-1.14.jar"
                    ,"jmeterHome/lib/batik-script-1.14.jar","jmeterHome/lib/batik-shared-resources-1.14.jar","jmeterHome/lib/batik-svg-dom-1.14.jar"
                    ,"jmeterHome/lib/batik-svggen-1.14.jar","jmeterHome/lib/batik-transcoder-1.14.jar","jmeterHome/lib/batik-util-1.14.jar"
                    ,"jmeterHome/lib/batik-xml-1.14.jar","jmeterHome/lib/bsf-2.4.0.jar","jmeterHome/lib/bsh-2.0b6.jar","jmeterHome/lib/bshclient.jar"
                    ,"jmeterHome/lib/caffeine-2.9.3.jar","jmeterHome/lib/checker-qual-3.19.0.jar","jmeterHome/lib/com.ibm.mq.allclient-9.0.4.0.jar"
                    ,"jmeterHome/lib/commons-codec-1.15.jar","jmeterHome/lib/commons-collections-3.2.2.jar","jmeterHome/lib/commons-collections4-4.4.jar"
                    ,"jmeterHome/lib/commons-dbcp2-2.9.0.jar","jmeterHome/lib/commons-io-2.11.0.jar","jmeterHome/lib/commons-jexl-2.1.1.jar"
                    ,"jmeterHome/lib/commons-jexl3-3.2.1.jar","jmeterHome/lib/commons-lang3-3.12.0.jar","jmeterHome/lib/commons-lang3-3.8.1.jar"
                    ,"jmeterHome/lib/commons-logging-1.2.jar","jmeterHome/lib/commons-math3-3.6.1.jar","jmeterHome/lib/commons-net-3.8.0.jar"
                    ,"jmeterHome/lib/commons-pool2-2.11.1.jar","jmeterHome/lib/commons-text-1.9.jar","jmeterHome/lib/darklaf-core-2.7.3.jar"
                    ,"jmeterHome/lib/darklaf-extensions-rsyntaxarea-0.3.4.jar","jmeterHome/lib/darklaf-macos-2.7.3.jar","jmeterHome/lib/darklaf-native-utils-2.7.3.jar"
                    ,"jmeterHome/lib/darklaf-platform-base-2.7.3.jar","jmeterHome/lib/darklaf-property-loader-2.7.3.jar","jmeterHome/lib/darklaf-theme-2.7.3.jar"
                    ,"jmeterHome/lib/darklaf-utils-2.7.3.jar","jmeterHome/lib/darklaf-windows-2.7.3.jar","jmeterHome/lib/dec-0.1.2.jar"
                    ,"jmeterHome/lib/dnsjava-2.1.9.jar","jmeterHome/lib/error_prone_annotations-2.10.0.jar"
                    ,"jmeterHome/lib/freemarker-2.3.31.jar","jmeterHome/lib/geronimo-jms_1.1_spec-1.1.1.jar","jmeterHome/lib/groovy-3.0.11.jar"
                    ,"jmeterHome/lib/groovy-datetime-3.0.11.jar","jmeterHome/lib/groovy-dateutil-3.0.11.jar","jmeterHome/lib/groovy-jmx-3.0.11.jar"
                    ,"jmeterHome/lib/groovy-json-2.1.1.jar","jmeterHome/lib/groovy-jsr223-3.0.11.jar","jmeterHome/lib/groovy-sql-3.0.11.jar"
                    ,"jmeterHome/lib/groovy-templates-3.0.11.jar","jmeterHome/lib/groovy-xml-3.0.11.jar","jmeterHome/lib/hamcrest-2.2.jar"
                    ,"jmeterHome/lib/hamcrest-core-2.2.jar","jmeterHome/lib/hamcrest-date-2.0.8.jar","jmeterHome/lib/hsqldb-2.5.0.jar"
                    ,"jmeterHome/lib/httpasyncclient-4.1.5.jar","jmeterHome/lib/httpclient-4.5.13.jar","jmeterHome/lib/httpcore-4.4.15.jar"
                    ,"jmeterHome/lib/httpcore-nio-4.4.15.jar","jmeterHome/lib/httpmime-4.5.13.jar","jmeterHome/lib/jackcess-3.0.1.jar"
                    ,"jmeterHome/lib/jackson-annotations-2.13.3.jar","jmeterHome/lib/jackson-core-2.13.3.jar","jmeterHome/lib/jackson-databind-2.13.3.jar"
                    ,"jmeterHome/lib/jackson-dataformat-xml-2.10.2.jar","jmeterHome/lib/jackson-module-jaxb-annotations-2.10.2.jar"
                    ,"jmeterHome/lib/javax.activation-1.2.0.jar","jmeterHome/lib/javax.mail.jar","jmeterHome/lib/jcharts-0.7.5.jar"
                    ,"jmeterHome/lib/jcl-over-slf4j-1.7.36.jar","jmeterHome/lib/jmespath-core-0.5.1.jar","jmeterHome/lib/jmespath-jackson-0.5.1.jar"
                    ,"jmeterHome/lib/jmeter-bzm-commons-0.2.1.jar","jmeterHome/lib/jodd-core-5.0.13.jar","jmeterHome/lib/jodd-lagarto-5.0.13.jar"
                    ,"jmeterHome/lib/jodd-log-5.0.13.jar","jmeterHome/lib/jodd-props-5.0.13.jar","jmeterHome/lib/jorphan.jar"
                    ,"jmeterHome/lib/json-20190722.jar","jmeterHome/lib/json-path-2.7.0.jar","jmeterHome/lib/json-smart-2.4.8.jar"
                    ,"jmeterHome/lib/jsoup-1.15.1.jar","jmeterHome/lib/jtidy-r938.jar","jmeterHome/lib/junit-4.13.2.jar"
                    ,"jmeterHome/lib/kotlin-logging-jvm-2.0.5.jar","jmeterHome/lib/kotlin-stdlib-1.6.21.jar","jmeterHome/lib/kotlin-stdlib-common-1.6.21.jar"
                    ,"jmeterHome/lib/kotlin-stdlib-jdk7-1.6.21.jar","jmeterHome/lib/kotlin-stdlib-jdk8-1.6.21.jar","jmeterHome/lib/kotlinx-coroutines-core-jvm-1.6.1.jar"
                    ,"jmeterHome/lib/kotlinx-coroutines-swing-1.6.1.jar","jmeterHome/lib/kotlinx-html-jvm-0.7.3.jar","jmeterHome/lib/lets-plot-batik-2.2.1.jar"
                    ,"jmeterHome/lib/lets-plot-common-2.2.1.jar","jmeterHome/lib/local_policy.jar","jmeterHome/lib/log4j-1.2-api-2.17.2.jar"
                    ,"jmeterHome/lib/log4j-api-2.17.2.jar","jmeterHome/lib/log4j-core-2.17.2.jar","jmeterHome/lib/log4j-slf4j-impl-2.17.2.jar"
                    ,"jmeterHome/lib/mail-1.5.0-b01.jar","jmeterHome/lib/maven-artifact-3.8.4.jar","jmeterHome/lib/miglayout-core-5.3.jar"
                    ,"jmeterHome/lib/miglayout-swing-5.3.jar","jmeterHome/lib/mongo-java-driver-2.11.3.jar","jmeterHome/lib/mxparser-1.2.2.jar"
                    ,"jmeterHome/lib/neo4j-java-driver-4.4.6.jar","jmeterHome/lib/ojdbc11.jar","jmeterHome/lib/ojdbc8.jar","jmeterHome/lib/oro-2.0.8.jar"
                    ,"jmeterHome/lib/perfmon-2.2.2.jar","jmeterHome/lib/ph-commons-10.1.6.jar","jmeterHome/lib/ph-css-6.5.0.jar"
                    ,"jmeterHome/lib/plot-api-jvm-3.1.1.jar","jmeterHome/lib/plot-base-portable-jvm-2.2.1.jar","jmeterHome/lib/plot-builder-portable-jvm-2.2.1.jar"
                    ,"jmeterHome/lib/plot-common-portable-jvm-2.2.1.jar","jmeterHome/lib/plot-config-portable-jvm-2.2.1.jar"
                    ,"jmeterHome/lib/reactive-streams-1.0.3.jar","jmeterHome/lib/rhino-1.7.14.jar","jmeterHome/lib/rsyntaxtextarea-3.2.0.jar"
                    ,"jmeterHome/lib/Saxon-HE-11.3.jar","jmeterHome/lib/selenium-os-4.13.0.jar","jmeterHome/lib/serializer-2.7.2.jar"
                    ,"jmeterHome/lib/slf4j-api-1.7.36.jar","jmeterHome/lib/svgSalamander-1.1.2.4.jar","jmeterHome/lib/swing-extensions-laf-support-0.1.3.jar"
                    ,"jmeterHome/lib/swing-extensions-visual-padding-0.1.3.jar","jmeterHome/lib/tika-core-1.28.3.jar","jmeterHome/lib/tika-parsers-1.28.3.jar"
                    ,"jmeterHome/lib/ucanaccess-5.0.1.jar","jmeterHome/lib/US_export_policy.jar","jmeterHome/lib/vis-svg-portable-jvm-2.2.1.jar"
                    ,"jmeterHome/lib/xalan-2.7.2.jar","jmeterHome/lib/xercesImpl-2.12.2.jar","jmeterHome/lib/xml-apis-1.4.01.jar"
                    ,"jmeterHome/lib/xml-apis-ext-1.3.04.jar","jmeterHome/lib/xmlgraphics-commons-2.7.jar","jmeterHome/lib/xmlpull-1.1.3.1.jar"
                    ,"jmeterHome/lib/xmlresolver-4.2.0-data.jar","jmeterHome/lib/xmlresolver-4.2.0.jar","jmeterHome/lib/xstream-1.4.19.jar"
                    ,"jmeterHome/lib/ext/ApacheJMeter_bolt.jar","jmeterHome/lib/ext/ApacheJMeter_components.jar","jmeterHome/lib/ext/ApacheJMeter_core.jar"
                    ,"jmeterHome/lib/ext/ApacheJMeter_ftp.jar","jmeterHome/lib/ext/ApacheJMeter_functions.jar","jmeterHome/lib/ext/ApacheJMeter_http.jar"
                    ,"jmeterHome/lib/ext/ApacheJMeter_java.jar","jmeterHome/lib/ext/ApacheJMeter_jdbc.jar","jmeterHome/lib/ext/ApacheJMeter_jms.jar"
                    ,"jmeterHome/lib/ext/ApacheJMeter_junit.jar","jmeterHome/lib/ext/ApacheJMeter_ldap.jar","jmeterHome/lib/ext/ApacheJMeter_mail.jar"
                    ,"jmeterHome/lib/ext/ApacheJMeter_mongodb.jar","jmeterHome/lib/ext/ApacheJMeter_native.jar","jmeterHome/lib/ext/ApacheJMeter_tcp.jar"
                    ,"jmeterHome/lib/ext/async-http-client-2.12.3.jar","jmeterHome/lib/ext/async-http-client-netty-utils-2.12.3.jar"
                    ,"jmeterHome/lib/ext/byte-buddy-1.14.2.jar","jmeterHome/lib/ext/bzm-repositories-plugin-1.0.jar","jmeterHome/lib/ext/bzm-repositories-plugin-1.1.jar"
                    ,"jmeterHome/lib/ext/cmdrunner-2.3.jar","jmeterHome/lib/ext/com.ibm.mq.allclient-9.0.4.0.jar","jmeterHome/lib/ext/commons-exec-1.3.jar"
                    ,"jmeterHome/lib/ext/CustomSoapSampler-1.3.3.jar","jmeterHome/lib/ext/di-extended-csv-2.0.jar","jmeterHome/lib/ext/failsafe-3.3.1.jar"
                    ,"jmeterHome/lib/ext/guava-31.1-jre.jar","jmeterHome/lib/ext/htmlunit-2.70.0.jar","jmeterHome/lib/ext/htmlunit-core-js-2.70.0.jar"
                    ,"jmeterHome/lib/ext/htmlunit-cssparser-1.14.0.jar","jmeterHome/lib/ext/htmlunit-driver-4.8.1.1.jar","jmeterHome/lib/ext/htmlunit-xpath-2.70.0.jar"
                    ,"jmeterHome/lib/ext/jmeter-bzm-correlation-recorder-2.1.jar","jmeterHome/lib/ext/jmeter-bzm-correlation-recorder-2.2.1.jar"
                    ,"jmeterHome/lib/ext/jmeter-dynatrace-plugin-1.8.0.jar","jmeterHome/lib/ext/jmeter-plugins-casutg-2.10.jar","jmeterHome/lib/ext/jmeter-plugins-cmn-jmeter-0.7.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-csvars-0.1.jar","jmeterHome/lib/ext/jmeter-plugins-dbmon-0.1.jar","jmeterHome/lib/ext/jmeter-plugins-functions-2.2.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-graphs-additional-2.0.jar","jmeterHome/lib/ext/jmeter-plugins-graphs-basic-2.0.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-graphs-composite-2.0.jar","jmeterHome/lib/ext/jmeter-plugins-graphs-vs-2.0.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-manager-1.10.jar","jmeterHome/lib/ext/jmeter-plugins-manager-1.9.jar","jmeterHome/lib/ext/jmeter-plugins-mergeresults-2.1.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-perfmon-2.1.jar","jmeterHome/lib/ext/jmeter-plugins-webdriver-4.10.0.0.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-webdriver-4.13.0.0.jar","jmeterHome/lib/ext/jmeter-plugins-webdriver-4.9.1.0.jar"
                    ,"jmeterHome/lib/ext/jmeter-plugins-wsc-0.7.jar","jmeterHome/lib/ext/jmeter-plugins-xml-0.1.jar","jmeterHome/lib/ext/jmeter-websocket-samplers-1.2.10.jar"
                    ,"jmeterHome/lib/ext/jmeter-websocket-samplers-1.2.8.jar","jmeterHome/lib/ext/jmeter.backendlistener.azure-0.2.9.jar"
                    ,"jmeterHome/lib/ext/json-lib-2.4-jdk15.jar","jmeterHome/lib/ext/jsyntaxpane-1.0.0.jar","jmeterHome/lib/ext/mqmeter-2.1.0.jar"
                    ,"jmeterHome/lib/ext/neko-htmlunit-2.70.0.jar","jmeterHome/lib/ext/netty-buffer-4.1.90.Final.jar","jmeterHome/lib/ext/netty-codec-4.1.90.Final.jar"
                    ,"jmeterHome/lib/ext/netty-codec-http-4.1.90.Final.jar","jmeterHome/lib/ext/netty-common-4.1.90.Final.jar","jmeterHome/lib/ext/netty-handler-4.1.90.Final.jar"
                    ,"jmeterHome/lib/ext/netty-reactive-streams-2.0.8.jar","jmeterHome/lib/ext/netty-resolver-4.1.90.Final.jar","jmeterHome/lib/ext/netty-transport-4.1.90.Final.jar"
                    ,"jmeterHome/lib/ext/okhttp-4.11.0.jar","jmeterHome/lib/ext/okio-jvm-3.4.0.jar","jmeterHome/lib/ext/opentelemetry-api-1.24.0.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-api-events-1.24.0-alpha.jar","jmeterHome/lib/ext/opentelemetry-api-logs-1.24.0-alpha.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-context-1.24.0.jar","jmeterHome/lib/ext/opentelemetry-exporter-common-1.24.0.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-exporter-logging-1.24.0.jar","jmeterHome/lib/ext/opentelemetry-exporter-otlp-1.28.0.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-exporter-otlp-common-1.28.0.jar","jmeterHome/lib/ext/opentelemetry-sdk-1.24.0.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-sdk-common-1.24.0.jar","jmeterHome/lib/ext/opentelemetry-sdk-extension-autoconfigure-1.24.0-alpha.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-sdk-extension-autoconfigure-spi-1.24.0.jar","jmeterHome/lib/ext/opentelemetry-sdk-logs-1.24.0-alpha.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-sdk-metrics-1.24.0.jar","jmeterHome/lib/ext/opentelemetry-sdk-trace-1.24.0.jar"
                    ,"jmeterHome/lib/ext/opentelemetry-semconv-1.24.0-alpha.jar","jmeterHome/lib/ext/readme.txt","jmeterHome/lib/ext/selenium-api-4.8.3.jar"
                    ,"jmeterHome/lib/ext/selenium-chrome-driver-4.8.3.jar","jmeterHome/lib/ext/selenium-chromium-driver-4.8.3.jar"
                    ,"jmeterHome/lib/ext/selenium-edge-driver-4.8.3.jar","jmeterHome/lib/ext/selenium-firefox-driver-4.8.3.jar","jmeterHome/lib/ext/selenium-http-4.8.3.jar"
                    ,"jmeterHome/lib/ext/selenium-ie-driver-4.8.3.jar","jmeterHome/lib/ext/selenium-java-4.8.3.jar","jmeterHome/lib/ext/selenium-json-4.8.3.jar"
                    ,"jmeterHome/lib/ext/selenium-remote-driver-4.8.3.jar","jmeterHome/lib/ext/selenium-support-4.8.3.jar","jmeterHome/lib/ext/validatetg-1.0.1.jar"
                    ,"jmeterHome/lib/junit/readme.txt"};
            fileList = fileListBF;
        }
        else {
            String [] fileListLeg = {jmxFileResource,"jmeterHome/bin/jmeter.properties","jmeterHome/bin/upgrade.properties","jmeterHome/bin/user.properties","jmeterHome/bin/system.properties","jmeterHome/bin/reportgenerator.properties","jmeterHome/bin/saveservice.properties"};
            fileList = fileListLeg;
        }

        List toFrag = new ArrayList<String>();
        for (String dentry : dirList) {
            new File(dentry).mkdir();
            toFrag.add(dentry);
        }
        for (String fentry : fileList) {
            verbose = true;
            if (verbose) log("about to copy /resource/"+fentry);
            ClassLoader loader = FileNotesRetroUpdate.class.getClassLoader();
            InputStream resourceStream = loader.getResourceAsStream(fentry);
            File tmpFile = new File(System.getProperty("user.dir")+"/"+fentry);
            Files.copy(resourceStream, tmpFile.toPath(), REPLACE_EXISTING);
            toFrag.add(fentry);
        }
        // copy the certificate to the jmeter bin folder
        String certDest = ""+System.getProperty("user.dir")+File.separator+"jmeterHome"+File.separator+"bin"+File.separator+sslCert;
        log("about to copy /resource/"+sslCert);
        log("destination will be "+certDest);
        ClassLoader loader = FileNotesRetroUpdate.class.getClassLoader();
        InputStream resourceStream = new FileInputStream(sslCert);
        File tmpFile = new File(certDest);
        Files.copy(resourceStream, tmpFile.toPath(), REPLACE_EXISTING);
        toFrag.add(""+File.separator+"jmeterHome"+File.separator+"bin"+File.separator+sslCert);

        log("copied "+fileList.length+" files to "+dirList.length+" directories");

        if (bruteForce){
            log(runByForce());
        }
        else {
            log(runLegit());
        }

        // clean up the temporary resources unless we're diagnosing a problem
        if (!diagnosis) {
            Collections.reverse(toFrag);
            for (Object fragTemp : toFrag) {
                if (verbose) log("cleaning up " + fragTemp);
                File toPurge = new File(System.getProperty("user.dir") + "/" + fragTemp);
                File[] contents = toPurge.listFiles();
                if (contents != null) {
                    for (File f : contents) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
                Files.deleteIfExists(toPurge.toPath());
            }
        }
    }

}
