package run.runnable;

/**
 * @author Asher
 * on 2022/3/12
 */

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.PutObjectRequest;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.DateUtils;

import java.io.*;
import java.util.*;

public class Upload {

    private static String endpoint = "oss-cn-guangzhou.aliyuncs.com";
    private static String accessKeyId = "";
    private static String accessKeySecret = "";
    private static String bucketName = "runnable";
    private static String dirName = "blog";

    //需要跳过上传的图片链接前缀
    static final HashSet<String> skipHandlerLink = new HashSet<>();
    static {
        skipHandlerLink.add("https://maven-badges.herokuapp.com/");
        skipHandlerLink.add("https://javadoc.io/badge/");
    }
    static final ArrayList<String> errorInfoList = new ArrayList();

    final static OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

    public static void main(String[] args) throws IOException {
        //solo导出的博客数据位置，只针对posts文件夹中的博客
        String blogDirPath = "/Users/asher/Downloads/solo-hexo-20220308225335/posts";
        //当图片下载异常，输出的日志位置
        String errorUploadLogPath = "/Users/asher/Desktop/temp/errorInfoList.txt";


        final File blogDir = new File(blogDirPath);
        if (blogDir.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(blogDir.listFiles()))
                    .filter(b -> StringUtils.isNotBlank(b.getAbsolutePath()))
                    .filter(File::isDirectory)
                    .flatMap(f -> Arrays.stream(f.listFiles()))
                    .filter(blogFile -> "md".equals(getFileExtension(blogFile.getName())))
                    .forEach(Upload::handlePostBlogFile);
        }
        FileUtils.writeLines(new File(errorUploadLogPath), errorInfoList);
    }


    private static void handlePostBlogFile(File file) {
        System.out.println("当前执行博客文件为：" + file.getAbsolutePath());
        List<String> blogContentList = null;
        try {
            blogContentList = FileUtils.readLines(file, Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final ArrayList<String> saveLine = new ArrayList<>();
            for (String line : blogContentList) {

                boolean skipFlag = false;

                String finalLine = line;
                if (skipHandlerLink.stream().anyMatch(finalLine::contains)) {
                    skipFlag = true;
                }

                final List<String> imgURLList = StringUtil.getRegexString(line, "\\!\\[.*\\]\\(http.*\\)");
                if (imgURLList.size() > 0 && !skipFlag) {
                    String imgContent = Optional.ofNullable(imgURLList.get(0)).orElse(line);

                    String imgURL = StringUtil.getRegexString(imgContent, "http.*")
                                              .get(0)
                                              .replace(")","");
                    String fileExtension = getFileExtension(imgURL);

                    System.out.println("正在上传的图片URL："+imgURL);

                    try {
                        InputStream imgInputStream = imgURL.indexOf("https")>0 ? X509TrustManager.downLoadFromUrlHttps(imgURL) :
                                X509TrustManager.downLoadFromUrlHttp(imgURL);
                        final String url = uploadFile(imgInputStream, fileExtensionJudge(fileExtension));
                        imgInputStream.close();
                        System.out.println("成功替换为aliyun的图片URL："+url);

                        System.out.println("当前行进行替换：" + line);
                        line = line.replace(imgURL, url);
                        System.out.println("替换之后的行为：" + line);

                        System.out.println();
                    }catch (Exception ignored){
                        errorInfoList.add("当前图片下载失败，文件为："+file.getAbsolutePath()+"，   链接为：" + imgURL);;
                        ignored.printStackTrace();
                    }

                }
                saveLine.add(line);
            }
            final String fileName = file.getName();
            final String renameName = file.getParentFile().getAbsolutePath() +File.separator + "aliyun-" + fileName;
        try {
            FileUtils.writeLines(new File(renameName), saveLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("当前文章执行完成，文章："+file.getAbsolutePath());
        System.out.println("=============================================");

    }

    static String fileExtensionJudge(String fileExtension){
        switch (fileExtension){
            case "svg":
                return "svg";
            case "jpeg":
                return "jpeg";
            case "apng":
                return "apng";
            case "webp":
                return "webp";
            case "png":
                return "png";
            default:
                return "jpg";
        }
    }


    private static String getFileExtension(String fileName) {
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }

    public static String uploadFile(InputStream inputStream, String fileExtension) {
        final String extension = StringUtils.isBlank(fileExtension) ? "png" : fileExtension;
        String fileName = dirName + File.separator + DateUtils.formatDate(new Date(), "yyyy-MM-dd") + "-" + inputStream.hashCode() + "."+extension;//获取文件名
        ossClient.putObject(new PutObjectRequest(bucketName, fileName, inputStream));
        ossClient.setObjectAcl(bucketName, fileName, CannedAccessControlList.PublicRead);
        ossClient.setObjectAcl(bucketName, fileName, CannedAccessControlList.Default);
        return "https://" + bucketName + "." + endpoint + "/" + fileName;
    }


}

